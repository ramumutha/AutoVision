package com.autovision.platform.authorization;

import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationRequestTests {

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.fromString(
                            "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
                    ),
                    UUID.fromString(
                            "2cf85fea-bc61-4405-be50-00a0ca45df3b"
                    ),
                    "svc-advisor-01"
            );

    @Test
    void carriesContextPermissionResourceTypeAndResourceId() {
        UUID resourceId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                resourceId
        );

        assertEquals(context, request.context());
        assertEquals(
                "ORGANIZATION.DEALER.READ",
                request.permissionCode()
        );
        assertEquals(
                AuthorizationResourceType.DEALER,
                request.resourceType()
        );
        assertEquals(resourceId, request.resourceId());
    }

    @Test
    void rejectsNullContext() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationRequest(
                        null,
                        "ORGANIZATION.DEALER.READ",
                        AuthorizationResourceType.DEALER,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsBlankPermissionCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationRequest(
                        context,
                        " ",
                        AuthorizationResourceType.DEALER,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsNullResourceType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationRequest(
                        context,
                        "ORGANIZATION.DEALER.READ",
                        null,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void rejectsNullResourceId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthorizationRequest(
                        context,
                        "ORGANIZATION.DEALER.READ",
                        AuthorizationResourceType.DEALER,
                        null
                )
        );
    }
}
