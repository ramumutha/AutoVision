package com.autovision.platform.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AuthorizationResourceTypeTests {

    @Test
    void containsExactlyExpectedValues() {
        assertArrayEquals(
                new AuthorizationResourceType[] {
                        AuthorizationResourceType.TENANT,
                        AuthorizationResourceType.DEALER,
                        AuthorizationResourceType.BRANCH,
                        AuthorizationResourceType.LOCATION,
                AuthorizationResourceType.AFTERSALES_CASE
                },
                AuthorizationResourceType.values()
        );
    }
}
