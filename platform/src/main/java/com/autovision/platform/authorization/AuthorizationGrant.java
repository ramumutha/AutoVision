package com.autovision.platform.authorization;

import java.util.UUID;

/**
 * Minimal projection of an active assignment granting a requested permission
 * at a given scope. Deliberately excludes role/permission-set/principal internals.
 */
public record AuthorizationGrant(
        AuthorizationScopeType scopeType,
        UUID scopeId
) {
    public AuthorizationGrant {
        if (scopeType == null) {
            throw new IllegalArgumentException("scopeType must not be null");
        }
        if (scopeId == null) {
            throw new IllegalArgumentException("scopeId must not be null");
        }
    }
}
