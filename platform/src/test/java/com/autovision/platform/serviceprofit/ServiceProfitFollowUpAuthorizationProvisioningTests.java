package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import javax.sql.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceProfitFollowUpAuthorizationProvisioningTests {

    @Autowired
    private JdbcClient jdbcClient;

        @Autowired
        private DataSource dataSource;

        @BeforeEach
        void createPermissionFixture() {
                jdbcClient.sql("""
                                CREATE TABLE IF NOT EXISTS platform.permissions (
                                        id UUID PRIMARY KEY,
                                        code VARCHAR(160) NOT NULL UNIQUE,
                                        resource VARCHAR(80) NOT NULL,
                                        action VARCHAR(80) NOT NULL,
                                        description VARCHAR(1000),
                                        system_defined BOOLEAN NOT NULL,
                                        is_active BOOLEAN NOT NULL,
                                        UNIQUE (resource, action)
                                )
                                """).update();
                jdbcClient.sql("DELETE FROM platform.permissions").update();
                jdbcClient.sql("""
                                INSERT INTO platform.permissions
                                        (id, code, resource, action, description, system_defined, is_active)
                                VALUES
                                        ('c72d2587-ec72-4a6d-ae94-850f438ed401',
                                         'SERVICE_PROFIT_OPPORTUNITY.CREATE',
                                         'SERVICE_PROFIT_OPPORTUNITY', 'CREATE', 'Create opportunity', TRUE, TRUE),
                                        ('8c221e8a-b08e-4dc1-976f-5f2f72f8ca02',
                                         'SERVICE_PROFIT_OPPORTUNITY.READ',
                                         'SERVICE_PROFIT_OPPORTUNITY', 'READ', 'Read opportunity', TRUE, TRUE)
                                """).update();
                try (var connection = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(connection, new EncodedResource(
                            new ClassPathResource("db/migration/V36__register_service_profit_follow_up_permissions.sql")));
                } catch (java.sql.SQLException exception) {
                    throw new IllegalStateException("Could not apply V36 authorization fixture", exception);
                }
        }

        @AfterEach
        void clearPermissionFixture() {
                jdbcClient.sql("DROP TABLE IF EXISTS platform.permissions").update();
        }

    @Test
    void registersExactlyTheTwoBoundedFollowUpPermissions() {
        List<PermissionRow> permissions = jdbcClient.sql("""
                SELECT code, resource, action, system_defined, is_active
                  FROM platform.permissions
                 WHERE resource = 'SERVICE_PROFIT_FOLLOW_UP'
                 ORDER BY code
                """)
                .query((resultSet, rowNum) -> new PermissionRow(
                        resultSet.getString("code"),
                        resultSet.getString("resource"),
                        resultSet.getString("action"),
                        resultSet.getBoolean("system_defined"),
                        resultSet.getBoolean("is_active")))
                .list();

        assertEquals(List.of(
                new PermissionRow(
                        ServiceProfitPermissions.FOLLOW_UP_MANAGE,
                        "SERVICE_PROFIT_FOLLOW_UP",
                        "MANAGE",
                        true,
                        true),
                new PermissionRow(
                        ServiceProfitPermissions.FOLLOW_UP_READ,
                        "SERVICE_PROFIT_FOLLOW_UP",
                        "READ",
                        true,
                        true)), permissions);
        assertEquals(2, jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.permissions
                 WHERE code IN (:readCode, :manageCode)
                """)
                .param("readCode", ServiceProfitPermissions.FOLLOW_UP_READ)
                .param("manageCode", ServiceProfitPermissions.FOLLOW_UP_MANAGE)
                .query(Integer.class)
                .single());
    }

    @Test
    void leavesExistingOpportunityPermissionsUnchangedAndAddsNoOtherFollowUpPermission() {
        List<String> opportunityCodes = jdbcClient.sql("""
                SELECT code
                  FROM platform.permissions
                 WHERE resource = 'SERVICE_PROFIT_OPPORTUNITY'
                 ORDER BY code
                """)
                .query(String.class)
                .list();

        assertEquals(List.of(
                ServiceProfitPermissions.OPPORTUNITY_CREATE,
                ServiceProfitPermissions.OPPORTUNITY_READ), opportunityCodes);
        assertTrue(jdbcClient.sql("""
                SELECT COUNT(*)
                  FROM platform.permissions
                 WHERE resource = 'SERVICE_PROFIT_FOLLOW_UP'
                   AND action NOT IN ('READ', 'MANAGE')
                """)
                .query(Integer.class)
                .single() == 0);
    }

    private record PermissionRow(
            String code,
            String resource,
            String action,
            boolean systemDefined,
            boolean active
    ) {
    }
}