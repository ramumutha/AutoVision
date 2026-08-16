package com.autovision.platform.authorization;

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
}
