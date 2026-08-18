package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceOrderCommandService {

    private final ServiceOrderRepository orderRepository;
    private final AuthorizationService authorizationService;
    private final ServiceLifecyclePolicy lifecyclePolicy;

    public ServiceOrderCommandService(
            ServiceOrderRepository orderRepository,
            AuthorizationService authorizationService,
            ServiceLifecyclePolicy lifecyclePolicy
    ) {
        this.orderRepository = orderRepository;
        this.authorizationService = authorizationService;
        this.lifecyclePolicy = lifecyclePolicy;
    }

    @Transactional
    public ServiceOrder start(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        order.start(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
    }

    @Transactional
    public ServiceOrder cancel(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        order.cancel(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
    }

    private ServiceOrder requireMutableOrder(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                        AuthorizationResourceType.SERVICE_ORDER,
                        orderId
                )
        );

        return orderRepository.findByIdAndTenantId(
                orderId,
                tenantContext.tenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "Service order not found"
        ));
    }

    private void requireTenantContext(
            AuthenticatedTenantContext tenantContext
    ) {
        if (
                tenantContext == null
                        || tenantContext.tenantId() == null
        ) {
            throw new IllegalArgumentException(
                    "Authenticated tenant context is required"
            );
        }
    }
}