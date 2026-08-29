package com.autovision.platform.authorization;

import java.util.List;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Tenant-level runtime authorization core (Sprint 4 S4.7.5.2).
 * Application code authorizes against permission codes only, never role names.
 */
@Service
public class AuthorizationService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthorizationService.class);

    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationScopeEvaluator scopeEvaluator;

    public AuthorizationService(
            AuthorizationRepository authorizationRepository,
            AuthorizationScopeEvaluator scopeEvaluator
    ) {
        this.authorizationRepository = authorizationRepository;
        this.scopeEvaluator = scopeEvaluator;
    }

    public boolean hasPermission(
            AuthenticatedTenantContext context,
            String permissionCode
    ) {
        boolean granted = authorizationRepository.hasActiveTenantPermission(
                context.userRefId(),
                context.tenantId(),
                permissionCode
        );

        log.info(
                "authorization decision permission={} tenantId={} decision={}",
                permissionCode,
                context.tenantId(),
                granted ? "ALLOW" : "DENY"
        );

        return granted;
    }

    public void requirePermission(
            AuthenticatedTenantContext context,
            String permissionCode
    ) {
        if (!hasPermission(context, permissionCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

        public void requireSystemPermission(
                        AuthenticatedTenantContext context,
                        String permissionCode
        ) {
                boolean granted = authorizationRepository.hasActiveSystemPermission(
                                context.userRefId(),
                                permissionCode
                );

                log.info(
                                "system authorization decision permission={} userRefId={} decision={}",
                                permissionCode,
                                context.userRefId(),
                                granted ? "ALLOW" : "DENY"
                );

                if (!granted) {
                        throw new AccessDeniedException("Access is denied");
                }
        }

    /**
     * Resource-aware authorization resolves active local and TENANT_GROUP
     * grants and allows only when at least one grant contains the requested
     * resource. SYSTEM scope remains unsupported and deny-by-default.
     */
    public boolean hasPermission(AuthorizationRequest request) {
        List<AuthorizationGrant> grants =
                authorizationRepository.findActivePermissionGrants(
                        request.context().userRefId(),
                        request.context().tenantId(),
                        request.permissionCode()
                );

        boolean granted = grants.stream().anyMatch(grant ->
                scopeEvaluator.contains(
                        grant,
                        request.context().tenantId(),
                        request.resourceType(),
                        request.resourceId()
                )
        );

        log.info(
                "authorization decision permission={} tenantId={} resourceType={} resourceId={} decision={}",
                request.permissionCode(),
                request.context().tenantId(),
                request.resourceType(),
                request.resourceId(),
                granted ? "ALLOW" : "DENY"
        );

        return granted;
    }

    public void requirePermission(AuthorizationRequest request) {
        if (!hasPermission(request)) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
