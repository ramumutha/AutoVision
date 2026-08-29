package com.autovision.platform.authorization;

/**
 * Scope types an active role assignment may be granted at.
 */
public enum AuthorizationScopeType {
    SYSTEM,
    TENANT_GROUP,
    TENANT,
    DEALER_GROUP,
    DEALER,
    BRANCH,
    LOCATION
}
