package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("service-profit-demo")
public class ServiceProfitDemoBootstrap {

    static final UUID MANAGER_PRINCIPAL_ID =
            UUID.fromString(
                    "15000000-0000-4000-8000-000000000001"
            );

    static final UUID MANAGER_ROLE_ID =
            UUID.fromString(
                    "16000000-0000-4000-8000-000000000001"
            );

    static final UUID MANAGER_ASSIGNMENT_ID =
            UUID.fromString(
                    "17000000-0000-4000-8000-000000000001"
            );

    private static final String MANAGER_ROLE_CODE =
            "SERVICE_PROFIT_DEMO_MANAGER";

    private final JdbcClient jdbcClient;

    public ServiceProfitDemoBootstrap(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional
    public ServiceProfitDemoBootstrapResult bootstrap(
            Path datasetRoot,
            UUID managerUserRefId,
            String managerExternalUserId,
            OffsetDateTime now
    ) {
        requirePath(datasetRoot);
        requireId(managerUserRefId, "Demo manager user reference ID is required");
        requireText(managerExternalUserId, "Demo manager external user ID is required");
        requireTime(now);

        List<OrganizationRow> rows =
                readOrganizationRows(datasetRoot);

        OrganizationRow tenant =
                requireSingle(rows, "TENANT");

        UUID tenantId = tenant.tenantId();

        Map<UUID, OrganizationRow> locations =
                collectLocations(rows);

        verifyReservedState(
                tenant,
                rows,
                locations,
                managerUserRefId,
                managerExternalUserId
        );

        ensureTenant(tenant, now);

        for (Map.Entry<UUID, OrganizationRow> entry : locations.entrySet()) {
            ensureLocation(
                    tenantId,
                    entry.getKey(),
                    entry.getValue(),
                    now
            );
        }

        List<OrganizationRow> dealers =
                rowsOfType(rows, "DEALER");

        for (OrganizationRow dealer : dealers) {
            UUID primaryLocationId = rows.stream()
                    .filter(row -> "BRANCH".equals(row.entityType()))
                    .filter(row -> dealer.dealerId().equals(row.dealerId()))
                    .map(OrganizationRow::locationId)
                    .findFirst()
                    .orElse(null);

            ensureDealer(
                    tenantId,
                    dealer,
                    primaryLocationId,
                    now
            );
        }

        List<OrganizationRow> dealerGroups =
                rowsOfType(rows, "DEALER_GROUP");

        for (OrganizationRow dealerGroup : dealerGroups) {
            ensureDealerGroup(tenantId, dealerGroup, now);
        }

        for (OrganizationRow dealer : dealers) {
            if (dealer.dealerGroupId() != null) {
                ensureDealerGroupMembership(
                        tenantId,
                        dealer.dealerGroupId(),
                        dealer.dealerId(),
                        now
                );
            }
        }

        List<OrganizationRow> branches =
                rowsOfType(rows, "BRANCH");

        for (OrganizationRow branch : branches) {
            ensureBranch(tenantId, branch, now);
        }

        ensureManagerIdentity(
                tenantId,
                managerUserRefId,
                managerExternalUserId,
                now
        );

        ensureManagerAuthorization(
                tenantId,
                managerUserRefId,
                managerExternalUserId,
                now
        );

        return new ServiceProfitDemoBootstrapResult(
                tenantId,
                managerUserRefId,
                dealerGroups.size(),
                dealers.size(),
                branches.size(),
                locations.size()
        );
    }

    private void verifyReservedState(
            OrganizationRow tenant,
            List<OrganizationRow> rows,
            Map<UUID, OrganizationRow> locations,
            UUID managerUserRefId,
            String managerExternalUserId
    ) {
        UUID tenantId = tenant.tenantId();
        String tenantSlug = tenant.code().toLowerCase();

        assertCompatibleTenant(tenantId, tenantSlug);

        for (Map.Entry<UUID, OrganizationRow> entry : locations.entrySet()) {
            assertCompatibleTenantEntity(
                    "platform.locations",
                    entry.getKey(),
                    tenantId,
                    entry.getValue().code()
            );
        }

        for (OrganizationRow dealer : rowsOfType(rows, "DEALER")) {
            assertCompatibleTenantEntity(
                    "platform.dealers",
                    dealer.dealerId(),
                    tenantId,
                    dealer.code()
            );
        }

        for (OrganizationRow dealerGroup : rowsOfType(rows, "DEALER_GROUP")) {
            assertCompatibleTenantEntity(
                    "platform.dealer_groups",
                    dealerGroup.dealerGroupId(),
                    tenantId,
                    dealerGroup.code()
            );
        }

        for (OrganizationRow branch : rowsOfType(rows, "BRANCH")) {
            assertCompatibleBranch(tenantId, branch);
        }

        assertCompatibleManagerIdentity(
                tenantId,
                managerUserRefId,
                managerExternalUserId
        );

        assertCompatibleAuthorization(
                tenantId,
                managerUserRefId,
                managerExternalUserId
        );
    }

    private void assertCompatibleTenant(
            UUID tenantId,
            String slug
    ) {
        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM public.tenants
                 WHERE id = :id
                   AND slug <> :slug
                """,
                Map.of("id", tenantId, "slug", slug),
                "Demo tenant ID is already used by another tenant"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM public.tenants
                 WHERE slug = :slug
                   AND id <> :id
                """,
                Map.of("id", tenantId, "slug", slug),
                "Demo tenant slug is already used by another tenant"
        );
    }

    private void assertCompatibleTenantEntity(
            String table,
            UUID id,
            UUID tenantId,
            String code
    ) {
        requireId(id, "Demo organization ID is required");

        assertNoCollision(
                "SELECT COUNT(*) FROM " + table + " "
                        + "WHERE id = :id "
                        + "AND (tenant_id <> :tenantId OR code <> :code)",
                Map.of(
                        "id", id,
                        "tenantId", tenantId,
                        "code", code
                ),
                "Reserved demo organization ID is already in use"
        );

        assertNoCollision(
                "SELECT COUNT(*) FROM " + table + " "
                        + "WHERE tenant_id = :tenantId "
                        + "AND code = :code AND id <> :id",
                Map.of(
                        "id", id,
                        "tenantId", tenantId,
                        "code", code
                ),
                "Demo organization code is already in use"
        );
    }

    private void assertCompatibleBranch(
            UUID tenantId,
            OrganizationRow branch
    ) {
        requireId(branch.branchId(), "Demo branch ID is required");
        requireId(branch.dealerId(), "Demo branch dealer ID is required");
        requireId(branch.locationId(), "Demo branch location ID is required");

        Map<String, Object> parameters = Map.of(
                "id", branch.branchId(),
                "tenantId", tenantId,
                "dealerId", branch.dealerId(),
                "code", branch.code()
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE id = :id
                   AND (tenant_id <> :tenantId
                        OR dealer_id <> :dealerId
                        OR code <> :code)
                """,
                parameters,
                "Reserved demo branch ID is already in use"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.branches
                 WHERE tenant_id = :tenantId
                   AND dealer_id = :dealerId
                   AND code = :code
                   AND id <> :id
                """,
                parameters,
                "Demo branch code is already in use"
        );
    }

    private void assertCompatibleManagerIdentity(
            UUID tenantId,
            UUID managerUserRefId,
            String managerExternalUserId
    ) {
        Map<String, Object> parameters = Map.of(
                "id", managerUserRefId,
                "tenantId", tenantId,
                "externalUserId", managerExternalUserId
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM public.user_refs
                 WHERE id = :id
                   AND (tenant_id <> :tenantId
                        OR external_user_id <> :externalUserId)
                """,
                parameters,
                "Demo manager user reference is already in use"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM public.user_refs
                 WHERE tenant_id = :tenantId
                   AND external_user_id = :externalUserId
                   AND id <> :id
                """,
                parameters,
                "Demo manager external user ID is already in use"
        );
    }

    private void assertCompatibleAuthorization(
            UUID tenantId,
            UUID managerUserRefId,
            String managerExternalUserId
    ) {
        Map<String, Object> principalParameters = Map.of(
                "id", MANAGER_PRINCIPAL_ID,
                "tenantId", tenantId,
                "userRefId", managerUserRefId,
                "subject", managerExternalUserId
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.authorization_principals
                 WHERE id = :id
                   AND (tenant_id <> :tenantId
                        OR user_ref_id <> :userRefId
                        OR issuer <> 'urn:autovision:service-profit-demo'
                        OR subject <> :subject)
                """,
                principalParameters,
                "Reserved demo authorization principal ID is already in use"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.authorization_principals
                 WHERE issuer = 'urn:autovision:service-profit-demo'
                   AND subject = :subject
                   AND id <> :id
                """,
                principalParameters,
                "Demo authorization principal subject is already in use"
        );

        Map<String, Object> roleParameters = Map.of(
                "id", MANAGER_ROLE_ID,
                "tenantId", tenantId,
                "code", MANAGER_ROLE_CODE
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.roles
                 WHERE id = :id
                   AND (tenant_id <> :tenantId OR code <> :code)
                """,
                roleParameters,
                "Reserved demo role ID is already in use"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.roles
                 WHERE tenant_id = :tenantId
                   AND code = :code
                   AND id <> :id
                """,
                roleParameters,
                "Demo manager role code is already in use"
        );

        assertNoCollision(
                """
                SELECT COUNT(*)
                  FROM platform.scoped_role_assignments
                 WHERE id = :id
                   AND (principal_id <> :principalId
                        OR role_id <> :roleId
                        OR tenant_id <> :tenantId)
                """,
                Map.of(
                        "id", MANAGER_ASSIGNMENT_ID,
                        "principalId", MANAGER_PRINCIPAL_ID,
                        "roleId", MANAGER_ROLE_ID,
                        "tenantId", tenantId
                ),
                "Reserved demo role assignment ID is already in use"
        );
    }

    private void assertNoCollision(
            String sql,
            Map<String, ?> parameters,
            String message
    ) {
        Integer collisions = jdbcClient.sql(sql)
                .params(parameters)
                .query(Integer.class)
                .single();

        if (collisions != null && collisions > 0) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureTenant(
            OrganizationRow tenant,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE public.tenants
                   SET slug = :slug,
                       name = :name,
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", tenant.tenantId())
                .param("slug", tenant.code().toLowerCase())
                .param("name", tenant.name())
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO public.tenants
                        (id, slug, name, created_at, updated_at)
                    VALUES
                        (:id, :slug, :name, :now, :now)
                    """)
                    .param("id", tenant.tenantId())
                    .param("slug", tenant.code().toLowerCase())
                    .param("name", tenant.name())
                    .param("now", now)
                    .update();
        }
    }

    private void ensureLocation(
            UUID tenantId,
            UUID locationId,
            OrganizationRow source,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.locations
                   SET tenant_id = :tenantId,
                       code = :code,
                       name = :name,
                       status = 'ACTIVE',
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", locationId)
                .param("tenantId", tenantId)
                .param("code", source.code())
                .param("name", source.name())
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.locations
                        (id, tenant_id, code, name, status, created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :code, :name, 'ACTIVE', :now, :now)
                    """)
                    .param("id", locationId)
                    .param("tenantId", tenantId)
                    .param("code", source.code())
                    .param("name", source.name())
                    .param("now", now)
                    .update();
        }
    }

    private void ensureDealer(
            UUID tenantId,
            OrganizationRow dealer,
            UUID primaryLocationId,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.dealers
                   SET tenant_id = :tenantId,
                       code = :code,
                       name = :name,
                       legal_name = :name,
                       primary_location_id = :primaryLocationId,
                       status = 'ACTIVE',
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", dealer.dealerId())
                .param("tenantId", tenantId)
                .param("code", dealer.code())
                .param("name", dealer.name())
                .param("primaryLocationId", primaryLocationId)
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.dealers
                        (id, tenant_id, code, name, legal_name,
                         primary_location_id, status, created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :code, :name, :name,
                         :primaryLocationId, 'ACTIVE', :now, :now)
                    """)
                    .param("id", dealer.dealerId())
                    .param("tenantId", tenantId)
                    .param("code", dealer.code())
                    .param("name", dealer.name())
                    .param("primaryLocationId", primaryLocationId)
                    .param("now", now)
                    .update();
        }
    }

    private void ensureDealerGroup(
            UUID tenantId,
            OrganizationRow dealerGroup,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.dealer_groups
                   SET tenant_id = :tenantId,
                       code = :code,
                       name = :name,
                       status = 'ACTIVE',
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", dealerGroup.dealerGroupId())
                .param("tenantId", tenantId)
                .param("code", dealerGroup.code())
                .param("name", dealerGroup.name())
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.dealer_groups
                        (id, tenant_id, code, name, status,
                         created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :code, :name, 'ACTIVE',
                         :now, :now)
                    """)
                    .param("id", dealerGroup.dealerGroupId())
                    .param("tenantId", tenantId)
                    .param("code", dealerGroup.code())
                    .param("name", dealerGroup.name())
                    .param("now", now)
                    .update();
        }
    }

    private void ensureDealerGroupMembership(
            UUID tenantId,
            UUID dealerGroupId,
            UUID dealerId,
            OffsetDateTime now
    ) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.dealer_group_memberships
                 WHERE dealer_group_id = :dealerGroupId
                   AND dealer_id = :dealerId
                """)
                .param("dealerGroupId", dealerGroupId)
                .param("dealerId", dealerId)
                .query(Integer.class)
                .single();

        if (count == null || count == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.dealer_group_memberships
                        (id, tenant_id, dealer_group_id, dealer_id,
                         membership_type, is_primary, created_at)
                    VALUES
                        (:id, :tenantId, :dealerGroupId, :dealerId,
                         'OWNERSHIP', TRUE, :now)
                    """)
                    .param(
                            "id",
                            deterministicId("dealer-membership", dealerId)
                    )
                    .param("tenantId", tenantId)
                    .param("dealerGroupId", dealerGroupId)
                    .param("dealerId", dealerId)
                    .param("now", now)
                    .update();
        }
    }

    private void ensureBranch(
            UUID tenantId,
            OrganizationRow branch,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.branches
                   SET tenant_id = :tenantId,
                       dealer_id = :dealerId,
                       location_id = :locationId,
                       code = :code,
                       name = :name,
                       status = 'ACTIVE',
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", branch.branchId())
                .param("tenantId", tenantId)
                .param("dealerId", branch.dealerId())
                .param("locationId", branch.locationId())
                .param("code", branch.code())
                .param("name", branch.name())
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.branches
                        (id, tenant_id, dealer_id, location_id, code,
                         name, status, created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :dealerId, :locationId, :code,
                         :name, 'ACTIVE', :now, :now)
                    """)
                    .param("id", branch.branchId())
                    .param("tenantId", tenantId)
                    .param("dealerId", branch.dealerId())
                    .param("locationId", branch.locationId())
                    .param("code", branch.code())
                    .param("name", branch.name())
                    .param("now", now)
                    .update();
        }
    }

    private void ensureManagerIdentity(
            UUID tenantId,
            UUID managerUserRefId,
            String managerExternalUserId,
            OffsetDateTime now
    ) {
        List<UUID> existingTenants = jdbcClient.sql("""
                SELECT tenant_id
                  FROM public.user_refs
                 WHERE id = :id
                """)
                .param("id", managerUserRefId)
                .query(UUID.class)
                .list();

        if (!existingTenants.isEmpty()
                && !tenantId.equals(existingTenants.getFirst())) {
            throw new IllegalStateException(
                    "Demo manager user reference belongs to another tenant"
            );
        }

        int updated = jdbcClient.sql("""
                UPDATE public.user_refs
                   SET external_user_id = :externalUserId,
                       display_name = 'Service Profit Demo Manager',
                       email = :email,
                       is_active = TRUE,
                       updated_at = :now
                 WHERE id = :id
                   AND tenant_id = :tenantId
                """)
                .param("id", managerUserRefId)
                .param("tenantId", tenantId)
                .param("externalUserId", managerExternalUserId)
                .param("email", managerExternalUserId + "@local.autovision")
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO public.user_refs
                        (id, tenant_id, external_user_id, display_name,
                         email, is_active, created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :externalUserId,
                         'Service Profit Demo Manager', :email,
                         TRUE, :now, :now)
                    """)
                    .param("id", managerUserRefId)
                    .param("tenantId", tenantId)
                    .param("externalUserId", managerExternalUserId)
                    .param("email", managerExternalUserId + "@local.autovision")
                    .param("now", now)
                    .update();
        }
    }

    private void ensureManagerAuthorization(
            UUID tenantId,
            UUID managerUserRefId,
            String managerExternalUserId,
            OffsetDateTime now
    ) {
        UUID permissionId = jdbcClient.sql("""
                SELECT id
                  FROM platform.permissions
                 WHERE code = :permissionCode
                   AND is_active = TRUE
                """)
                .param(
                        "permissionCode",
                        ServiceProfitPermissions.OPPORTUNITY_READ
                )
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException(
                        "Service Profit read permission is not registered"
                ));

        ensurePrincipal(
                tenantId,
                managerUserRefId,
                managerExternalUserId,
                now
        );

        ensureRole(tenantId, now);

        jdbcClient.sql("""
                DELETE FROM platform.role_permission_sets
                 WHERE role_id = :roleId
                """)
                .param("roleId", MANAGER_ROLE_ID)
                .update();

        jdbcClient.sql("""
                DELETE FROM platform.role_permissions
                 WHERE role_id = :roleId
                   AND permission_id <> :permissionId
                """)
                .param("roleId", MANAGER_ROLE_ID)
                .param("permissionId", permissionId)
                .update();

        Integer rolePermissionCount = jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.role_permissions
                 WHERE role_id = :roleId
                   AND permission_id = :permissionId
                """)
                .param("roleId", MANAGER_ROLE_ID)
                .param("permissionId", permissionId)
                .query(Integer.class)
                .single();

        if (rolePermissionCount == null || rolePermissionCount == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.role_permissions
                        (role_id, permission_id, created_at)
                    VALUES
                        (:roleId, :permissionId, :now)
                    """)
                    .param("roleId", MANAGER_ROLE_ID)
                    .param("permissionId", permissionId)
                    .param("now", now)
                    .update();
        }

        int updated = jdbcClient.sql("""
                UPDATE platform.scoped_role_assignments
                   SET principal_id = :principalId,
                       role_id = :roleId,
                       scope_type = 'TENANT',
                       tenant_group_id = NULL,
                       tenant_id = :tenantId,
                       dealer_group_id = NULL,
                       dealer_id = NULL,
                       branch_id = NULL,
                       location_id = NULL,
                       is_active = TRUE,
                       valid_from = NULL,
                       valid_until = NULL,
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", MANAGER_ASSIGNMENT_ID)
                .param("principalId", MANAGER_PRINCIPAL_ID)
                .param("roleId", MANAGER_ROLE_ID)
                .param("tenantId", tenantId)
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.scoped_role_assignments
                        (id, principal_id, role_id, scope_type, tenant_id,
                         is_active, created_at, updated_at)
                    VALUES
                        (:id, :principalId, :roleId, 'TENANT', :tenantId,
                         TRUE, :now, :now)
                    """)
                    .param("id", MANAGER_ASSIGNMENT_ID)
                    .param("principalId", MANAGER_PRINCIPAL_ID)
                    .param("roleId", MANAGER_ROLE_ID)
                    .param("tenantId", tenantId)
                    .param("now", now)
                    .update();
        }
    }

    private void ensurePrincipal(
            UUID tenantId,
            UUID managerUserRefId,
            String managerExternalUserId,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.authorization_principals
                   SET principal_type = 'HUMAN',
                       user_ref_id = :userRefId,
                       tenant_id = :tenantId,
                       issuer = 'urn:autovision:service-profit-demo',
                       subject = :subject,
                       display_name = 'Service Profit Demo Manager',
                       status = 'ACTIVE',
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", MANAGER_PRINCIPAL_ID)
                .param("userRefId", managerUserRefId)
                .param("tenantId", tenantId)
                .param("subject", managerExternalUserId)
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.authorization_principals
                        (id, principal_type, user_ref_id, tenant_id,
                         issuer, subject, display_name, status,
                         created_at, updated_at)
                    VALUES
                        (:id, 'HUMAN', :userRefId, :tenantId,
                         'urn:autovision:service-profit-demo', :subject,
                         'Service Profit Demo Manager', 'ACTIVE',
                         :now, :now)
                    """)
                    .param("id", MANAGER_PRINCIPAL_ID)
                    .param("userRefId", managerUserRefId)
                    .param("tenantId", tenantId)
                    .param("subject", managerExternalUserId)
                    .param("now", now)
                    .update();
        }
    }

    private void ensureRole(
            UUID tenantId,
            OffsetDateTime now
    ) {
        int updated = jdbcClient.sql("""
                UPDATE platform.roles
                   SET tenant_group_id = NULL,
                       tenant_id = :tenantId,
                       code = :code,
                       name = 'Service Profit Demo Manager',
                       description = 'Read-only Service Profit demo manager access.',
                       system_defined = FALSE,
                       is_protected = TRUE,
                       is_active = TRUE,
                       updated_at = :now
                 WHERE id = :id
                """)
                .param("id", MANAGER_ROLE_ID)
                .param("tenantId", tenantId)
                .param("code", MANAGER_ROLE_CODE)
                .param("now", now)
                .update();

        if (updated == 0) {
            jdbcClient.sql("""
                    INSERT INTO platform.roles
                        (id, tenant_id, code, name, description,
                         system_defined, is_protected, is_active,
                         created_at, updated_at)
                    VALUES
                        (:id, :tenantId, :code,
                         'Service Profit Demo Manager',
                         'Read-only Service Profit demo manager access.',
                         FALSE, TRUE, TRUE, :now, :now)
                    """)
                    .param("id", MANAGER_ROLE_ID)
                    .param("tenantId", tenantId)
                    .param("code", MANAGER_ROLE_CODE)
                    .param("now", now)
                    .update();
        }
    }

    private Map<UUID, OrganizationRow> collectLocations(
            List<OrganizationRow> rows
    ) {
        Map<UUID, OrganizationRow> locations =
                new LinkedHashMap<>();

        for (OrganizationRow row : rows) {
            if ("LOCATION".equals(row.entityType())) {
                locations.put(row.locationId(), row);
            } else if ("BRANCH".equals(row.entityType())) {
                locations.putIfAbsent(row.locationId(), row);
            }
        }

        return locations;
    }

    private List<OrganizationRow> readOrganizationRows(
            Path datasetRoot
    ) {
        Path path = datasetRoot.resolve("source").resolve("organization.csv");

        try {
            List<String> lines = Files.readAllLines(path);

            if (lines.isEmpty()) {
                throw new IllegalArgumentException(
                        "Service Profit demo organization dataset is empty"
                );
            }

            String[] headers = lines.getFirst().split(",", -1);
            List<OrganizationRow> rows = new ArrayList<>();

            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) {
                    continue;
                }

                String[] values = lines.get(index).split(",", -1);

                if (values.length != headers.length) {
                    throw new IllegalArgumentException(
                            "Invalid demo organization CSV column count at line "
                                    + (index + 1)
                    );
                }

                Map<String, String> row = new HashMap<>();

                for (int column = 0; column < headers.length; column++) {
                    row.put(
                            headers[column].trim(),
                            normalize(values[column])
                    );
                }

                rows.add(new OrganizationRow(
                        uuid(row.get("tenant_id")),
                        uuid(row.get("dealer_group_id")),
                        uuid(row.get("dealer_id")),
                        uuid(row.get("branch_id")),
                        uuid(row.get("location_id")),
                        row.get("entity_type"),
                        row.get("code"),
                        row.get("name")
                ));
            }

            return List.copyOf(rows);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to read Service Profit demo organization dataset",
                    exception
            );
        }
    }

    private OrganizationRow requireSingle(
            List<OrganizationRow> rows,
            String entityType
    ) {
        List<OrganizationRow> matches = rowsOfType(rows, entityType);

        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                    "Demo organization requires exactly one " + entityType
            );
        }

        return matches.getFirst();
    }

    private List<OrganizationRow> rowsOfType(
            List<OrganizationRow> rows,
            String entityType
    ) {
        return rows.stream()
                .filter(row -> entityType.equals(row.entityType()))
                .toList();
    }

    private UUID deterministicId(
            String purpose,
            UUID sourceId
    ) {
        return UUID.nameUUIDFromBytes(
                ("service-profit-demo:" + purpose + ":" + sourceId)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private static UUID uuid(
            String value
    ) {
        return value == null ? null : UUID.fromString(value);
    }

    private static String normalize(
            String value
    ) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static void requirePath(
            Path datasetRoot
    ) {
        if (datasetRoot == null) {
            throw new IllegalArgumentException(
                    "Demo dataset root is required"
            );
        }
    }

    private static void requireId(
            UUID id,
            String message
    ) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(
            OffsetDateTime now
    ) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "Demo bootstrap time is required"
            );
        }
    }

    private record OrganizationRow(
            UUID tenantId,
            UUID dealerGroupId,
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            String entityType,
            String code,
            String name
    ) {
        private OrganizationRow {
            requireId(tenantId, "Demo organization tenant ID is required");
            requireText(entityType, "Demo organization entity type is required");
            requireText(code, "Demo organization code is required");
            requireText(name, "Demo organization name is required");
        }
    }
}