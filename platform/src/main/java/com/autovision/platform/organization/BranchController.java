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
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;
    private final TenantContextResolver tenantContextResolver;

    public BranchController(
            BranchService branchService,
            TenantContextResolver tenantContextResolver
    ) {
        this.branchService = branchService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<BranchResponse> findAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return branchService.findAll(context);
    }

    @GetMapping("/{branchId}")
    public BranchResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return branchService.findById(
                context,
                branchId
        );
    }
}