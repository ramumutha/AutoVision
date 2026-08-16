package com.autovision.platform.authorization;

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

    public AuthorizationService(
            AuthorizationRepository authorizationRepository
    ) {
        this.authorizationRepository = authorizationRepository;
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

    /**
     * Resource-aware authorization contract foundation (S4.7.7.3A).
     * Hierarchical scope containment (TENANT_GROUP/DEALER_GROUP/DEALER/BRANCH/
     * LOCATION) is not implemented yet, so this deliberately denies by default
     * until real scope evaluation is introduced in a later controlled slice.
     */
    public boolean hasPermission(AuthorizationRequest request) {
        log.info(
                "authorization decision permission={} tenantId={} resourceType={} decision=DENY",
                request.permissionCode(),
                request.context().tenantId(),
                request.resourceType()
        );

        return false;
    }

    public void requirePermission(AuthorizationRequest request) {
        if (!hasPermission(request)) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
