package com.autovision.platform.organization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dealers")
public class DealerController {

    private final DealerService dealerService;
    private final TenantContextResolver tenantContextResolver;

    public DealerController(
            DealerService dealerService,
            TenantContextResolver tenantContextResolver
    ) {
        this.dealerService = dealerService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<DealerResponse> findAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return dealerService.findAll(context);
    }

    @GetMapping("/{dealerId}")
    public DealerResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID dealerId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return dealerService.findById(
                context,
                dealerId
        );
    }
}