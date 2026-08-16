package com.autovision.platform.security;

import java.util.Map;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CurrentUserController {

    private final TenantContextResolver tenantContextResolver;

    public CurrentUserController(
            TenantContextResolver tenantContextResolver
    ) {
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return Map.of(
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),
                "userRefId", context.userRefId(),
                "tenantId", context.tenantId(),
                "externalUserId", context.externalUserId()
        );
    }
}