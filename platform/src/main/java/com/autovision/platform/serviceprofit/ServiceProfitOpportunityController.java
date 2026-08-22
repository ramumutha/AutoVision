package com.autovision.platform.serviceprofit;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-profit/opportunities")
public class ServiceProfitOpportunityController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceProfitOpportunityCommandService commandService;
    private final ServiceProfitOpportunityAccessService accessService;

    public ServiceProfitOpportunityController(
            TenantContextResolver tenantContextResolver,
            ServiceProfitOpportunityCommandService commandService,
            ServiceProfitOpportunityAccessService accessService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
        this.accessService = accessService;
    }

    @PostMapping
    public ServiceProfitOpportunityResponse create(
            @RequestBody CreateServiceProfitOpportunityRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return ServiceProfitOpportunityResponse.from(
                commandService.create(
                        tenantContext,
                        request
                )
        );
    }

    @GetMapping("/{opportunityId}")
    public ServiceProfitOpportunityResponse get(
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return ServiceProfitOpportunityResponse.from(
                accessService.requireOpportunity(
                        tenantContext,
                        opportunityId
                )
        );
    }
}
