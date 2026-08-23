package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessmentRepository;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.data.R1DealerDataReadinessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url="
                + "jdbc:h2:mem:service_profit_demo_bootstrap;"
                + "MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS PLATFORM"
})
@ActiveProfiles({"test", "service-profit-demo"})
class ServiceProfitDemoBootstrapTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private static final UUID MANAGER_USER_REF_ID =
            UUID.fromString(
                    "00000000-0000-4000-8000-000000000001"
            );

    private static final UUID OTHER_TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000099"
            );

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse(
                    "2026-08-22T10:00:00+05:30"
            );

    @Autowired
    private ServiceProfitDemoBootstrap bootstrap;

    @Autowired
    private ServiceProfitDemoTenantRepository tenantRepository;

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void prepareFoundation() {
        createFoundationTables();
        clearBootstrapRecords();

        jdbcClient.sql("""
                INSERT INTO platform.permissions
                    (id, code, resource, action, description,
                     system_defined, is_active, created_at, updated_at)
                VALUES
                    ('8c221e8a-b08e-4dc1-976f-5f2f72f8ca02',
                     'SERVICE_PROFIT_OPPORTUNITY.READ',
                     'SERVICE_PROFIT_OPPORTUNITY', 'READ',
                     'Read Service Profit opportunities.',
                     TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).update();
    }

    @Test
    void createsCanonicalOrganizationAndLeastPrivilegeManagerIdempotently() {
        ServiceProfitDemoBootstrapResult first =
                bootstrap.bootstrap(
                        datasetRoot(),
                        MANAGER_USER_REF_ID,
                        "service-profit-demo-manager",
                        NOW
                );

        assertEquals(TENANT_ID, first.tenantId());
        assertEquals(MANAGER_USER_REF_ID, first.managerUserRefId());
        assertEquals(1, first.dealerGroups());
        assertEquals(2, first.dealers());
        assertEquals(3, first.branches());
        assertEquals(4, first.locations());
        assertTrue(tenantRepository.existsById(TENANT_ID));

        assertCount("public.tenants", 1);
        assertCount("public.user_refs", 1);
        assertCount("platform.dealer_groups", 1);
        assertCount("platform.dealers", 2);
        assertCount("platform.branches", 3);
        assertCount("platform.locations", 4);
        assertCount("platform.dealer_group_memberships", 2);
        assertCount("platform.authorization_principals", 1);
        assertCount("platform.roles", 1);
        assertCount("platform.role_permissions", 1);
        assertCount("platform.role_permission_sets", 0);
        assertCount("platform.scoped_role_assignments", 1);

        assertEquals(
                UUID.fromString(
                        "12000000-0000-4000-8000-000000000001"
                ),
                branchDealer(
                        "13000000-0000-4000-8000-000000000002"
                )
        );

        assertEquals(
                UUID.fromString(
                        "14000000-0000-4000-8000-000000000003"
                ),
                branchLocation(
                        "13000000-0000-4000-8000-000000000003"
                )
        );

        assertTrue(
                authorizationRepository.hasActiveTenantPermission(
                        MANAGER_USER_REF_ID,
                        TENANT_ID,
                        ServiceProfitPermissions.OPPORTUNITY_READ
                )
        );

        assertFalse(
                authorizationRepository.hasActiveTenantPermission(
                        MANAGER_USER_REF_ID,
                        OTHER_TENANT_ID,
                        ServiceProfitPermissions.OPPORTUNITY_READ
                )
        );

        assertFalse(
                authorizationRepository.hasActiveTenantPermission(
                        MANAGER_USER_REF_ID,
                        TENANT_ID,
                        "SERVICE_PROFIT_OPPORTUNITY.CREATE"
                )
        );

        ServiceProfitDemoBootstrapResult second =
                bootstrap.bootstrap(
                        datasetRoot(),
                        MANAGER_USER_REF_ID,
                        "service-profit-demo-manager",
                        NOW.plusMinutes(1)
                );

        assertEquals(first.tenantId(), second.tenantId());
        assertCount("public.tenants", 1);
        assertCount("public.user_refs", 1);
        assertCount("platform.dealer_groups", 1);
        assertCount("platform.dealers", 2);
        assertCount("platform.branches", 3);
        assertCount("platform.locations", 4);
        assertCount("platform.dealer_group_memberships", 2);
        assertCount("platform.authorization_principals", 1);
        assertCount("platform.roles", 1);
        assertCount("platform.role_permissions", 1);
        assertCount("platform.scoped_role_assignments", 1);
    }

    @Test
    void demoLoaderRunsAfterBootstrapCreatesTenant() {
        bootstrap.bootstrap(
                datasetRoot(),
                MANAGER_USER_REF_ID,
                "service-profit-demo-manager",
                NOW
        );

        DealerDataAssessmentRepository assessmentRepository =
                mock(DealerDataAssessmentRepository.class);

        DealerDataCapabilityAssessmentRepository capabilityRepository =
                mock(DealerDataCapabilityAssessmentRepository.class);

        when(
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(Optional.empty());

        when(assessmentRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(capabilityRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitDemoLoader loader =
                new ServiceProfitDemoLoader(
                        new ObjectMapper(),
                        tenantRepository,
                        assessmentRepository,
                        capabilityRepository,
                        new R1DealerDataReadinessPolicy(),
                        new R1DealerDataCapabilityPolicy()
                );

        ServiceProfitDemoLoadResult result =
                loader.load(
                        datasetRoot(),
                        MANAGER_USER_REF_ID,
                        NOW
                );

        assertTrue(result.loaded());
    }

        @Test
        void rejectsReservedIdCollisionBeforeCreatingTenant() {
                jdbcClient.sql("""
                                INSERT INTO platform.locations
                                        (id, tenant_id, code, name, status, created_at, updated_at)
                                VALUES
                                        ('14000000-0000-4000-8000-000000000001',
                                         :tenantId, 'OTHER', 'Other Location', 'ACTIVE',
                                         :now, :now)
                                """)
                                .param("tenantId", OTHER_TENANT_ID)
                                .param("now", NOW)
                                .update();

                assertThrows(
                                IllegalStateException.class,
                                () -> bootstrap.bootstrap(
                                                datasetRoot(),
                                                MANAGER_USER_REF_ID,
                                                "service-profit-demo-manager",
                                                NOW
                                )
                );

                assertCount("public.tenants", 0);
                assertCount("public.user_refs", 0);
                assertCount("platform.locations", 1);
                assertCount("platform.roles", 0);
        }

    private void createFoundationTables() {
        execute("""
                CREATE TABLE IF NOT EXISTS public.tenants (
                    id UUID PRIMARY KEY,
                    slug VARCHAR(80) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS public.user_refs (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    external_user_id VARCHAR(255),
                    display_name VARCHAR(255),
                    email VARCHAR(255),
                    is_active BOOLEAN NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE (tenant_id, external_user_id)
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.dealer_groups (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    code VARCHAR(80) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(1000),
                    status VARCHAR(32) NOT NULL,
                    created_by_principal_id UUID,
                    updated_by_principal_id UUID,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE (tenant_id, code)
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.dealer_group_memberships (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    dealer_group_id UUID NOT NULL,
                    dealer_id UUID NOT NULL,
                    membership_type VARCHAR(32) NOT NULL,
                    is_primary BOOLEAN NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE (dealer_group_id, dealer_id)
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.authorization_principals (
                    id UUID PRIMARY KEY,
                    principal_type VARCHAR(32) NOT NULL,
                    user_ref_id UUID,
                    tenant_id UUID,
                    issuer VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    display_name VARCHAR(255),
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    UNIQUE (issuer, subject)
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.permissions (
                    id UUID PRIMARY KEY,
                    code VARCHAR(160) NOT NULL UNIQUE,
                    resource VARCHAR(80) NOT NULL,
                    action VARCHAR(80) NOT NULL,
                    description VARCHAR(1000),
                    system_defined BOOLEAN NOT NULL,
                    is_active BOOLEAN NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.permission_sets (
                    id UUID PRIMARY KEY,
                    is_active BOOLEAN NOT NULL
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.permission_set_permissions (
                    permission_set_id UUID NOT NULL,
                    permission_id UUID NOT NULL
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.roles (
                    id UUID PRIMARY KEY,
                    tenant_group_id UUID,
                    tenant_id UUID,
                    code VARCHAR(160) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    description VARCHAR(1000),
                    system_defined BOOLEAN NOT NULL,
                    is_protected BOOLEAN NOT NULL,
                    is_active BOOLEAN NOT NULL,
                    created_by_principal_id UUID,
                    updated_by_principal_id UUID,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.role_permissions (
                    role_id UUID NOT NULL,
                    permission_id UUID NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    PRIMARY KEY (role_id, permission_id)
                )
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS platform.role_permission_sets (
                    role_id UUID NOT NULL,
                    permission_set_id UUID NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    PRIMARY KEY (role_id, permission_set_id)
                )
                """);

        execute("""
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
                    valid_until TIMESTAMP WITH TIME ZONE,
                    created_by_principal_id UUID,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
    }

    private void clearBootstrapRecords() {
        execute("DELETE FROM platform.scoped_role_assignments");
        execute("DELETE FROM platform.role_permission_sets");
        execute("DELETE FROM platform.role_permissions");
        execute("DELETE FROM platform.roles");
        execute("DELETE FROM platform.authorization_principals");
        execute("DELETE FROM platform.permissions");
        execute("DELETE FROM platform.dealer_group_memberships");
        execute("DELETE FROM platform.dealer_groups");
        execute("DELETE FROM platform.branches");
        execute("DELETE FROM platform.dealers");
        execute("DELETE FROM platform.locations");
        execute("DELETE FROM public.user_refs");
        execute("DELETE FROM public.tenants");
    }

    private void assertCount(
            String table,
            int expected
    ) {
        Integer actual = jdbcClient.sql("SELECT COUNT(*) FROM " + table)
                .query(Integer.class)
                .single();

        assertEquals(expected, actual);
    }

    private UUID branchDealer(
            String branchId
    ) {
        return jdbcClient.sql("""
                SELECT dealer_id
                  FROM platform.branches
                 WHERE id = :id
                """)
                .param("id", UUID.fromString(branchId))
                .query(UUID.class)
                .single();
    }

    private UUID branchLocation(
            String branchId
    ) {
        return jdbcClient.sql("""
                SELECT location_id
                  FROM platform.branches
                 WHERE id = :id
                """)
                .param("id", UUID.fromString(branchId))
                .query(UUID.class)
                .single();
    }

    private void execute(
            String sql
    ) {
        jdbcClient.sql(sql).update();
    }

    private Path datasetRoot() {
        return Path.of(
                "..",
                "demo-data",
                "service-profit",
                "r1"
        ).toAbsolutePath().normalize();
    }
}