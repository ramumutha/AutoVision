package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperationalAuthorizationEvaluationServiceTests {

    private final ServiceOrderRepository orderRepository =
            mock(ServiceOrderRepository.class);
    private final ServiceJobRepository jobRepository =
            mock(ServiceJobRepository.class);
    private final ServiceLineRepository lineRepository =
            mock(ServiceLineRepository.class);
    private final ServiceQuoteRepository quoteRepository =
            mock(ServiceQuoteRepository.class);
    private final ServiceQuoteLineRepository quoteLineRepository =
            mock(ServiceQuoteLineRepository.class);
    private final CustomerAuthorizationRepository authorizationRepository =
            mock(CustomerAuthorizationRepository.class);
    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final OperationalAuthorizationEvaluationService service =
            new OperationalAuthorizationEvaluationService(
                    orderRepository,
                    jobRepository,
                    lineRepository,
                    quoteRepository,
                    quoteLineRepository,
                    authorizationRepository,
                    authorizationService
            );

    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "advisor");

    @Test
    void notRequiredReportsCurrentLineCountWithoutEvidence() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        ServiceOrder order = order(orderId);
        ServiceJob job = job(jobId, orderId, ServiceJobApprovalStatus.NOT_REQUIRED);
        List<ServiceLine> lines = List.of(line(UUID.randomUUID(), orderId, jobId, 1));
        prepareJob(order, job, lines);

        OperationalAuthorizationEvaluation result =
                service.evaluate(context, orderId, jobId);

        assertEquals(OperationalAuthorizationStatus.NOT_REQUIRED, result.status());
        assertEquals(1, result.totalLineCount());
        assertEquals(0, result.authorizedLineCount());
        assertEquals(0, result.pendingLineCount());
        assertEquals(0, result.notAuthorizedLineCount());
        verifyNoInteractions(quoteRepository, authorizationRepository, quoteLineRepository);
    }

    @Test
    void requiredJobWithNoCurrentLinesIsNotAuthorized() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of());

        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);

        assertEvaluation(result, jobId, orderId, OperationalAuthorizationStatus.NOT_AUTHORIZED, 0, 0, 0, 0);
        verifyNoInteractions(quoteRepository, authorizationRepository, quoteLineRepository);
    }

    @Test
    void authorizedEvidenceProducesFullyAuthorizedAndExactCounts() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID firstLineId = UUID.randomUUID();
        UUID secondLineId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of(
                line(firstLineId, orderId, jobId, 1),
                line(secondLineId, orderId, jobId, 2)
        ));
        prepareEvidence(orderId, quoteId, List.of(
                authorization(quoteId, CustomerAuthorizationStatus.AUTHORIZED),
                authorization(quoteId, CustomerAuthorizationStatus.DECLINED)
        ), List.of(
                quoteLine(quoteId, firstLineId, 1),
                quoteLine(quoteId, secondLineId, 2)
        ));

        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);

        assertEvaluation(result, jobId, orderId, OperationalAuthorizationStatus.FULLY_AUTHORIZED, 2, 2, 0, 0);
    }

    @Test
    void authorizedTakesPrecedenceOverPendingDeclinedAndCancelledEvidence() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of(line(lineId, orderId, jobId, 1)));
        prepareEvidence(orderId, quoteId, List.of(
                authorization(quoteId, CustomerAuthorizationStatus.REQUESTED),
                authorization(quoteId, CustomerAuthorizationStatus.DEFERRED),
                authorization(quoteId, CustomerAuthorizationStatus.DECLINED),
                authorization(quoteId, CustomerAuthorizationStatus.CANCELLED),
                authorization(quoteId, CustomerAuthorizationStatus.AUTHORIZED)
        ), List.of(quoteLine(quoteId, lineId, 1)));

        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);

        assertEvaluation(result, jobId, orderId, OperationalAuthorizationStatus.FULLY_AUTHORIZED, 1, 1, 0, 0);
    }

    @Test
    void pendingAndNotAuthorizedLineMixProducesPartialOrPendingStates() {
        assertMixedStatus(CustomerAuthorizationStatus.REQUESTED, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
        assertMixedStatus(CustomerAuthorizationStatus.DEFERRED, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
        assertMixedStatus(CustomerAuthorizationStatus.DECLINED, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
        assertMixedStatus(CustomerAuthorizationStatus.CANCELLED, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
        assertMixedStatus(null, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
    }

    @Test
    void noAuthorizedPendingEvidenceProducesPending() {
        assertUnresolvedStatus(CustomerAuthorizationStatus.REQUESTED, OperationalAuthorizationStatus.PENDING);
        assertUnresolvedStatus(CustomerAuthorizationStatus.DEFERRED, OperationalAuthorizationStatus.PENDING);
        assertUnresolvedStatus(CustomerAuthorizationStatus.DECLINED, OperationalAuthorizationStatus.NOT_AUTHORIZED);
        assertUnresolvedStatus(CustomerAuthorizationStatus.CANCELLED, OperationalAuthorizationStatus.NOT_AUTHORIZED);
        assertUnresolvedStatus(null, OperationalAuthorizationStatus.NOT_AUTHORIZED);
    }

    @Test
    void currentJobAssignmentDefinesScopeAndDoesNotInferFromServiceLines() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID assignedLineId = UUID.randomUUID();
        UUID unassignedHistoricalLineId = UUID.randomUUID();
        UUID newlyAssignedLineId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of(
                line(assignedLineId, orderId, jobId, 1),
                line(newlyAssignedLineId, orderId, jobId, 2)
        ));
        prepareEvidence(orderId, quoteId,
                List.of(authorization(quoteId, CustomerAuthorizationStatus.AUTHORIZED)),
                List.of(quoteLine(quoteId, assignedLineId, 1), quoteLine(quoteId, unassignedHistoricalLineId, 2)));

        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);

        assertEvaluation(result, jobId, orderId, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED, 2, 1, 0, 1);
        verify(lineRepository).findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(orderId, jobId);
    }

    @Test
    void permissionAndTenantContainmentAreEnforced() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AuthorizationRequest permission = new AuthorizationRequest(
                context,
                AfterSalesPermissions.SERVICE_ORDER_READ,
                AuthorizationResourceType.SERVICE_ORDER,
                orderId
        );
        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService).requirePermission(permission);
        assertThrows(AccessDeniedException.class, () -> service.evaluate(context, orderId, jobId));
        verifyNoInteractions(orderRepository, jobRepository, lineRepository);

        doNothing().when(authorizationService).requirePermission(permission);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.evaluate(context, orderId, jobId)
        );
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void unknownOrCrossOrderJobIsNotFoundWithoutQuoteAccess() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order(orderId)));
        when(jobRepository.findByIdAndServiceOrderId(jobId, orderId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.evaluate(context, orderId, jobId)
        );

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(lineRepository, quoteRepository, authorizationRepository);
    }

    @Test
    void noEntitySaveOrMutationOccurs() {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of());
        ServiceJobApprovalStatus approvalBefore = jobRepository.findByIdAndServiceOrderId(jobId, orderId)
                .orElseThrow().getApprovalStatus();

        service.evaluate(context, orderId, jobId);

        assertEquals(approvalBefore, jobRepository.findByIdAndServiceOrderId(jobId, orderId)
                .orElseThrow().getApprovalStatus());
        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(jobRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(lineRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(authorizationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void assertMixedStatus(
            CustomerAuthorizationStatus unresolvedStatus,
            OperationalAuthorizationStatus expected
    ) {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID authorizedLineId = UUID.randomUUID();
        UUID unresolvedLineId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of(
                line(authorizedLineId, orderId, jobId, 1), line(unresolvedLineId, orderId, jobId, 2)));
        UUID authorizedQuoteId = UUID.randomUUID();
        UUID unresolvedQuoteId = UUID.randomUUID();
        List<CustomerAuthorization> authorizations =
                unresolvedStatus == null
                        ? List.of(authorization(authorizedQuoteId, CustomerAuthorizationStatus.AUTHORIZED))
                        : List.of(
                                authorization(authorizedQuoteId, CustomerAuthorizationStatus.AUTHORIZED),
                                authorization(unresolvedQuoteId, unresolvedStatus)
                        );
        List<ServiceQuoteLine> quoteLines =
                unresolvedStatus == null
                        ? List.of(quoteLine(authorizedQuoteId, authorizedLineId, 1))
                        : List.of(
                                quoteLine(authorizedQuoteId, authorizedLineId, 1),
                                quoteLine(unresolvedQuoteId, unresolvedLineId, 2)
                        );
        prepareEvidence(
                orderId,
                List.of(
                        quote(authorizedQuoteId, tenantId, orderId, UUID.randomUUID()),
                        quote(unresolvedQuoteId, tenantId, orderId, UUID.randomUUID())
                ),
                authorizations,
                quoteLines
        );
        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);
        assertEvaluation(result, jobId, orderId, expected, 2, 1, unresolvedStatus == null ? 0 : expected == OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED && unresolvedStatus != CustomerAuthorizationStatus.DECLINED && unresolvedStatus != CustomerAuthorizationStatus.CANCELLED ? 1 : 0, unresolvedStatus == null || unresolvedStatus == CustomerAuthorizationStatus.DECLINED || unresolvedStatus == CustomerAuthorizationStatus.CANCELLED ? 1 : 0);
    }

    private void assertUnresolvedStatus(
            CustomerAuthorizationStatus status,
            OperationalAuthorizationStatus expected
    ) {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        prepareJob(order(orderId), job(jobId, orderId, ServiceJobApprovalStatus.PENDING), List.of(line(lineId, orderId, jobId, 1)));
        prepareEvidence(orderId, quoteId, status == null ? List.of() : List.of(authorization(quoteId, status)), List.of(quoteLine(quoteId, lineId, 1)));
        OperationalAuthorizationEvaluation result = service.evaluate(context, orderId, jobId);
        assertEvaluation(result, jobId, orderId, expected, 1, 0, expected == OperationalAuthorizationStatus.PENDING ? 1 : 0, expected == OperationalAuthorizationStatus.NOT_AUTHORIZED ? 1 : 0);
    }

    private void prepareJob(ServiceOrder order, ServiceJob job, List<ServiceLine> lines) {
        when(orderRepository.findByIdAndTenantId(order.getId(), tenantId)).thenReturn(Optional.of(order));
        when(jobRepository.findByIdAndServiceOrderId(job.getId(), order.getId())).thenReturn(Optional.of(job));
        when(lineRepository.findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(order.getId(), job.getId())).thenReturn(lines);
    }

    private void prepareEvidence(UUID orderId, UUID quoteId, List<CustomerAuthorization> authorizations, List<ServiceQuoteLine> quoteLines) {
        prepareEvidence(
                orderId,
                List.of(quote(quoteId, tenantId, orderId, UUID.randomUUID())),
                authorizations,
                quoteLines
        );
    }

    private void prepareEvidence(UUID orderId, List<ServiceQuote> quotes, List<CustomerAuthorization> authorizations, List<ServiceQuoteLine> quoteLines) {
        List<UUID> quoteIds = quotes.stream().map(quote -> quote.getId()).toList();
        when(quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(orderId, tenantId))
                .thenReturn(quotes);
        when(authorizationRepository.findAllByTenantIdAndServiceQuoteIdIn(tenantId, quoteIds))
                .thenReturn(authorizations);
        when(quoteLineRepository.findAllByServiceQuoteIdInOrderByServiceQuoteIdAscSequenceAsc(quoteIds))
                .thenReturn(quoteLines);
    }

    private ServiceOrder order(UUID id) {
        return ServiceOrder.open(id, tenantId, null, null, "ORDER-" + id, UUID.randomUUID(), principalId, OffsetDateTime.now());
    }

    private ServiceJob job(UUID id, UUID orderId, ServiceJobApprovalStatus status) {
        return ServiceJob.open(id, orderId, "JOB-" + id, "Job", status, principalId, OffsetDateTime.now());
    }

    private ServiceLine line(UUID id, UUID orderId, UUID jobId, int number) {
        return ServiceLine.create(id, orderId, jobId, number, ServiceLineType.LABOR, "Line " + number, BigDecimal.ONE, "HOUR", principalId, OffsetDateTime.now());
    }

    private ServiceQuote quote(UUID id, UUID quoteTenantId, UUID orderId, UUID caseId) {
        return ServiceQuote.create(id, quoteTenantId, null, null, caseId, orderId, "QUOTE-" + id, "EUR", null, null, null, principalId, OffsetDateTime.now());
    }

    private ServiceQuoteLine quoteLine(UUID quoteId, UUID serviceLineId, int sequence) {
        return ServiceQuoteLine.create(UUID.randomUUID(), quoteId, serviceLineId, null, "Line", BigDecimal.ONE, BigDecimal.TEN, "EUR", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(11), sequence, principalId, OffsetDateTime.now());
    }

    private CustomerAuthorization authorization(UUID quoteId, CustomerAuthorizationStatus status) {
        CustomerAuthorization authorization = CustomerAuthorization.request(UUID.randomUUID(), tenantId, null, null, UUID.randomUUID(), quoteId, "AUTH", null, null, "Scope", null, null, null, null, principalId, OffsetDateTime.now());
        if (status == CustomerAuthorizationStatus.AUTHORIZED) authorization.authorize(null, null, principalId, OffsetDateTime.now());
        if (status == CustomerAuthorizationStatus.DECLINED) authorization.decline(null, null, principalId, OffsetDateTime.now());
        if (status == CustomerAuthorizationStatus.DEFERRED) authorization.defer(null, null, principalId, OffsetDateTime.now());
        if (status == CustomerAuthorizationStatus.CANCELLED) authorization.cancel(null, null, principalId, OffsetDateTime.now());
        return authorization;
    }

    private void assertEvaluation(OperationalAuthorizationEvaluation result, UUID jobId, UUID orderId, OperationalAuthorizationStatus status, int total, int authorized, int pending, int notAuthorized) {
        assertEquals(jobId, result.serviceJobId());
        assertEquals(orderId, result.serviceOrderId());
        assertEquals(status, result.status());
        assertEquals(total, result.totalLineCount());
        assertEquals(authorized, result.authorizedLineCount());
        assertEquals(pending, result.pendingLineCount());
        assertEquals(notAuthorized, result.notAuthorizedLineCount());
        assertEquals(total, authorized + pending + notAuthorized);
    }
}
