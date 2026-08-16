package com.autovision.platform.authorization;

/**
 * Resource types requested against the authorization contract.
 * Assignment-only scopes (SYSTEM, TENANT_GROUP, DEALER_GROUP) are excluded.
 */
public enum AuthorizationResourceType {
    TENANT,
    DEALER,
    BRANCH,
    LOCATION
}
