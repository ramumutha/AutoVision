package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderAggregateReadTests {

    @Mock
    private ServiceOrderRepository orderRepository;

    @Mock
    private ServiceJobRepository jobRepository;

    @Mock
    private ServiceLineRepository lineRepository;

    @Mock
    private AuthorizationService authorizationService;

    private ServiceOrderAccessService service;

    @BeforeEach
    void setUp() {
        service = new ServiceOrderAccessService(
                orderRepository,
                jobRepository,
                lineRepository,
                authorizationService
        );
    }

    @Test
    void loadsContainedOrderWithJobsAndLines() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        ServiceJob job =
                org.mockito.Mockito.mock(ServiceJob.class);

        ServiceLine line =
                org.mockito.Mockito.mock(ServiceLine.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        orderId
                )).thenReturn(List.of(job));

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        orderId
                )).thenReturn(List.of(line));

        ServiceOrderAggregateView result =
                service.requireAggregate(
                        context,
                        orderId
                );

        assertSame(order, result.order());

        assertEquals(1, result.jobs().size());
        assertSame(job, result.jobs().getFirst());

        assertEquals(1, result.lines().size());
        assertSame(line, result.lines().getFirst());
    }

    @Test
    void supportsOrderWithoutJobsOrLines() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        orderId
                )).thenReturn(List.of());

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        orderId
                )).thenReturn(List.of());

        ServiceOrderAggregateView result =
                service.requireAggregate(
                        context,
                        orderId
                );

        assertSame(order, result.order());
        assertTrue(result.jobs().isEmpty());
        assertTrue(result.lines().isEmpty());
    }

    @Test
    void supportsOrderLinesWithoutJobs() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        ServiceOrder order =
                org.mockito.Mockito.mock(ServiceOrder.class);

        ServiceLine line =
                org.mockito.Mockito.mock(ServiceLine.class);

        when(order.getId()).thenReturn(orderId);

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.of(order));

        when(jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        orderId
                )).thenReturn(List.of());

        when(lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        orderId
                )).thenReturn(List.of(line));

        ServiceOrderAggregateView result =
                service.requireAggregate(
                        context,
                        orderId
                );

        assertTrue(result.jobs().isEmpty());
        assertEquals(1, result.lines().size());
        assertSame(line, result.lines().getFirst());
    }

    @Test
    void doesNotLoadChildrenWhenOrderIsOutsideTenant() {

        AuthenticatedTenantContext context = context();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndTenantId(
                orderId,
                context.tenantId()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requireAggregate(
                                context,
                                orderId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        verify(
                jobRepository,
                never()
        ).findAllByServiceOrderIdOrderByJobNumber(
                orderId
        );

        verify(
                lineRepository,
                never()
        ).findAllByServiceOrderIdOrderByLineNumber(
                orderId
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