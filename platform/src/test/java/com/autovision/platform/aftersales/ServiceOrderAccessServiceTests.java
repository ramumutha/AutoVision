package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderAccessServiceTests {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceJobRepository jobRepository;

    @Mock
    private ServiceLineRepository lineRepository;

    private ServiceOrderAccessService service;

    @BeforeEach
    void setUp() {
        service = new ServiceOrderAccessService(
                orderRepository,
                jobRepository,
                lineRepository
        );
    }

    @Test
    void resolvesOrderOnlyInsideAuthenticatedTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        ServiceOrder result =
                service.requireOrder(
                        context,
                        orderId
                );

        assertSame(order, result);

        verify(orderRepository)
                .findByIdAndTenantId(
                        orderId,
                        context.tenantId()
                );
    }

    @Test
    void hidesOrderOutsideAuthenticatedTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requireOrder(
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
    void doesNotQueryJobsWhenParentOrderIsOutsideTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.findJobs(
                        context,
                        orderId
                )
        );

        verify(
                jobRepository,
                never()
        ).findAllByServiceOrderIdOrderByJobNumber(
                orderId
        );
    }

    @Test
    void resolvesJobOnlyThroughContainedParentOrder() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        ServiceJob job =
                org.mockito.Mockito.mock(ServiceJob.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                jobId,
                orderId
        )).thenReturn(Optional.of(job));

        assertSame(
                job,
                service.requireJob(
                        context,
                        orderId,
                        jobId
                )
        );
    }

    @Test
    void hidesJobBelongingToDifferentOrder() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                jobId,
                orderId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requireJob(
                                context,
                                orderId,
                                jobId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void doesNotQueryLinesWhenParentOrderIsOutsideTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.findLines(
                        context,
                        orderId
                )
        );

        verify(
                lineRepository,
                never()
        ).findAllByServiceOrderIdOrderByLineNumber(
                orderId
        );
    }

    @Test
    void resolvesLineOnlyThroughContainedParentOrder() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        ServiceLine line =
                org.mockito.Mockito.mock(ServiceLine.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(lineRepository.findByIdAndServiceOrderId(
                lineId,
                orderId
        )).thenReturn(Optional.of(line));

        assertSame(
                line,
                service.requireLine(
                        context,
                        orderId,
                        lineId
                )
        );
    }

    @Test
    void hidesLineBelongingToDifferentOrder() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(lineRepository.findByIdAndServiceOrderId(
                lineId,
                orderId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requireLine(
                                context,
                                orderId,
                                lineId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void validatesJobBeforeLoadingItsLines() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                jobId,
                orderId
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.findLinesForJob(
                        context,
                        orderId,
                        jobId
                )
        );

        verify(
                lineRepository,
                never()
        ).findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
                orderId,
                jobId
        );
    }

    @Test
    void loadsJobLinesOnlyAfterOrderAndJobContainmentPass() {

        AuthenticatedTenantContext context = context();

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        ServiceJob job =
                org.mockito.Mockito.mock(ServiceJob.class);

        ServiceLine line =
                org.mockito.Mockito.mock(ServiceLine.class);

        when(order.getId()).thenReturn(orderId);
        when(job.getId()).thenReturn(jobId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository.findByIdAndServiceOrderId(
                jobId,
                orderId
        )).thenReturn(Optional.of(job));

        when(
                lineRepository
                        .findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
                                orderId,
                                jobId
                        )
        ).thenReturn(List.of(line));

        List<ServiceLine> result =
                service.findLinesForJob(
                        context,
                        orderId,
                        jobId
                );

        assertEquals(1, result.size());
        assertSame(line, result.getFirst());
    }

    @Test
    void rejectsMissingAuthenticatedTenantContext() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.requireOrder(
                        null,
                        UUID.randomUUID()
                )
        );

        verify(
                orderRepository,
                never()
        ).findByIdAndTenantId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private AuthenticatedTenantContext context() {
        return new AuthenticatedTenantContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test-user"
        );
    }
}