package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceQuoteCommandServiceTests {

    @Mock private ServiceOrderRepository orderRepository;
    @Mock private ServiceLineRepository lineRepository;
    @Mock private ServiceQuoteRepository quoteRepository;
    @Mock private ServiceQuoteLineRepository quoteLineRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private ServiceQuoteLineEligibilityPolicy eligibilityPolicy;

    private ServiceQuoteCommandService service;

    @BeforeEach
    void setUp() {
        service = new ServiceQuoteCommandService(
                orderRepository,
                lineRepository,
                quoteRepository,
                quoteLineRepository,
                authorizationService,
                eligibilityPolicy
        );
    }

    @Test
    void createsDraftQuoteAndSnapshotsEligibleLinesInLineNumberOrder() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceLine later = commercialLine(order, 20, null);
        ServiceLine earlier = commercialLine(order, 10, UUID.randomUUID());
        stubOrder(context, order);
        when(lineRepository.findAllByServiceOrderIdOrderByLineNumber(order.getId()))
                .thenReturn(List.of(earlier, later));
        when(quoteRepository.existsByTenantIdAndQuoteNumber(order.getTenantId(), "Q-1"))
                .thenReturn(false);
        when(quoteLineRepository
                .findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
                        any(), eq(order.getId()), eq(order.getTenantId())))
                .thenReturn(List.of());
        when(quoteRepository.save(any(ServiceQuote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteLineRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceQuote result = service.create(
                context, order.getId(), "Q-1", "EUR", null,
                "Terms", "Disclaimer"
        );

        assertEquals(ServiceQuoteStatus.DRAFT, result.getStatus());
        assertEquals(order.getTenantId(), result.getTenantId());
        assertEquals(order.getDealerId(), result.getDealerId());
        assertEquals(order.getBranchId(), result.getBranchId());
        assertEquals(order.getId(), result.getServiceOrderId());
        assertNull(result.getAfterSalesCaseId());
        assertEquals("Q-1", result.getQuoteNumber());
        assertEquals("EUR", result.getCurrencyCode());

        ArgumentCaptor<List<ServiceQuoteLine>> linesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(quoteLineRepository).saveAll(linesCaptor.capture());
        List<ServiceQuoteLine> lines = linesCaptor.getValue();
        assertEquals(2, lines.size());
        assertEquals(earlier.getId(), lines.get(0).getServiceLineId());
        assertEquals(0, lines.get(0).getSequence());
        assertEquals(earlier.getServiceJobId(), lines.get(0).getServiceJobId());
        assertEquals(later.getId(), lines.get(1).getServiceLineId());
        assertEquals(1, lines.get(1).getSequence());

        InOrder orderOfSaves = inOrder(quoteRepository, quoteLineRepository);
        orderOfSaves.verify(quoteRepository).save(any(ServiceQuote.class));
        orderOfSaves.verify(quoteLineRepository).saveAll(any());
        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context,
                AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                AuthorizationResourceType.SERVICE_ORDER,
                order.getId()
        ));
    }

    @Test
    void excludesUnreadyMismatchedAndBlockingLines() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceLine unready = line(order, 10, null);
        ServiceLine wrongCurrency = commercialLine(order, 20, null);
        wrongCurrency.applyCommercialSnapshot(
                BigDecimal.ONE, "USD", BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ONE, context.userRefId(), OffsetDateTime.now());
        ServiceLine blocked = commercialLine(order, 30, null);
        stubOrder(context, order);
        when(lineRepository.findAllByServiceOrderIdOrderByLineNumber(order.getId()))
                .thenReturn(List.of(unready, wrongCurrency, blocked));
        when(quoteRepository.existsByTenantIdAndQuoteNumber(order.getTenantId(), "Q-2"))
                .thenReturn(false);
        when(quoteLineRepository
                .findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
                        blocked.getId(), order.getId(), order.getTenantId()))
                .thenReturn(List.of(ServiceQuoteStatus.ISSUED));
        when(eligibilityPolicy.blocksRequotation(ServiceQuoteStatus.ISSUED))
                .thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.create(
                context, order.getId(), "Q-2", "EUR", null, null, null));

        verify(quoteRepository, never()).save(any());
        verify(quoteLineRepository, never()).saveAll(any());
    }

    @Test
    void releasingHistoryDoesNotBlockAndBlockingHistoryWins() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceLine releasing = commercialLine(order, 10, null);
        ServiceLine mixedHistory = commercialLine(order, 20, null);
        stubOrder(context, order);
        when(lineRepository.findAllByServiceOrderIdOrderByLineNumber(order.getId()))
                .thenReturn(List.of(releasing, mixedHistory));
        when(quoteRepository.existsByTenantIdAndQuoteNumber(order.getTenantId(), "Q-3"))
                .thenReturn(false);
        when(quoteLineRepository
                .findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
                        releasing.getId(), order.getId(), order.getTenantId()))
                .thenReturn(List.of(ServiceQuoteStatus.DECLINED, ServiceQuoteStatus.CANCELLED));
        when(quoteLineRepository
                .findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
                        mixedHistory.getId(), order.getId(), order.getTenantId()))
                .thenReturn(List.of(ServiceQuoteStatus.DECLINED, ServiceQuoteStatus.ISSUED));
        when(eligibilityPolicy.blocksRequotation(ServiceQuoteStatus.DECLINED))
                .thenReturn(false);
        when(eligibilityPolicy.blocksRequotation(ServiceQuoteStatus.CANCELLED))
                .thenReturn(false);
        when(eligibilityPolicy.blocksRequotation(ServiceQuoteStatus.ISSUED))
                .thenReturn(true);
        when(quoteRepository.save(any(ServiceQuote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteLineRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(context, order.getId(), "Q-3", "EUR", null, null, null);

        ArgumentCaptor<List<ServiceQuoteLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(quoteLineRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(releasing.getId(), captor.getValue().getFirst().getServiceLineId());
    }

    @Test
    void authorizesBeforeDependentRepositoryWorkAndHidesMissingOrder() {
        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService).requirePermission(any());

        assertThrows(AccessDeniedException.class, () -> service.create(
                context, orderId, "Q-4", "EUR", null, null, null));
        verify(orderRepository, never()).findByIdAndTenantId(any(), any());

        org.mockito.Mockito.reset(authorizationService);
        when(orderRepository.findByIdAndTenantId(orderId, context.tenantId()))
                .thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.create(
                context, orderId, "Q-4", "EUR", null, null, null));
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void duplicateQuoteNumberStopsBeforeLineResolution() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        stubOrder(context, order);
        when(quoteRepository.existsByTenantIdAndQuoteNumber(order.getTenantId(), "Q-DUP"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(context, order.getId(), "Q-DUP", "EUR", null, null, null));

        assertEquals(409, exception.getStatusCode().value());
        verify(lineRepository, never()).findAllByServiceOrderIdOrderByLineNumber(any());
        verify(quoteRepository, never()).save(any());
    }

    private void stubOrder(AuthenticatedTenantContext context, ServiceOrder order) {
        when(orderRepository.findByIdAndTenantId(order.getId(), context.tenantId()))
                .thenReturn(Optional.of(order));
    }

    private AuthenticatedTenantContext context() {
        return new AuthenticatedTenantContext(
                UUID.randomUUID(), UUID.randomUUID(), "operator");
    }

    private ServiceOrder order(AuthenticatedTenantContext context) {
        return ServiceOrder.open(
                UUID.randomUUID(), context.tenantId(), UUID.randomUUID(),
                UUID.randomUUID(), "SO-" + UUID.randomUUID(), UUID.randomUUID(),
                context.userRefId(), OffsetDateTime.now());
    }

    private ServiceLine line(ServiceOrder order, int lineNumber, UUID jobId) {
        return ServiceLine.create(
                UUID.randomUUID(), order.getId(), jobId, lineNumber,
                ServiceLineType.LABOR, "Workshop labor", BigDecimal.ONE,
                "HOUR", UUID.randomUUID(), OffsetDateTime.now());
    }

    private ServiceLine commercialLine(ServiceOrder order, int lineNumber, UUID jobId) {
        ServiceLine line = line(order, lineNumber, jobId);
        line.applyCommercialSnapshot(
                new BigDecimal("10.00"), "EUR", new BigDecimal("10.00"),
                BigDecimal.ZERO, new BigDecimal("10.00"), UUID.randomUUID(),
                OffsetDateTime.now());
        return line;
    }
}