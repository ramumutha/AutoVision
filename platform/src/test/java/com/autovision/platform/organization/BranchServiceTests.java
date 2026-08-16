package com.autovision.platform.organization;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationScopeRepository;
import com.autovision.platform.authorization.AuthorizationScopeType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BranchServiceTests {

    private final BranchRepository repository =
            mock(BranchRepository.class);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final AuthorizationRepository authorizationRepository =
            mock(AuthorizationRepository.class);

    private final AuthorizationScopeRepository authorizationScopeRepository =
            mock(AuthorizationScopeRepository.class);

    private final BranchService service =
            new BranchService(
                    repository,
                    authorizationService,
                    authorizationRepository,
                    authorizationScopeRepository
            );

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
    void tenantGrantListsAllTenantBranches() throws Exception {
        Branch branch = branch(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "B001",
                "Demo Branch"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(AuthorizationScopeType.TENANT, tenantId)
        ));

        when(repository.findAllByTenantId(tenantId))
                .thenReturn(List.of(branch));

        List<BranchResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("B001", result.getFirst().code());

        verify(repository)
                .findAllByTenantId(tenantId);
    }

    @Test
    void dealerGrantListsOnlyThatDealersBranches() throws Exception {
        UUID dealerId = UUID.randomUUID();

        Branch branch = branch(
                UUID.randomUUID(),
                tenantId,
                dealerId,
                "B002",
                "Dealer Branch"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(AuthorizationScopeType.DEALER, dealerId)
        ));

        when(repository.findAllByTenantIdAndDealerId(tenantId, dealerId))
                .thenReturn(List.of(branch));

        List<BranchResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(dealerId, result.getFirst().dealerId());

        verify(repository, never()).findAllByTenantId(tenantId);
    }

    @Test
    void dealerGroupGrantListsMemberDealerBranches() throws Exception {
        UUID dealerGroupId = UUID.randomUUID();
        UUID memberDealerId = UUID.randomUUID();

        Branch branch = branch(
                UUID.randomUUID(),
                tenantId,
                memberDealerId,
                "B003",
                "Group Branch"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER_GROUP,
                        dealerGroupId
                )
        ));

        when(authorizationScopeRepository.findDealerIdsInGroup(
                dealerGroupId,
                tenantId
        )).thenReturn(List.of(memberDealerId));

        when(repository.findAllByTenantIdAndDealerIdIn(
                tenantId,
                List.of(memberDealerId)
        )).thenReturn(List.of(branch));

        List<BranchResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(memberDealerId, result.getFirst().dealerId());
    }

    @Test
    void combinesAndDeduplicatesMultipleGrants() throws Exception {
        UUID branchId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        Branch branch = branch(
                branchId,
                tenantId,
                dealerId,
                "B004",
                "Shared Branch"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(AuthorizationScopeType.BRANCH, branchId),
                new AuthorizationGrant(AuthorizationScopeType.DEALER, dealerId)
        ));

        when(repository.findByIdWithinAuthorizedTenantBoundary(branchId, tenantId))
                .thenReturn(Optional.of(branch));

        when(repository.findAllByTenantIdAndDealerId(tenantId, dealerId))
                .thenReturn(List.of(branch));

        List<BranchResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(branchId, result.getFirst().id());
    }

    @Test
    void unsupportedNarrowerScopeReturnsEmptyCollection() {
        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.LOCATION,
                        UUID.randomUUID()
                )
        ));

        List<BranchResponse> result = service.findAll(context);

        assertTrue(result.isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void deniesFindAllWhenNoPermissionBearingGrants() {
        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.findAll(context)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void tenantGrantForAnotherTenantGrantsNoAccess() {
        UUID otherTenantId = UUID.randomUUID();

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.BRANCH_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        otherTenantId
                )
        ));

        List<BranchResponse> result = service.findAll(context);

        assertTrue(result.isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void findsBranchOnlyWithinAuthenticatedTenant()
            throws Exception {

        UUID branchId = UUID.randomUUID();

        Branch branch = branch(
                branchId,
                tenantId,
                UUID.randomUUID(),
                "B001",
                "Demo Branch"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                branchId,
                tenantId
        )).thenReturn(Optional.of(branch));

        BranchResponse result =
                service.findById(context, branchId);

        assertEquals(branchId, result.id());

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        branchId,
                        tenantId
                );

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.BRANCH_READ,
                                AuthorizationResourceType.BRANCH,
                                branchId
                        )
                );
    }

    @Test
    void findsBranchInAuthorizedTenantGroupMemberTenant()
            throws Exception {

        UUID otherTenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Branch branch = branch(
                branchId,
                otherTenantId,
                UUID.randomUUID(),
                "B-XTENANT",
                "Cross Tenant Branch"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                branchId,
                tenantId
        )).thenReturn(Optional.of(branch));

        BranchResponse result =
                service.findById(context, branchId);

        assertEquals(branchId, result.id());

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.BRANCH_READ,
                                AuthorizationResourceType.BRANCH,
                                branchId
                        )
                );

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        branchId,
                        tenantId
                );
    }

    @Test
    void returnsNotFoundForBranchOutsideTenant() {
        UUID branchId = UUID.randomUUID();

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                branchId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                branchId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void deniesFindByIdWhenPermissionMissing() {
        UUID branchId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.BRANCH_READ,
                                AuthorizationResourceType.BRANCH,
                                branchId
                        )
                );

        assertThrows(
                AccessDeniedException.class,
                () -> service.findById(context, branchId)
        );

        verifyNoInteractions(repository);
    }

    private Branch branch(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            String code,
            String name
    ) throws Exception {

        Branch branch = new Branch();

        set(branch, "id", id);
        set(branch, "tenantId", tenantId);
        set(branch, "dealerId", dealerId);
        set(branch, "code", code);
        set(branch, "name", name);
        set(branch, "status", OrganizationStatus.ACTIVE);
        set(branch, "createdAt", OffsetDateTime.now());
        set(branch, "updatedAt", OffsetDateTime.now());

        return branch;
    }

    private void set(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass()
                        .getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}
