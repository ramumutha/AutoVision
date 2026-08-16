package com.autovision.platform.authorization;

import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

/**
 * Resource-aware authorization request for hierarchical scope evaluation.
 * Contract foundation only; scope evaluation lands in a later slice.
 */
public record AuthorizationRequest(
        AuthenticatedTenantContext context,
        String permissionCode,
        AuthorizationResourceType resourceType,
        UUID resourceId
) {
    public AuthorizationRequest {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("permissionCode must not be blank");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType must not be null");
        }
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId must not be null");
        }
    }
}
