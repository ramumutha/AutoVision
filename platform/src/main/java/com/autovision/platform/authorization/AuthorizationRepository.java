package com.autovision.platform.authorization;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorizationRepository {

    private final JdbcClient jdbcClient;

    public AuthorizationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Evaluates TENANT-scope authorization per Sprint 4 S4.7.5.2 semantics:
     * active principal for the tenant, an active TENANT-scope role assignment
     * within its validity window, an active role, and the permission granted
     * either directly or through an active permission set.
     */
    public boolean hasActiveTenantPermission(
            UUID userRefId,
            UUID tenantId,
            String permissionCode
    ) {
        Integer matchCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.authorization_principals p
                  JOIN platform.scoped_role_assignments sra
                    ON sra.principal_id = p.id
                  JOIN platform.roles r
                    ON r.id = sra.role_id
                 WHERE p.user_ref_id = :userRefId
                   AND p.tenant_id = :tenantId
                   AND p.status = 'ACTIVE'
                   AND sra.scope_type = 'TENANT'
                   AND sra.tenant_id = :tenantId
                   AND sra.is_active = TRUE
                   AND (sra.valid_from IS NULL OR sra.valid_from <= CURRENT_TIMESTAMP)
                   AND (sra.valid_until IS NULL OR sra.valid_until > CURRENT_TIMESTAMP)
                   AND r.is_active = TRUE
                   AND (
                        EXISTS (
                            SELECT 1
                              FROM platform.role_permissions rp
                              JOIN platform.permissions perm
                                ON perm.id = rp.permission_id
                             WHERE rp.role_id = r.id
                               AND perm.code = :permissionCode
                               AND perm.is_active = TRUE
                        )
                        OR EXISTS (
                            SELECT 1
                              FROM platform.role_permission_sets rps
                              JOIN platform.permission_sets ps
                                ON ps.id = rps.permission_set_id
                              JOIN platform.permission_set_permissions psp
                                ON psp.permission_set_id = ps.id
                              JOIN platform.permissions perm2
                                ON perm2.id = psp.permission_id
                             WHERE rps.role_id = r.id
                               AND ps.is_active = TRUE
                               AND perm2.code = :permissionCode
                               AND perm2.is_active = TRUE
                        )
                   )
                """)
                .param("userRefId", userRefId)
                .param("tenantId", tenantId)
                .param("permissionCode", permissionCode)
                .query(Integer.class)
                .single();

        return matchCount != null && matchCount > 0;
    }

    /**
     * Resolves active permission-bearing scoped assignments. Local scopes
     * remain tenant-bound. TENANT_GROUP grants are returned only when the
     * authenticated principal's tenant is a member of the active tenant group
     * and the assigned role is owned by that same tenant group. SYSTEM remains
     * unsupported.
     */
    public List<AuthorizationGrant> findActivePermissionGrants(
            UUID userRefId,
            UUID authenticatedTenantId,
            String permissionCode
    ) {
        return jdbcClient.sql("""
                SELECT DISTINCT
                       sra.scope_type,
                       sra.tenant_group_id,
                       sra.tenant_id,
                       sra.dealer_group_id,
                       sra.dealer_id,
                       sra.branch_id,
                       sra.location_id
                  FROM platform.authorization_principals p
                  JOIN platform.scoped_role_assignments sra
                    ON sra.principal_id = p.id
                  JOIN platform.roles r
                    ON r.id = sra.role_id
                 WHERE p.user_ref_id = :userRefId
                   AND p.tenant_id = :authenticatedTenantId
                   AND p.status = 'ACTIVE'
                   AND sra.scope_type IN ('TENANT_GROUP', 'TENANT', 'DEALER_GROUP', 'DEALER', 'BRANCH', 'LOCATION')
                   AND sra.is_active = TRUE
                   AND (sra.valid_from IS NULL OR sra.valid_from <= CURRENT_TIMESTAMP)
                   AND (sra.valid_until IS NULL OR sra.valid_until > CURRENT_TIMESTAMP)
                   AND r.is_active = TRUE
                   AND (
                        (
                            sra.scope_type = 'TENANT_GROUP'
                            AND r.tenant_group_id = sra.tenant_group_id
                            AND r.tenant_id IS NULL
                            AND EXISTS (
                                SELECT 1
                                  FROM platform.tenant_groups tg
                                  JOIN platform.tenant_group_memberships tgm
                                    ON tgm.tenant_group_id = tg.id
                                 WHERE tg.id = sra.tenant_group_id
                                   AND tg.status = 'ACTIVE'
                                   AND tgm.tenant_id = :authenticatedTenantId
                            )
                        )
                        OR sra.scope_type IN ('TENANT', 'DEALER_GROUP', 'DEALER', 'BRANCH', 'LOCATION')
                   )
                   AND (
                        EXISTS (
                            SELECT 1
                              FROM platform.role_permissions rp
                              JOIN platform.permissions perm
                                ON perm.id = rp.permission_id
                             WHERE rp.role_id = r.id
                               AND perm.code = :permissionCode
                               AND perm.is_active = TRUE
                        )
                        OR EXISTS (
                            SELECT 1
                              FROM platform.role_permission_sets rps
                              JOIN platform.permission_sets ps
                                ON ps.id = rps.permission_set_id
                              JOIN platform.permission_set_permissions psp
                                ON psp.permission_set_id = ps.id
                              JOIN platform.permissions perm2
                                ON perm2.id = psp.permission_id
                             WHERE rps.role_id = r.id
                               AND ps.is_active = TRUE
                               AND perm2.code = :permissionCode
                               AND perm2.is_active = TRUE
                        )
                   )
                """)
                .param("userRefId", userRefId)
                .param("authenticatedTenantId", authenticatedTenantId)
                .param("permissionCode", permissionCode)
                .query(this::mapGrant)
                .list();
    }

    private AuthorizationGrant mapGrant(ResultSet rs, int rowNum) throws SQLException {
        AuthorizationScopeType scopeType =
                AuthorizationScopeType.valueOf(rs.getString("scope_type"));

        UUID scopeId = switch (scopeType) {
            case TENANT_GROUP -> rs.getObject("tenant_group_id", UUID.class);
            case TENANT -> rs.getObject("tenant_id", UUID.class);
            case DEALER_GROUP -> rs.getObject("dealer_group_id", UUID.class);
            case DEALER -> rs.getObject("dealer_id", UUID.class);
            case BRANCH -> rs.getObject("branch_id", UUID.class);
            case LOCATION -> rs.getObject("location_id", UUID.class);
        };

        return new AuthorizationGrant(scopeType, scopeId);
    }
}
