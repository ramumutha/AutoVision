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
    private AuthorizationService authorizationService;

    private ServiceOrderCommandService service;

    private final ServiceLifecyclePolicy lifecyclePolicy =
            new DefaultServiceLifecyclePolicy();

    @BeforeEach
    void setUp() {
        service = new ServiceOrderCommandService(
                orderRepository,
                authorizationService,
                lifecyclePolicy
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