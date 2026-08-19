package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.ServiceOrderRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderWorkflowExecutionReadServiceTests {

    @Mock private ServiceOrderRepository orderRepository;
    @Mock private ServiceOrderWorkflowExecutionRepository executionRepository;
    @Mock private AuthorizationService authorizationService;

    private ServiceOrderWorkflowExecutionReadService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "user");

    @BeforeEach
    void setUp() {
        service = new ServiceOrderWorkflowExecutionReadService(
                orderRepository, executionRepository, authorizationService);
    }

    @Test
    void returnsTenantContainedExecutionWithPinnedIdentities() {
        ServiceOrderWorkflowExecution execution = execution();
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(
                        com.autovision.platform.aftersales.ServiceOrder.class)));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution));

        assertEquals(execution, service.getForServiceOrder(context, orderId));
        verify(authorizationService).requirePermission(new AuthorizationRequest(
                context,
                com.autovision.platform.aftersales.AfterSalesPermissions.SERVICE_ORDER_READ,
                AuthorizationResourceType.SERVICE_ORDER,
                orderId));
        verify(executionRepository).findByServiceOrderIdAndTenantId(orderId, tenantId);
    }

    @Test
    void authorizationDenialPreventsRepositoryDisclosure() {
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService)
                .requirePermission(new AuthorizationRequest(
                        context,
                        com.autovision.platform.aftersales.AfterSalesPermissions.SERVICE_ORDER_READ,
                        AuthorizationResourceType.SERVICE_ORDER,
                        orderId));

        assertThrows(AccessDeniedException.class,
                () -> service.getForServiceOrder(context, orderId));

        verifyNoInteractions(orderRepository, executionRepository);
    }

    @Test
    void missingOrCrossTenantOrderIsNotFoundBeforeExecutionLookup() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.getForServiceOrder(context, orderId))
                .getStatusCode().value());
        verifyNoInteractions(executionRepository);
    }

    @Test
    void missingAssignmentIsNotFound() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(
                        com.autovision.platform.aftersales.ServiceOrder.class)));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.getForServiceOrder(context, orderId))
                .getStatusCode().value());
    }

    @Test
    void noConfigurationRepositoryIsConsulted() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(
                        com.autovision.platform.aftersales.ServiceOrder.class)));
        when(executionRepository.findByServiceOrderIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(execution()));

        service.getForServiceOrder(context, orderId);

        verify(executionRepository, never()).findByServiceOrderId(orderId);
    }

    private ServiceOrderWorkflowExecution execution() {
        return ServiceOrderWorkflowExecution.start(
                UUID.randomUUID(), tenantId, orderId,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), principalId, OffsetDateTime.now());
    }
}