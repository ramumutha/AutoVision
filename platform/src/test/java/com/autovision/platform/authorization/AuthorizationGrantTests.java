package com.autovision.platform.authorization;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationGrantTests {

    @Test
    void carriesScopeTypeAndScopeId() {
        UUID scopeId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                scopeId
        );

        assertEquals(AuthorizationScopeType.DEALER, grant.scopeType());
        assertEquals(scopeId, grant.scopeId());
    }

    @Test
    void rejectsNullScopeType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationGrant(null, UUID.randomUUID())
        );
    }

    @Test
    void rejectsNullScopeId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationGrant(
                        AuthorizationScopeType.DEALER,
                        null
                )
        );
    }
}
