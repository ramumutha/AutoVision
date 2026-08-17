package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-orders")
public class ServiceOrderController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceOrderAccessService accessService;

    public ServiceOrderController(
            TenantContextResolver tenantContextResolver,
            ServiceOrderAccessService accessService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.accessService = accessService;
    }

    @GetMapping("/{orderId}/aggregate")
    public ServiceOrderAggregateResponse aggregate(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceOrderAggregateView aggregate =
                accessService.requireAggregate(
                        tenantContext,
                        orderId
                );

        return ServiceOrderAggregateResponse.from(
                aggregate
        );
    }
}