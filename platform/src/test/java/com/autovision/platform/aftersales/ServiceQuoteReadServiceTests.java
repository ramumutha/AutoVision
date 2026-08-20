package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceQuoteReadServiceTests {

    @Mock private ServiceOrderRepository orderRepository;
    @Mock private ServiceQuoteRepository quoteRepository;
    @Mock private ServiceQuoteLineRepository lineRepository;
    @Mock private AuthorizationService authorizationService;

    private ServiceQuoteReadService service;

    @BeforeEach
    void setUp() {
        service = new ServiceQuoteReadService(
                orderRepository, quoteRepository, lineRepository, authorizationService);
    }

    @Test
    void listsTenantContainedQuotesInRepositoryOrder() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceQuote first = quote(order, "Q-1");
        ServiceQuote second = quote(order, "Q-2");
        when(orderRepository.findByIdAndTenantId(order.getId(), context.tenantId()))
                .thenReturn(Optional.of(order));
        when(quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                order.getId(), context.tenantId())).thenReturn(List.of(first, second));

        assertEquals(List.of(first, second), service.listForServiceOrder(context, order.getId()));
        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context, AfterSalesPermissions.SERVICE_ORDER_READ,
                AuthorizationResourceType.SERVICE_ORDER, order.getId()));
    }

    @Test
    void emptyOrderQuoteListIsReturnedUnchanged() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        when(orderRepository.findByIdAndTenantId(order.getId(), context.tenantId()))
                .thenReturn(Optional.of(order));
        when(quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                order.getId(), context.tenantId())).thenReturn(List.of());

        assertEquals(List.of(), service.listForServiceOrder(context, order.getId()));
    }

    @Test
    void authorizationDenialPreventsQuoteRepositoryAccess() {
        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();
        doThrow(new AccessDeniedException("denied")).when(authorizationService)
                .requirePermission(any(AuthorizationRequest.class));

        assertThrows(AccessDeniedException.class,
                () -> service.listForServiceOrder(context, orderId));
        verify(orderRepository, never()).findByIdAndTenantId(any(), any());
        verify(quoteRepository, never()).findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void detailRejectsCrossOrderQuoteAndLoadsOrderedLinesForContainedQuote() {
        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceOrder otherOrder = order(context);
        ServiceQuote quote = quote(otherOrder, "Q-CROSS");
        when(orderRepository.findByIdAndTenantId(order.getId(), context.tenantId()))
                .thenReturn(Optional.of(order));
        when(quoteRepository.findByIdAndTenantId(quote.getId(), context.tenantId()))
                .thenReturn(Optional.of(quote));

        assertThrows(ResponseStatusException.class,
                () -> service.getForServiceOrder(context, order.getId(), quote.getId()));

        ServiceQuote contained = quote(order, "Q-DETAIL");
        when(quoteRepository.findByIdAndTenantId(contained.getId(), context.tenantId()))
                .thenReturn(Optional.of(contained));
        ServiceQuoteLine line = line(contained);
        when(lineRepository.findByServiceQuoteIdOrderBySequenceAsc(contained.getId()))
                .thenReturn(List.of(line));

        assertEquals(List.of(line), service.linesFor(context, order.getId(), contained.getId()));
    }

    @Test
    void missingContainedOrderIsNotFound() {
        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(orderId, context.tenantId()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.listForServiceOrder(context, orderId));
    }

    private AuthenticatedTenantContext context() {
        return new AuthenticatedTenantContext(UUID.randomUUID(), UUID.randomUUID(), "operator");
    }

    private ServiceOrder order(AuthenticatedTenantContext context) {
        return ServiceOrder.open(UUID.randomUUID(), context.tenantId(), null, null,
                "SO-" + UUID.randomUUID(), UUID.randomUUID(), context.userRefId(), OffsetDateTime.now());
    }

    private ServiceQuote quote(ServiceOrder order, String number) {
        return ServiceQuote.create(UUID.randomUUID(), order.getTenantId(), null, null, null,
                order.getId(), number, "EUR", null, null, null,
                UUID.randomUUID(), OffsetDateTime.now());
    }

    private ServiceQuoteLine line(ServiceQuote quote) {
        return ServiceQuoteLine.create(UUID.randomUUID(), quote.getId(), UUID.randomUUID(), null,
                "Labor", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, "EUR",
                java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO, java.math.BigDecimal.TEN,
                0, UUID.randomUUID(), OffsetDateTime.now());
    }
}