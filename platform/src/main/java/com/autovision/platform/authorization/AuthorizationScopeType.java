package com.autovision.platform.authorization;

/**
 * Scope types an active role assignment may be granted at.
 * SYSTEM scope remains explicitly deferred.
 */
public enum AuthorizationScopeType {
    TENANT_GROUP,
    TENANT,
    DEALER_GROUP,
    DEALER,
    BRANCH,
    LOCATION
}
