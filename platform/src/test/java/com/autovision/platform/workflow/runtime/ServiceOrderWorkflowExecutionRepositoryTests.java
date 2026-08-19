package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.ServiceOrder;
import com.autovision.platform.aftersales.ServiceOrderRepository;
import com.autovision.platform.aftersales.ServiceOrderStatus;
import com.autovision.platform.workflow.ServiceWorkflowDefinition;
import com.autovision.platform.workflow.ServiceWorkflowDefinitionRepository;
import com.autovision.platform.workflow.ServiceWorkflowStage;
import com.autovision.platform.workflow.ServiceWorkflowStageRepository;
import com.autovision.platform.workflow.ServiceWorkflowStatus;
import com.autovision.platform.workflow.ServiceWorkflowStatusRepository;
import com.autovision.platform.workflow.ServiceWorkflowVersion;
import com.autovision.platform.workflow.ServiceWorkflowVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceOrderWorkflowExecutionRepositoryTests {

    @Autowired private JdbcClient jdbcClient;
    @Autowired private ServiceOrderWorkflowExecutionRepository executionRepository;
    @Autowired private ServiceOrderRepository serviceOrderRepository;
    @Autowired private ServiceWorkflowDefinitionRepository definitionRepository;
    @Autowired private ServiceWorkflowVersionRepository versionRepository;
    @Autowired private ServiceWorkflowStageRepository stageRepository;
    @Autowired private ServiceWorkflowStatusRepository statusRepository;

    @BeforeAll
    void addRuntimeConstraintsToTestSchema() {
        exec("ALTER TABLE platform.service_workflow_definitions "
                + "ADD CONSTRAINT uq_test_workflow_definition_id_tenant "
                + "UNIQUE (id, tenant_id)");
        exec("ALTER TABLE platform.service_workflow_versions "
                + "ADD CONSTRAINT uq_test_workflow_version_id_definition "
                + "UNIQUE (id, workflow_definition_id)");
        exec("ALTER TABLE platform.service_workflow_stages "
                + "ADD CONSTRAINT uq_test_workflow_stage_id_version "
                + "UNIQUE (id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_statuses "
                + "ADD CONSTRAINT uq_test_workflow_status_id_stage "
                + "UNIQUE (id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT uq_test_execution_service_order "
                + "UNIQUE (service_order_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_execution_order "
                + "FOREIGN KEY (service_order_id, tenant_id) "
                + "REFERENCES platform.service_orders(id, tenant_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_execution_definition "
                + "FOREIGN KEY (workflow_definition_id, tenant_id) "
                + "REFERENCES platform.service_workflow_definitions(id, tenant_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_execution_version "
                + "FOREIGN KEY (workflow_version_id, workflow_definition_id) "
                + "REFERENCES platform.service_workflow_versions(id, workflow_definition_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_execution_stage "
                + "FOREIGN KEY (current_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_execution_status "
                + "FOREIGN KEY (current_status_id, current_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
    }

    @AfterEach
    void clearData() {
        executionRepository.deleteAll();
        statusRepository.deleteAll();
        stageRepository.deleteAll();
        versionRepository.deleteAll();
        definitionRepository.deleteAll();
        serviceOrderRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsValidExecution() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowExecution saved = saveExecution(fixture);

        ServiceOrderWorkflowExecution reloaded = executionRepository
                .findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), reloaded.getId());
        assertEquals(fixture.tenantId, reloaded.getTenantId());
        assertEquals(fixture.order.getId(), reloaded.getServiceOrderId());
        assertEquals(fixture.definition.getId(), reloaded.getWorkflowDefinitionId());
        assertEquals(fixture.version.getId(), reloaded.getWorkflowVersionId());
        assertEquals(fixture.stage.getId(), reloaded.getCurrentStageId());
        assertEquals(fixture.status.getId(), reloaded.getCurrentStatusId());
    }

    @Test
    void findsByServiceOrderIdAndTenantAndIdTenant() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowExecution saved = saveExecution(fixture);

        assertTrue(executionRepository.findByServiceOrderId(
                fixture.order.getId()).isPresent());
        assertTrue(executionRepository.findByServiceOrderIdAndTenantId(
                fixture.order.getId(), fixture.tenantId).isPresent());
        assertFalse(executionRepository.findByServiceOrderIdAndTenantId(
                fixture.order.getId(), UUID.randomUUID()).isPresent());
        assertTrue(executionRepository.findByIdAndTenantId(
                saved.getId(), fixture.tenantId).isPresent());
        assertFalse(executionRepository.findByIdAndTenantId(
                saved.getId(), UUID.randomUUID()).isPresent());
    }

    @Test
    void existsQueriesAndDuplicateOrderAreEnforced() {
        Fixture fixture = fixture();
        saveExecution(fixture);

        assertTrue(executionRepository.existsByServiceOrderId(
                fixture.order.getId()));
        assertTrue(executionRepository.existsByServiceOrderIdAndTenantId(
                fixture.order.getId(), fixture.tenantId));
        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.definition.getId(), fixture.version.getId(),
                        fixture.stage.getId(), fixture.status.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void differentServiceOrdersCanHaveSeparateExecutions() {
        Fixture first = fixture();
        ServiceOrder secondOrder = saveOrder(first.tenantId, "SO-SECOND");
        saveExecution(first);

        ServiceOrderWorkflowExecution second = ServiceOrderWorkflowExecution.start(
                UUID.randomUUID(), first.tenantId, secondOrder.getId(),
                first.definition.getId(), first.version.getId(), first.stage.getId(),
                first.status.getId(), first.principalId, first.now);
        executionRepository.saveAndFlush(second);

        assertEquals(2, executionRepository.count());
    }

    @Test
    void serviceOrderFromAnotherTenantIsRejected() {
        Fixture fixture = fixture();
        ServiceOrder otherTenantOrder = saveOrder(UUID.randomUUID(), "SO-OTHER-TENANT");

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), fixture.tenantId, otherTenantOrder.getId(),
                        fixture.definition.getId(), fixture.version.getId(),
                        fixture.stage.getId(), fixture.status.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void workflowDefinitionForeignKeyIsEnforced() {
        Fixture fixture = fixture();

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        UUID.randomUUID(), fixture.version.getId(), fixture.stage.getId(),
                        fixture.status.getId(), fixture.principalId, fixture.now)));
    }

    @Test
    void workflowVersionMustBelongToDefinition() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-DEFINITION");

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.definition.getId(), second.version.getId(),
                        first.stage.getId(), first.status.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void stageMustBelongToPinnedVersion() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-STAGE");

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.definition.getId(), first.version.getId(),
                        second.stage.getId(), first.status.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void statusMustBelongToCurrentStage() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-STATUS");

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.definition.getId(), first.version.getId(),
                        first.stage.getId(), second.status.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void nonexistentVersionStageAndStatusAreRejected() {
        Fixture fixture = fixture();

        assertThrows(DataIntegrityViolationException.class, () ->
                executionRepository.saveAndFlush(ServiceOrderWorkflowExecution.start(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.definition.getId(), UUID.randomUUID(), fixture.stage.getId(),
                        fixture.status.getId(), fixture.principalId, fixture.now)));
    }

    @Test
    void auditFieldsAndOptimisticVersionPersist() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowExecution saved = saveExecution(fixture);
        ServiceOrderWorkflowExecution reloaded = executionRepository
                .findById(saved.getId()).orElseThrow();

        assertEquals(fixture.principalId, reloaded.getCreatedByPrincipalId());
        assertEquals(fixture.principalId, reloaded.getUpdatedByPrincipalId());
        assertEquals(fixture.now.toInstant().toEpochMilli(),
                reloaded.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(fixture.now.toInstant().toEpochMilli(),
                reloaded.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(0, reloaded.getVersion());
        assertNotNull(reloaded.getCreatedAt());
    }

    @Test
    void persistenceDoesNotMutateOrderStatusOrConfiguration() {
        Fixture fixture = fixture();
        ServiceOrderStatus orderStatus = fixture.order.getStatus();
        String definitionCode = fixture.definition.getCode();
        ServiceWorkflowVersionStatusSnapshot versionStatus =
                new ServiceWorkflowVersionStatusSnapshot(fixture.version.getStatus());
        saveExecution(fixture);

        assertEquals(orderStatus, serviceOrderRepository.findById(fixture.order.getId())
                .orElseThrow().getStatus());
        assertEquals(definitionCode, definitionRepository.findById(fixture.definition.getId())
                .orElseThrow().getCode());
        assertEquals(versionStatus.status, versionRepository.findById(fixture.version.getId())
                .orElseThrow().getStatus());
    }

    @Test
    void referencedConfigurationCannotBeDeletedThroughCascade() {
        Fixture fixture = fixture();
        saveExecution(fixture);

        assertThrows(DataIntegrityViolationException.class, () -> {
            definitionRepository.delete(fixture.definition);
            definitionRepository.flush();
        });
        assertTrue(executionRepository.findById(fixture.executionId()).isPresent());
    }

    private ServiceOrderWorkflowExecution saveExecution(Fixture fixture) {
        ServiceOrderWorkflowExecution execution = ServiceOrderWorkflowExecution.start(
                fixture.executionId(), fixture.tenantId, fixture.order.getId(),
                fixture.definition.getId(), fixture.version.getId(), fixture.stage.getId(),
                fixture.status.getId(), fixture.principalId, fixture.now);
        return executionRepository.saveAndFlush(execution);
    }

    private Fixture fixture() {
        return fixtureWithTenant(UUID.randomUUID(), "SO-PRIMARY");
    }

    private Fixture fixtureWithTenant(UUID tenantId, String orderNumber) {
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ServiceOrder order = saveOrder(tenantId, orderNumber);
        ServiceWorkflowDefinition definition = definitionRepository.saveAndFlush(
                ServiceWorkflowDefinition.create(UUID.randomUUID(), tenantId, null, null,
                        "WORKFLOW-" + UUID.randomUUID(), "Workflow", principalId, now));
        ServiceWorkflowVersion version = versionRepository.saveAndFlush(
                ServiceWorkflowVersion.draft(UUID.randomUUID(), definition.getId(), 1,
                        principalId, now));
        ServiceWorkflowStage stage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(UUID.randomUUID(), version.getId(),
                        "STAGE-" + UUID.randomUUID(), "Stage", 1));
        ServiceWorkflowStatus status = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(UUID.randomUUID(), stage.getId(),
                        "STATUS-" + UUID.randomUUID(), "Status", 1));
        return new Fixture(tenantId, principalId, now, order, definition,
                version, stage, status, UUID.randomUUID());
    }

    private ServiceOrder saveOrder(UUID tenantId, String orderNumber) {
        return serviceOrderRepository.saveAndFlush(ServiceOrder.open(
                UUID.randomUUID(), tenantId, null, null, orderNumber,
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now()));
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }

    private record Fixture(
            UUID tenantId,
            UUID principalId,
            OffsetDateTime now,
            ServiceOrder order,
            ServiceWorkflowDefinition definition,
            ServiceWorkflowVersion version,
            ServiceWorkflowStage stage,
            ServiceWorkflowStatus status,
            UUID executionId
    ) {
    }

    private record ServiceWorkflowVersionStatusSnapshot(
            com.autovision.platform.workflow.ServiceWorkflowVersionStatus status
    ) {
    }
}