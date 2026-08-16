package com.autovision.platform.authorization;

import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthorizationServiceTests {

    private final AuthorizationRepository repository =
            mock(AuthorizationRepository.class);

    private final AuthorizationService service =
            new AuthorizationService(repository);

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.fromString(
                            "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
                    ),
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void hasPermissionReturnsTrueWhenRepositoryGrants() {
        when(repository.hasActiveTenantPermission(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(true);

        assertTrue(
                service.hasPermission(
                        context,
                        "ORGANIZATION.DEALER.READ"
                )
        );

        verify(repository).hasActiveTenantPermission(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        );
    }

    @Test
    void hasPermissionReturnsFalseWhenRepositoryDenies() {
        when(repository.hasActiveTenantPermission(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(false);

        assertFalse(
                service.hasPermission(
                        context,
                        "ORGANIZATION.DEALER.READ"
                )
        );
    }

    @Test
    void requirePermissionThrowsAccessDeniedWhenNotGranted() {
        when(repository.hasActiveTenantPermission(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.requirePermission(
                        context,
                        "ORGANIZATION.DEALER.READ"
                )
        );
    }

    @Test
    void requirePermissionSucceedsWhenGranted() {
        when(repository.hasActiveTenantPermission(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(true);

        service.requirePermission(
                context,
                "ORGANIZATION.DEALER.READ"
        );
    }

    @Test
    void resourceAwareHasPermissionDeniesByDefault() {
        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                UUID.randomUUID()
        );

        assertFalse(service.hasPermission(request));

        verifyNoInteractions(repository);
    }

    @Test
    void resourceAwareRequirePermissionThrowsByDefault() {
        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.BRANCH.READ",
                AuthorizationResourceType.BRANCH,
                UUID.randomUUID()
        );

        assertThrows(
                AccessDeniedException.class,
                () -> service.requirePermission(request)
        );

        verifyNoInteractions(repository);
    }
}
