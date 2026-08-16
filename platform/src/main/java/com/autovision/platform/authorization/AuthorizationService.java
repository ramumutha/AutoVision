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
}
