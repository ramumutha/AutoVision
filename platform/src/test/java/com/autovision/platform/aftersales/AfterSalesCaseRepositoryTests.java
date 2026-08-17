package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AfterSalesCaseRepositoryTests {

    @Autowired
    private AfterSalesCaseRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();

    @BeforeEach
    void createSchema() {
        exec("""
                CREATE TABLE IF NOT EXISTS platform.aftersales_cases (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    dealer_id UUID,
                    branch_id UUID,
                    case_number VARCHAR(80) NOT NULL,
                    lifecycle_status VARCHAR(32) NOT NULL,
                    source_channel VARCHAR(32),
                    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    closed_at TIMESTAMP WITH TIME ZONE,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_by_principal_id UUID,
                    updated_by_principal_id UUID,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    CONSTRAINT uq_aftersales_cases_tenant_case_number
                        UNIQUE (tenant_id, case_number)
                )
                """);
    }

    @AfterEach
    void clearTables() {
        exec("DELETE FROM platform.aftersales_cases");
    }

    @Test
    void findsCaseByIdWithinTenant() {
        UUID caseId = insertCase(tenantId, "ASC-1001", "OPEN");

        assertTrue(repository.findByIdAndTenantId(caseId, tenantId).isPresent());
    }

    @Test
    void doesNotFindCaseByIdForAnotherTenant() {
        UUID caseId = insertCase(tenantId, "ASC-1002", "OPEN");

        assertFalse(
                repository.findByIdAndTenantId(caseId, otherTenantId).isPresent()
        );
    }

    @Test
    void findsCaseByTenantAndCaseNumber() {
        insertCase(tenantId, "ASC-1003", "OPEN");

        assertTrue(
                repository.findByTenantIdAndCaseNumber(
                        tenantId,
                        "ASC-1003"
                ).isPresent()
        );

        assertFalse(
                repository.findByTenantIdAndCaseNumber(
                        otherTenantId,
                        "ASC-1003"
                ).isPresent()
        );
    }

    @Test
    void filtersCasesByTenantAndLifecycleStatus() {
        insertCase(tenantId, "ASC-1004", "OPEN");
        insertCase(tenantId, "ASC-1005", "CLOSED");
        insertCase(otherTenantId, "ASC-1006", "OPEN");

        List<AfterSalesCase> cases =
                repository.findAllByTenantIdAndLifecycleStatus(
                        tenantId,
                        "OPEN"
                );

        assertEquals(1, cases.size());
        assertEquals("ASC-1004", cases.getFirst().getCaseNumber());
        assertEquals(tenantId, cases.getFirst().getTenantId());
    }

    @Test
    void allowsSameCaseNumberAcrossDifferentTenants() {
        insertCase(tenantId, "ASC-1007", "OPEN");
        insertCase(otherTenantId, "ASC-1007", "OPEN");

        assertTrue(
                repository.existsByTenantIdAndCaseNumber(
                        tenantId,
                        "ASC-1007"
                )
        );

        assertTrue(
                repository.existsByTenantIdAndCaseNumber(
                        otherTenantId,
                        "ASC-1007"
                )
        );

        assertEquals(1, repository.findAllByTenantId(tenantId).size());
        assertEquals(1, repository.findAllByTenantId(otherTenantId).size());
    }

    private UUID insertCase(
            UUID caseTenantId,
            String caseNumber,
            String lifecycleStatus
    ) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbcClient.sql("""
                INSERT INTO platform.aftersales_cases
                    (id, tenant_id, case_number, lifecycle_status,
                     opened_at, version, created_at, updated_at)
                VALUES
                    (:id, :tenantId, :caseNumber, :lifecycleStatus,
                     :openedAt, 0, :createdAt, :updatedAt)
                """)
                .param("id", id)
                .param("tenantId", caseTenantId)
                .param("caseNumber", caseNumber)
                .param("lifecycleStatus", lifecycleStatus)
                .param("openedAt", now)
                .param("createdAt", now)
                .param("updatedAt", now)
                .update();

        return id;
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }
}