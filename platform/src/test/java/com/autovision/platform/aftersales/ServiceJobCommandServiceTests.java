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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;

@ExtendWith(MockitoExtension.class)
class ServiceJobCommandServiceTests {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceJobRepository jobRepository;

    @Mock
    private AuthorizationService authorizationService;

    private ServiceJobCommandService service;

    private final ServiceLifecyclePolicy lifecyclePolicy =
            new DefaultServiceLifecyclePolicy();

    @BeforeEach
    void setUp() {
        service = new ServiceJobCommandService(
                orderRepository,
                jobRepository,
                authorizationService,
                lifecyclePolicy
        );
    }

    @Test
    void createsServiceJobWithinAuthorizedServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        String jobNumber = "JOB-CREATE-001";
        String summary = "Initial diagnostic work";

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));
        when(jobRepository.existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        )).thenReturn(false);
        when(jobRepository.save(any(ServiceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceJob result = service.create(
                context,
                order.getId(),
                jobNumber,
                summary,
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        assertEquals(order.getId(), result.getServiceOrderId());
        assertEquals(jobNumber, result.getJobNumber());
        assertEquals(summary, result.getSummary());
        assertEquals(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                result.getApprovalStatus()
        );
        assertEquals(ServiceJobStatus.OPEN, result.getStatus());
        assertEquals(
                context.userRefId(),
                result.getCreatedByPrincipalId()
        );

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                        AuthorizationResourceType.SERVICE_ORDER,
                        order.getId()
                )
        );
        verify(orderRepository).findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        );
        verify(jobRepository).existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        );
        verify(jobRepository).save(any(ServiceJob.class));
    }

    @Test
    void authorizesJobCreationBeforeServiceOrderLookup() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(any(AuthorizationRequest.class));

        assertThrows(
                AccessDeniedException.class,
                () -> service.create(
                        context,
                        orderId,
                        "JOB-AUTH-001",
                        "Unauthorized job",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                )
        );

        verify(orderRepository, never()).findByIdAndTenantId(
                any(),
                any()
        );
        verify(jobRepository, never())
                .existsByServiceOrderIdAndJobNumber(any(), any());
        verify(jobRepository, never()).save(any(ServiceJob.class));
    }

    @Test
    void hidesServiceOrderOutsideAuthenticatedTenantDuringCreation() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        context,
                        orderId,
                        "JOB-NOT-FOUND-001",
                        "Missing order job",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(jobRepository, never())
                .existsByServiceOrderIdAndJobNumber(any(), any());
        verify(jobRepository, never()).save(any(ServiceJob.class));
    }

    @Test
    void rejectsDuplicateJobNumberWithinServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        String jobNumber = "JOB-DUPLICATE-001";

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));
        when(jobRepository.existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        context,
                        order.getId(),
                        jobNumber,
                        "Duplicate job",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                )
        );

        assertEquals(CONFLICT.value(), exception.getStatusCode().value());
        verify(jobRepository, never()).save(any(ServiceJob.class));
    }

    @Test
    void scopesDuplicateJobNumberCheckToResolvedServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        String jobNumber = "JOB-SCOPED-001";

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));
        when(jobRepository.existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        )).thenReturn(false);
        when(jobRepository.save(any(ServiceJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(
                context,
                order.getId(),
                jobNumber,
                "Scoped job",
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        verify(jobRepository).existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        );
    }

    @Test
    void rejectsMissingTenantContextBeforeAuthorizationDuringCreation() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        null,
                        UUID.randomUUID(),
                        "JOB-NO-CONTEXT-001",
                        "Missing context",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                )
        );

        verify(authorizationService, never()).requirePermission(
                any(AuthorizationRequest.class)
        );
        verify(orderRepository, never()).findByIdAndTenantId(
                any(),
                any()
        );
    }

    @Test
    void movesNoApprovalRequiredJobThroughCanonicalLifecycle() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        stubContainedJob(
                context,
                order,
                job
        );

        assertSame(
                job,
                service.markReady(
                        context,
                        order.getId(),
                        job.getId()
                )
        );

        assertEquals(
                ServiceJobStatus.READY,
                job.getStatus()
        );

        service.start(
                context,
                order.getId(),
                job.getId()
        );

        assertEquals(
                ServiceJobStatus.IN_PROGRESS,
                job.getStatus()
        );

        service.completeWork(
                context,
                order.getId(),
                job.getId()
        );

        assertEquals(
                ServiceJobStatus.WORK_COMPLETED,
                job.getStatus()
        );

        service.close(
                context,
                order.getId(),
                job.getId()
        );

        assertEquals(
                ServiceJobStatus.CLOSED,
                job.getStatus()
        );

        assertEquals(
                context.userRefId(),
                job.getUpdatedByPrincipalId()
        );
    }

    @Test
    void allowsApprovedJobToBecomeReady() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.APPROVED
        );

        stubContainedJob(context, order, job);

        service.markReady(
                context,
                order.getId(),
                job.getId()
        );

        assertEquals(
                ServiceJobStatus.READY,
                job.getStatus()
        );
    }

    @Test
    void rejectsPendingApprovalJobFromBecomingReady() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.PENDING
        );

        stubContainedJob(context, order, job);

        assertThrows(
                IllegalStateException.class,
                () -> service.markReady(
                        context,
                        order.getId(),
                        job.getId()
                )
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                job.getStatus()
        );
    }

    @Test
    void rejectsDeclinedApprovalJobFromBecomingReady() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.DECLINED
        );

        stubContainedJob(context, order, job);

        assertThrows(
                IllegalStateException.class,
                () -> service.markReady(
                        context,
                        order.getId(),
                        job.getId()
                )
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                job.getStatus()
        );
    }

    @Test
    void cancelsContainedOpenJob() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        stubContainedJob(context, order, job);

        ServiceJob result =
                service.cancel(
                        context,
                        order.getId(),
                        job.getId()
                );

        assertSame(job, result);

        assertEquals(
                ServiceJobStatus.CANCELLED,
                job.getStatus()
        );

        assertEquals(
                context.userRefId(),
                job.getUpdatedByPrincipalId()
        );
    }

    @Test
    void authorizesParentServiceOrderBeforeRepositoryAccess() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AuthorizationRequest request =
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                        AuthorizationResourceType.SERVICE_ORDER,
                        orderId
                );

        doThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        ).when(authorizationService)
                .requirePermission(request);

        assertThrows(
                AccessDeniedException.class,
                () -> service.markReady(
                        context,
                        orderId,
                        jobId
                )
        );

        verify(authorizationService)
                .requirePermission(request);

        verify(
                orderRepository,
                never()
        ).findByIdAndTenantId(
                any(),
                any()
        );

        verify(
                jobRepository,
                never()
        ).findByIdAndServiceOrderId(
                any(),
                any()
        );
    }

    @Test
    void hidesServiceOrderOutsideAuthenticatedTenant() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.start(
                                context,
                                orderId,
                                jobId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(
                jobRepository,
                never()
        ).findByIdAndServiceOrderId(
                any(),
                any()
        );
    }

    @Test
    void hidesJobOutsideRequestedServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        UUID jobId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                jobId,
                order.getId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.start(
                                context,
                                order.getId(),
                                jobId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void rejectsSkippingReadyState() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        stubContainedJob(context, order, job);

        assertThrows(
                IllegalStateException.class,
                () -> service.start(
                        context,
                        order.getId(),
                        job.getId()
                )
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                job.getStatus()
        );
    }

    @Test
    void rejectsTransitionAfterTerminalState() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob job = job(
                order,
                ServiceJobApprovalStatus.NOT_REQUIRED
        );

        stubContainedJob(context, order, job);

        service.cancel(
                context,
                order.getId(),
                job.getId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.markReady(
                        context,
                        order.getId(),
                        job.getId()
                )
        );
    }

    @Test
    void rejectsMissingTenantContextBeforeAuthorization() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.markReady(
                        null,
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
        );

        verify(
                authorizationService,
                never()
        ).requirePermission(
                any(AuthorizationRequest.class)
        );

        verify(
                orderRepository,
                never()
        ).findByIdAndTenantId(
                any(),
                any()
        );
    }

    private void stubContainedJob(
            AuthenticatedTenantContext context,
            ServiceOrder order,
            ServiceJob job
    ) {
        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                job.getId(),
                order.getId()
        )).thenReturn(Optional.of(job));
    }

    private AuthenticatedTenantContext context() {
        return new AuthenticatedTenantContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "service-advisor"
        );
    }

    private ServiceOrder order(
            AuthenticatedTenantContext context
    ) {
        return ServiceOrder.open(
                UUID.randomUUID(),
                context.tenantId(),
                null,
                null,
                "SO-" + UUID.randomUUID(),
                UUID.randomUUID(),
                context.userRefId(),
                OffsetDateTime.now()
        );
    }

    private ServiceJob job(
            ServiceOrder order,
            ServiceJobApprovalStatus approvalStatus
    ) {
        return ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                "JOB-" + UUID.randomUUID(),
                "Service job command test",
                approvalStatus,
                UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }
}