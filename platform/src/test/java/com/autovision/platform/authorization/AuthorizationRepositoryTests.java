package com.autovision.platform.authorization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real TENANT-scope authorization SQL against an H2 database
 * using a test-scope schema mirroring V3__create_authorization_foundation.sql.
 * No production migration or fixture data is used.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthorizationRepositoryTests {

    private static final String PERMISSION_CODE = "ORGANIZATION.DEALER.READ";

    @Autowired
    private AuthorizationRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();
    private final UUID userRefId = UUID.randomUUID();

    @BeforeEach
    void createSchema() {
        exec("""
                CREATE TABLE IF NOT EXISTS platform.authorization_principals (
                    id UUID PRIMARY KEY,
                    user_ref_id UUID,
                    tenant_id UUID,
                    status VARCHAR(32) NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.tenant_groups (
                    id UUID PRIMARY KEY,
                    status VARCHAR(32) NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.tenant_group_memberships (
                    id UUID PRIMARY KEY,
                    tenant_group_id UUID NOT NULL,
                    tenant_id UUID NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.roles (
                    id UUID PRIMARY KEY,
                    tenant_group_id UUID,
                    tenant_id UUID,
                    is_active BOOLEAN NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.permissions (
                    id UUID PRIMARY KEY,
                    code VARCHAR(160) NOT NULL,
                    is_active BOOLEAN NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.permission_sets (
                    id UUID PRIMARY KEY,
                    is_active BOOLEAN NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.permission_set_permissions (
                    permission_set_id UUID NOT NULL,
                    permission_id UUID NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.role_permissions (
                    role_id UUID NOT NULL,
                    permission_id UUID NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.role_permission_sets (
                    role_id UUID NOT NULL,
                    permission_set_id UUID NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.scoped_role_assignments (
                    id UUID PRIMARY KEY,
                    principal_id UUID NOT NULL,
                    role_id UUID NOT NULL,
                    scope_type VARCHAR(32) NOT NULL,
                    tenant_group_id UUID,
                    tenant_id UUID,
                    dealer_group_id UUID,
                    dealer_id UUID,
                    branch_id UUID,
                    location_id UUID,
                    is_active BOOLEAN NOT NULL,
                    valid_from TIMESTAMP WITH TIME ZONE,
                    valid_until TIMESTAMP WITH TIME ZONE
                )
                """);
    }

    @AfterEach
    void clearTables() {
        exec("DELETE FROM platform.scoped_role_assignments");
        exec("DELETE FROM platform.tenant_group_memberships");
        exec("DELETE FROM platform.tenant_groups");
        exec("DELETE FROM platform.role_permission_sets");
        exec("DELETE FROM platform.role_permissions");
        exec("DELETE FROM platform.permission_set_permissions");
        exec("DELETE FROM platform.permission_sets");
        exec("DELETE FROM platform.permissions");
        exec("DELETE FROM platform.roles");
        exec("DELETE FROM platform.authorization_principals");
    }

    @Test
    void allowsDirectRolePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertTrue(evaluate());
    }

    @Test
    void allowsPermissionThroughActivePermissionSet() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID permissionSetId = insertPermissionSet(true);
        insertPermissionSetPermission(permissionSetId, permissionId);
        insertRolePermissionSet(roleId, permissionSetId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertTrue(evaluate());
    }

    @Test
    void deniesWhenPermissionMissing() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenRoleAssignmentMissing() {
        insertPrincipal(tenantId, "ACTIVE");

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenAssignmentInactive() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                false,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenValidFromInFuture() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                OffsetDateTime.now().plusDays(1),
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenValidUntilInPast() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                OffsetDateTime.now().minusDays(1)
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenRoleInactive() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(false);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenPermissionInactive() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, false);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesWhenPermissionOnlyAvailableThroughInactivePermissionSet() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID permissionSetId = insertPermissionSet(false);
        insertPermissionSetPermission(permissionSetId, permissionId);
        insertRolePermissionSet(roleId, permissionSetId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesAssignmentForAnotherTenant() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                otherTenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    @Test
    void deniesPrincipalRegisteredForAnotherTenant() {
        UUID principalId = insertPrincipal(otherTenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertAssignment(
                principalId,
                roleId,
                tenantId,
                true,
                null,
                null
        );

        assertFalse(evaluate());
    }

    private boolean evaluate() {
        return repository.hasActiveTenantPermission(
                userRefId,
                tenantId,
                PERMISSION_CODE
        );
    }

    private List<AuthorizationGrant> evaluateGrants() {
        return evaluateGrants(PERMISSION_CODE);
    }

    private List<AuthorizationGrant> evaluateGrants(String permissionCode) {
        return repository.findActivePermissionGrants(
                userRefId,
                tenantId,
                permissionCode
        );
    }

    @Test
    void grantResolutionReturnsTenantGrantForDirectRolePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsDealerGrantForDirectRolePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID dealerId = UUID.randomUUID();
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.DEALER,
                dealerId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.DEALER,
                        dealerId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsBranchGrantForActivePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID branchId = UUID.randomUUID();
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.BRANCH,
                branchId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.BRANCH,
                        branchId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsLocationGrantForActivePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID locationId = UUID.randomUUID();
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.LOCATION,
                locationId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.LOCATION,
                        locationId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsDealerGroupGrantForActivePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID dealerGroupId = UUID.randomUUID();
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.DEALER_GROUP,
                        dealerGroupId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsGrantForPermissionThroughActivePermissionSet() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID permissionSetId = insertPermissionSet(true);
        insertPermissionSetPermission(permissionSetId, permissionId);
        insertRolePermissionSet(roleId, permissionSetId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsNoGrantForInactiveAssignment() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                false,
                null,
                null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoGrantForFutureValidFrom() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                OffsetDateTime.now().plusDays(1),
                null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoGrantForExpiredValidUntil() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                OffsetDateTime.now().minusDays(1)
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoGrantForInactiveRole() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(false);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoGrantForInactivePermission() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, false);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoGrantForInactivePermissionSet() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        UUID permissionSetId = insertPermissionSet(false);
        insertPermissionSetPermission(permissionSetId, permissionId);
        insertRolePermissionSet(roleId, permissionSetId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionReturnsNoLocalGrantsForAnotherTenantsPrincipal() {
        insertPrincipal(otherTenantId, "ACTIVE");

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionDoesNotLeakAnotherPrincipalsAssignment() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        UUID otherUserRefId = UUID.randomUUID();
        UUID otherPrincipalId = insertPrincipal(
                otherUserRefId,
                tenantId,
                "ACTIVE"
        );
        UUID otherRoleId = insertRole(true);
        insertRolePermission(otherRoleId, permissionId);
        UUID otherDealerId = UUID.randomUUID();
        insertScopedAssignment(
                otherPrincipalId,
                otherRoleId,
                AuthorizationScopeType.DEALER,
                otherDealerId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionReturnsTenantGroupGrantForMemberTenantAndGroupOwnedRole() {
        UUID tenantGroupId = insertTenantGroup("ACTIVE");
        insertTenantGroupMembership(tenantGroupId, tenantId);

        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertTenantGroupRole(tenantGroupId, true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId,
                roleId,
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.TENANT_GROUP,
                        tenantGroupId
                )),
                evaluateGrants()
        );
    }

    @Test
    void grantResolutionDeniesTenantGroupGrantWhenPrincipalTenantIsNotMember() {
        UUID tenantGroupId = insertTenantGroup("ACTIVE");

        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertTenantGroupRole(tenantGroupId, true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId, roleId, AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId, true, null, null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionDeniesTenantGroupGrantWhenGroupInactive() {
        UUID tenantGroupId = insertTenantGroup("INACTIVE");
        insertTenantGroupMembership(tenantGroupId, tenantId);

        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertTenantGroupRole(tenantGroupId, true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId, roleId, AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId, true, null, null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionDeniesTenantGroupGrantForTenantOwnedRole() {
        UUID tenantGroupId = insertTenantGroup("ACTIVE");
        insertTenantGroupMembership(tenantGroupId, tenantId);

        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID roleId = insertRole(true);
        UUID permissionId = insertPermission(PERMISSION_CODE, true);
        insertRolePermission(roleId, permissionId);
        insertScopedAssignment(
                principalId, roleId, AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId, true, null, null
        );

        assertEquals(List.of(), evaluateGrants());
    }

    @Test
    void grantResolutionDeduplicatesGrantsFromDifferentPermissionPaths() {
        UUID principalId = insertPrincipal(tenantId, "ACTIVE");
        UUID permissionId = insertPermission(PERMISSION_CODE, true);

        UUID directRoleId = insertRole(true);
        insertRolePermission(directRoleId, permissionId);
        insertScopedAssignment(
                principalId,
                directRoleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        UUID setRoleId = insertRole(true);
        UUID permissionSetId = insertPermissionSet(true);
        insertPermissionSetPermission(permissionSetId, permissionId);
        insertRolePermissionSet(setRoleId, permissionSetId);
        insertScopedAssignment(
                principalId,
                setRoleId,
                AuthorizationScopeType.TENANT,
                tenantId,
                true,
                null,
                null
        );

        assertEquals(
                List.of(new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                )),
                evaluateGrants()
        );
    }

    private UUID insertPrincipal(UUID principalTenantId, String status) {
        return insertPrincipal(userRefId, principalTenantId, status);
    }

    private UUID insertPrincipal(
            UUID principalUserRefId,
            UUID principalTenantId,
            String status
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.authorization_principals
                    (id, user_ref_id, tenant_id, status)
                VALUES (:id, :userRefId, :tenantId, :status)
                """)
                .param("id", id)
                .param("userRefId", principalUserRefId)
                .param("tenantId", principalTenantId)
                .param("status", status)
                .update();

        return id;
    }

    private UUID insertRole(boolean isActive) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.roles (id, tenant_id, is_active)
                VALUES (:id, :tenantId, :isActive)
                """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("isActive", isActive)
                .update();

        return id;
    }

    private UUID insertTenantGroup(String status) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.tenant_groups (id, status)
                VALUES (:id, :status)
                """)
                .param("id", id)
                .param("status", status)
                .update();

        return id;
    }

    private void insertTenantGroupMembership(
            UUID tenantGroupId,
            UUID memberTenantId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.tenant_group_memberships
                    (id, tenant_group_id, tenant_id)
                VALUES (:id, :tenantGroupId, :tenantId)
                """)
                .param("id", UUID.randomUUID())
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", memberTenantId)
                .update();
    }

    private UUID insertTenantGroupRole(
            UUID tenantGroupId,
            boolean isActive
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.roles
                    (id, tenant_group_id, tenant_id, is_active)
                VALUES (:id, :tenantGroupId, NULL, :isActive)
                """)
                .param("id", id)
                .param("tenantGroupId", tenantGroupId)
                .param("isActive", isActive)
                .update();

        return id;
    }

    private UUID insertPermission(String code, boolean isActive) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.permissions (id, code, is_active)
                VALUES (:id, :code, :isActive)
                """)
                .param("id", id)
                .param("code", code)
                .param("isActive", isActive)
                .update();

        return id;
    }

    private UUID insertPermissionSet(boolean isActive) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.permission_sets (id, is_active)
                VALUES (:id, :isActive)
                """)
                .param("id", id)
                .param("isActive", isActive)
                .update();

        return id;
    }

    private void insertPermissionSetPermission(
            UUID permissionSetId,
            UUID permissionId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.permission_set_permissions
                    (permission_set_id, permission_id)
                VALUES (:permissionSetId, :permissionId)
                """)
                .param("permissionSetId", permissionSetId)
                .param("permissionId", permissionId)
                .update();
    }

    private void insertRolePermission(UUID roleId, UUID permissionId) {
        jdbcClient.sql("""
                INSERT INTO platform.role_permissions
                    (role_id, permission_id)
                VALUES (:roleId, :permissionId)
                """)
                .param("roleId", roleId)
                .param("permissionId", permissionId)
                .update();
    }

    private void insertRolePermissionSet(
            UUID roleId,
            UUID permissionSetId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.role_permission_sets
                    (role_id, permission_set_id)
                VALUES (:roleId, :permissionSetId)
                """)
                .param("roleId", roleId)
                .param("permissionSetId", permissionSetId)
                .update();
    }

    private void insertAssignment(
            UUID principalId,
            UUID roleId,
            UUID scopeTenantId,
            boolean isActive,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.scoped_role_assignments
                    (id, principal_id, role_id, scope_type, tenant_id,
                     is_active, valid_from, valid_until)
                VALUES (:id, :principalId, :roleId, 'TENANT', :tenantId,
                        :isActive, :validFrom, :validUntil)
                """)
                .param("id", UUID.randomUUID())
                .param("principalId", principalId)
                .param("roleId", roleId)
                .param("tenantId", scopeTenantId)
                .param("isActive", isActive)
                .param("validFrom", validFrom)
                .param("validUntil", validUntil)
                .update();
    }

    private UUID insertScopedAssignment(
            UUID principalId,
            UUID roleId,
            AuthorizationScopeType scopeType,
            UUID scopeId,
            boolean isActive,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil
    ) {
        UUID id = UUID.randomUUID();

        UUID tenantGroupId =
                scopeType == AuthorizationScopeType.TENANT_GROUP ? scopeId : null;
        UUID assignmentTenantId =
                scopeType == AuthorizationScopeType.TENANT ? scopeId : null;
        UUID dealerGroupId =
                scopeType == AuthorizationScopeType.DEALER_GROUP ? scopeId : null;
        UUID dealerId =
                scopeType == AuthorizationScopeType.DEALER ? scopeId : null;
        UUID branchId =
                scopeType == AuthorizationScopeType.BRANCH ? scopeId : null;
        UUID locationId =
                scopeType == AuthorizationScopeType.LOCATION ? scopeId : null;

        jdbcClient.sql("""
                INSERT INTO platform.scoped_role_assignments
                    (id, principal_id, role_id, scope_type, tenant_group_id,
                     tenant_id, dealer_group_id, dealer_id, branch_id, location_id,
                     is_active, valid_from, valid_until)
                VALUES (:id, :principalId, :roleId, :scopeType, :tenantGroupId,
                        :tenantId, :dealerGroupId, :dealerId, :branchId, :locationId,
                        :isActive, :validFrom, :validUntil)
                """)
                .param("id", id)
                .param("principalId", principalId)
                .param("roleId", roleId)
                .param("scopeType", scopeType.name())
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", assignmentTenantId)
                .param("dealerGroupId", dealerGroupId)
                .param("dealerId", dealerId)
                .param("branchId", branchId)
                .param("locationId", locationId)
                .param("isActive", isActive)
                .param("validFrom", validFrom)
                .param("validUntil", validUntil)
                .update();

        return id;
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }
}
