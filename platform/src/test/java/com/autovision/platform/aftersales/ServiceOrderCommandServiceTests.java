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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderCommandServiceTests {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceJobRepository jobRepository;

    @Mock
    private ServiceLineRepository lineRepository;

    @Mock
    private AuthorizationService authorizationService;

    private ServiceOrderCommandService service;

    private final ServiceLifecyclePolicy lifecyclePolicy =
            new DefaultServiceLifecyclePolicy();

    private final ServiceOrderAggregateLifecycle aggregateLifecycle =
            new ServiceOrderAggregateLifecycle();

    private final ServiceOrderAggregatePolicy aggregatePolicy =
            new DefaultServiceOrderAggregatePolicy();

    @BeforeEach
    void setUp() {
        service = new ServiceOrderCommandService(
                orderRepository,
                jobRepository,
                lineRepository,
                authorizationService,
                lifecyclePolicy,
                aggregateLifecycle,
                aggregatePolicy
        );
    }

    @Test
    void startsAuthorizedServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        ServiceOrder result =
                service.start(
                        context,
                        order.getId()
                );

        assertSame(order, result);

        assertEquals(
                ServiceOrderStatus.IN_PROGRESS,
                result.getStatus()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                                AuthorizationResourceType.SERVICE_ORDER,
                                order.getId()
                        )
                );
    }

    @Test
    void cancelsAuthorizedServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        ServiceOrder result =
                service.cancel(
                        context,
                        order.getId()
                );

        assertSame(order, result);

        assertEquals(
                ServiceOrderStatus.CANCELLED,
                result.getStatus()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void authorizationOccursBeforeRepositoryAccess() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

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
                () -> service.start(
                        context,
                        orderId
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
    }

    @Test
    void hidesServiceOrderOutsideAuthenticatedTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.start(
                                context,
                                orderId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void rejectsRepeatedStartThroughDomainLifecyclePolicy() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        service.start(
                context,
                order.getId()
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.start(
                                context,
                                order.getId()
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Service order transition is not allowed"
                        )
        );
    }

    @Test
    void rejectsCancellationAfterOrderIsAlreadyCancelled() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        service.cancel(
                context,
                order.getId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.cancel(
                        context,
                        order.getId()
                )
        );
    }

    @Test
    void rejectsMissingAuthenticatedTenantContextBeforeAuthorization() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.start(
                        null,
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

    @Test
    void completesDirectWorkOrderWithoutServiceJobs() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceLine line =
                ServiceLine.create(
                        UUID.randomUUID(),
                        order.getId(),
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Direct diagnostic work",
                        java.math.BigDecimal.ONE,
                        "HOUR",
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of(line));

        ServiceOrder result =
                service.completeWork(
                        context,
                        order.getId()
                );

        assertSame(order, result);

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                result.getStatus()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void completesOrderWhenAllJobsAreResolved() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob completedJob =
                ServiceJob.open(
                        UUID.randomUUID(),
                        order.getId(),
                        "JOB-001",
                        "Completed work",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        completedJob.markReady(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        completedJob.start(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        completedJob.completeWork(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(
                        java.util.List.of(completedJob)
                );

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        ServiceOrder result =
                service.completeWork(
                        context,
                        order.getId()
                );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                result.getStatus()
        );
    }

    @Test
    void rejectsCompletionWhileServiceJobIsStillOpen() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob openJob =
                ServiceJob.open(
                        UUID.randomUUID(),
                        order.getId(),
                        "JOB-OPEN",
                        "Unresolved job",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(
                        java.util.List.of(openJob)
                );

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        assertThrows(
                IllegalStateException.class,
                () -> service.completeWork(
                        context,
                        order.getId()
                )
        );

        assertEquals(
                ServiceOrderStatus.OPEN,
                order.getStatus()
        );
    }

    @Test
    void rejectsCompletionOfEmptyServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        assertThrows(
                IllegalStateException.class,
                () -> service.completeWork(
                        context,
                        order.getId()
                )
        );

        assertEquals(
                ServiceOrderStatus.OPEN,
                order.getStatus()
        );
    }

    @Test
    void closesCompletedDirectWorkOrderWithoutJobs() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceLine line =
                ServiceLine.create(
                        UUID.randomUUID(),
                        order.getId(),
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Direct work",
                        java.math.BigDecimal.ONE,
                        "HOUR",
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of(line));

        service.completeWork(
                context,
                order.getId()
        );

        ServiceOrder result =
                service.close(
                        context,
                        order.getId()
                );

        assertEquals(
                ServiceOrderStatus.CLOSED,
                result.getStatus()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void closesOrderOnlyAfterServiceJobsAreClosedOrCancelled() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob closedJob =
                ServiceJob.open(
                        UUID.randomUUID(),
                        order.getId(),
                        "JOB-001",
                        "Closed job",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        closedJob.markReady(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        closedJob.start(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        closedJob.completeWork(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        closedJob.close(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(
                        java.util.List.of(closedJob)
                );

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        service.completeWork(
                context,
                order.getId()
        );

        ServiceOrder result =
                service.close(
                        context,
                        order.getId()
                );

        assertEquals(
                ServiceOrderStatus.CLOSED,
                result.getStatus()
        );
    }

    @Test
    void rejectsCloseWhileJobIsOnlyWorkCompleted() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceJob completedJob =
                ServiceJob.open(
                        UUID.randomUUID(),
                        order.getId(),
                        "JOB-001",
                        "Completed job",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        context.userRefId(),
                        OffsetDateTime.now()
                );

        completedJob.markReady(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        completedJob.start(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        completedJob.completeWork(
                lifecyclePolicy,
                context.userRefId(),
                OffsetDateTime.now()
        );

        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                )).thenReturn(
                        java.util.List.of(completedJob)
                );

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                )).thenReturn(java.util.List.of());

        service.completeWork(
                context,
                order.getId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.close(
                        context,
                        order.getId()
                )
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
    }

    @Test
    void doesNotLoadAggregateChildrenWhenUpdateAuthorizationFails() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

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
                () -> service.completeWork(
                        context,
                        orderId
                )
        );

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
        ).findAllByServiceOrderIdOrderByJobNumber(
                any()
        );

        verify(
                lineRepository,
                never()
        ).findAllByServiceOrderIdOrderByLineNumber(
                any()
        );
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
}