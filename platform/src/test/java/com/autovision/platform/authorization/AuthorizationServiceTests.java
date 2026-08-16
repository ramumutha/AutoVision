package com.autovision.platform.authorization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationServiceTests {

    private final AuthorizationRepository repository =
            mock(AuthorizationRepository.class);

    private final AuthorizationScopeEvaluator scopeEvaluator =
            mock(AuthorizationScopeEvaluator.class);

    private final AuthorizationService service =
            new AuthorizationService(repository, scopeEvaluator);

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
    void resourceAwareHasPermissionDeniesWhenNoGrants() {
        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                UUID.randomUUID()
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of());

        assertFalse(service.hasPermission(request));
    }

    @Test
    void resourceAwareHasPermissionDeniesWhenNoGrantContainsResource() {
        UUID resourceId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                resourceId
        );

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                UUID.randomUUID()
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of(grant));

        when(scopeEvaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.DEALER,
                resourceId
        )).thenReturn(false);

        assertFalse(service.hasPermission(request));
    }

    @Test
    void resourceAwareHasPermissionAllowsWhenGrantContainsResource() {
        UUID resourceId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                resourceId
        );

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                resourceId
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of(grant));

        when(scopeEvaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.DEALER,
                resourceId
        )).thenReturn(true);

        assertTrue(service.hasPermission(request));
    }

    @Test
    void resourceAwareHasPermissionAllowsWhenAnyOfMultipleGrantsContainsResource() {
        UUID resourceId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                resourceId
        );

        AuthorizationGrant nonContainingGrant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                UUID.randomUUID()
        );

        AuthorizationGrant containingGrant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                resourceId
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of(nonContainingGrant, containingGrant));

        when(scopeEvaluator.contains(
                nonContainingGrant,
                tenantId,
                AuthorizationResourceType.DEALER,
                resourceId
        )).thenReturn(false);

        when(scopeEvaluator.contains(
                containingGrant,
                tenantId,
                AuthorizationResourceType.DEALER,
                resourceId
        )).thenReturn(true);

        assertTrue(service.hasPermission(request));
    }

    @Test
    void resourceAwareRequirePermissionSucceedsWhenGranted() {
        UUID resourceId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                resourceId
        );

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                resourceId
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of(grant));

        when(scopeEvaluator.contains(any(), any(), any(), any()))
                .thenReturn(true);

        service.requirePermission(request);
    }

    @Test
    void resourceAwareRequirePermissionThrowsAccessDeniedWhenDenied() {
        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.BRANCH.READ",
                AuthorizationResourceType.BRANCH,
                UUID.randomUUID()
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.BRANCH.READ"
        )).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.requirePermission(request)
        );
    }

    @Test
    void resourceAwareHasPermissionAllowsTenantGroupGrantWhenContainmentMatches() {
        UUID tenantGroupId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        AuthorizationRequest request = new AuthorizationRequest(
                context,
                "ORGANIZATION.DEALER.READ",
                AuthorizationResourceType.DEALER,
                dealerId
        );

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(repository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                "ORGANIZATION.DEALER.READ"
        )).thenReturn(List.of(grant));

        when(scopeEvaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.DEALER,
                dealerId
        )).thenReturn(true);

        assertTrue(service.hasPermission(request));

        verify(scopeEvaluator).contains(
                grant,
                tenantId,
                AuthorizationResourceType.DEALER,
                dealerId
        );
    }

}
