package com.autovision.platform.serviceprofit.data;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service Profit dealer-data capability read API.
 *
 * The caller cannot supply tenant scope. Tenant authority is resolved from
 * the authenticated identity before the read service performs authorization.
 */
@RestController
@RequestMapping("/api/v1/service-profit/data-capability")
public class DealerDataCapabilityController {

    private final TenantContextResolver tenantContextResolver;
    private final DealerDataCapabilityReadService readService;

    public DealerDataCapabilityController(
            TenantContextResolver tenantContextResolver,
            DealerDataCapabilityReadService readService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.readService = readService;
    }

    @GetMapping
    public DealerDataCapabilityResponse getCurrent(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return readService.findCurrent(tenantContext);
    }
}