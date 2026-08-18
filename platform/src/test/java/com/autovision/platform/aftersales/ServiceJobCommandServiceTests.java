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