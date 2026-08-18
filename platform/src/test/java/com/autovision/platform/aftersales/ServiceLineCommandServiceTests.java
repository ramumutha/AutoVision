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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceLineCommandServiceTests {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceJobRepository jobRepository;

    @Mock
    private ServiceLineRepository lineRepository;

    @Mock
    private AuthorizationService authorizationService;

    private ServiceLineCommandService service;

    @BeforeEach
    void setUp() {
        service = new ServiceLineCommandService(
                orderRepository,
                jobRepository,
                lineRepository,
                authorizationService
        );
    }

    @Test
    void createsDirectOrderLevelServiceLine() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.existsByServiceOrderIdAndLineNumber(
                order.getId(),
                10
        )).thenReturn(false);

        when(lineRepository.save(any(ServiceLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceLine result =
                service.create(
                        context,
                        order.getId(),
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Diagnostic labor",
                        new BigDecimal("1.5000"),
                        "HOUR"
                );

        assertEquals(
                order.getId(),
                result.getServiceOrderId()
        );

        assertNull(
                result.getServiceJobId()
        );

        assertEquals(
                10,
                result.getLineNumber()
        );

        assertEquals(
                context.userRefId(),
                result.getCreatedByPrincipalId()
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
    void createsServiceLineAssignedToContainedJob() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceJob job = job(order);

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.existsByServiceOrderIdAndLineNumber(
                order.getId(),
                20
        )).thenReturn(false);

        when(jobRepository.findByIdAndServiceOrderId(
                job.getId(),
                order.getId()
        )).thenReturn(Optional.of(job));

        when(lineRepository.save(any(ServiceLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceLine result =
                service.create(
                        context,
                        order.getId(),
                        job.getId(),
                        20,
                        ServiceLineType.PART,
                        "Brake pad",
                        new BigDecimal("2.0000"),
                        "EA"
                );

        assertEquals(
                job.getId(),
                result.getServiceJobId()
        );
    }

    @Test
    void rejectsDuplicateLineNumberWithinServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.existsByServiceOrderIdAndLineNumber(
                order.getId(),
                10
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.create(
                                context,
                                order.getId(),
                                null,
                                10,
                                ServiceLineType.LABOR,
                                "Duplicate line",
                                BigDecimal.ONE,
                                "HOUR"
                        )
                );

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        verify(
                lineRepository,
                never()
        ).save(any(ServiceLine.class));
    }

    @Test
    void rejectsCreateWhenTargetJobBelongsOutsideOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        UUID foreignJobId = UUID.randomUUID();

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.existsByServiceOrderIdAndLineNumber(
                order.getId(),
                10
        )).thenReturn(false);

        when(jobRepository.findByIdAndServiceOrderId(
                foreignJobId,
                order.getId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.create(
                                context,
                                order.getId(),
                                foreignJobId,
                                10,
                                ServiceLineType.LABOR,
                                "Invalid job line",
                                BigDecimal.ONE,
                                "HOUR"
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(
                lineRepository,
                never()
        ).save(any(ServiceLine.class));
    }

    @Test
    void updatesContainedServiceLineDetails() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceLine line = line(order, null);

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.findByIdAndServiceOrderId(
                line.getId(),
                order.getId()
        )).thenReturn(Optional.of(line));

        ServiceLine result =
                service.updateDetails(
                        context,
                        order.getId(),
                        line.getId(),
                        ServiceLineType.PART,
                        "Updated part",
                        new BigDecimal("3.0000"),
                        "EA"
                );

        assertSame(line, result);

        assertEquals(
                ServiceLineType.PART,
                result.getLineType()
        );

        assertEquals(
                "Updated part",
                result.getDescription()
        );

        assertEquals(
                0,
                new BigDecimal("3.0000")
                        .compareTo(result.getQuantity())
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void assignsContainedLineToContainedJob() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceLine line =
                line(
                        order,
                        null
                );

        ServiceJob job =
                job(order);

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.findByIdAndServiceOrderId(
                line.getId(),
                order.getId()
        )).thenReturn(Optional.of(line));

        when(jobRepository.findByIdAndServiceOrderId(
                job.getId(),
                order.getId()
        )).thenReturn(Optional.of(job));

        ServiceLine result =
                service.assignToJob(
                        context,
                        order.getId(),
                        line.getId(),
                        job.getId()
                );

        assertSame(line, result);

        assertEquals(
                job.getId(),
                result.getServiceJobId()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void rejectsAssignmentToJobOutsideServiceOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        ServiceLine line =
                line(
                        order,
                        null
                );

        UUID foreignJobId = UUID.randomUUID();

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.findByIdAndServiceOrderId(
                line.getId(),
                order.getId()
        )).thenReturn(Optional.of(line));

        when(jobRepository.findByIdAndServiceOrderId(
                foreignJobId,
                order.getId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.assignToJob(
                                context,
                                order.getId(),
                                line.getId(),
                                foreignJobId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        assertNull(
                line.getServiceJobId()
        );
    }

    @Test
    void unassignsContainedServiceLineFromJob() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);
        ServiceJob job = job(order);

        ServiceLine line =
                line(
                        order,
                        job.getId()
                );

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.findByIdAndServiceOrderId(
                line.getId(),
                order.getId()
        )).thenReturn(Optional.of(line));

        ServiceLine result =
                service.unassignFromJob(
                        context,
                        order.getId(),
                        line.getId()
                );

        assertSame(line, result);

        assertNull(
                result.getServiceJobId()
        );

        assertEquals(
                context.userRefId(),
                result.getUpdatedByPrincipalId()
        );
    }

    @Test
    void authorizesParentOrderBeforeRepositoryAccess() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

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
                () -> service.updateDetails(
                        context,
                        orderId,
                        lineId,
                        ServiceLineType.LABOR,
                        "Updated labor",
                        BigDecimal.ONE,
                        "HOUR"
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
                lineRepository,
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
        UUID lineId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.unassignFromJob(
                                context,
                                orderId,
                                lineId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(
                lineRepository,
                never()
        ).findByIdAndServiceOrderId(
                any(),
                any()
        );
    }

    @Test
    void hidesServiceLineOutsideRequestedOrder() {

        AuthenticatedTenantContext context = context();
        ServiceOrder order = order(context);

        UUID lineId = UUID.randomUUID();

        stubContainedOrder(
                context,
                order
        );

        when(lineRepository.findByIdAndServiceOrderId(
                lineId,
                order.getId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.updateDetails(
                                context,
                                order.getId(),
                                lineId,
                                ServiceLineType.LABOR,
                                "Updated labor",
                                BigDecimal.ONE,
                                "HOUR"
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void rejectsMissingTenantContextBeforeAuthorization() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        null,
                        UUID.randomUUID(),
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Labor",
                        BigDecimal.ONE,
                        "HOUR"
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

    private void stubContainedOrder(
            AuthenticatedTenantContext context,
            ServiceOrder order
    ) {
        when(orderRepository.findByIdAndTenantId(
                order.getId(),
                context.tenantId()
        )).thenReturn(Optional.of(order));
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
            ServiceOrder order
    ) {
        return ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                "JOB-" + UUID.randomUUID(),
                "Service line command test job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }

    private ServiceLine line(
            ServiceOrder order,
            UUID serviceJobId
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                order.getId(),
                serviceJobId,
                10,
                ServiceLineType.LABOR,
                "Service line command test",
                BigDecimal.ONE,
                "HOUR",
                UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }
}