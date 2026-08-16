package com.autovision.platform.organization;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BranchServiceTests {

    private final BranchRepository repository =
            mock(BranchRepository.class);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final BranchService service =
            new BranchService(repository, authorizationService);

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
    void listsOnlyAuthenticatedTenantBranches() throws Exception {
        Branch branch = branch(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "B001",
                "Demo Branch"
        );

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

        when(repository.findByIdAndTenantId(
                branchId,
                tenantId
        )).thenReturn(Optional.of(branch));

        BranchResponse result =
                service.findById(context, branchId);

        assertEquals(branchId, result.id());

        verify(repository)
                .findByIdAndTenantId(
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
    void returnsNotFoundForBranchOutsideTenant() {
        UUID branchId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(
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
    void deniesFindAllWhenPermissionMissing() {
        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(
                        context,
                        OrganizationPermissions.BRANCH_READ
                );

        assertThrows(
                AccessDeniedException.class,
                () -> service.findAll(context)
        );

        verifyNoInteractions(repository);
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