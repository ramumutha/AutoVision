package com.autovision.platform.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AuthorizationScopeTypeTests {

    @Test
    void containsExactlyExpectedValues() {
        assertArrayEquals(
                new AuthorizationScopeType[] {
                    AuthorizationScopeType.SYSTEM,
                    AuthorizationScopeType.TENANT_GROUP,
                    AuthorizationScopeType.TENANT,
                    AuthorizationScopeType.DEALER_GROUP,
                    AuthorizationScopeType.DEALER,
                    AuthorizationScopeType.BRANCH,
                    AuthorizationScopeType.LOCATION
                },
                AuthorizationScopeType.values()
        );
    }
}
