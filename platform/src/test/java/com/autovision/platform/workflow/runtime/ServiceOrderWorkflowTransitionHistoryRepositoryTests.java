package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.ServiceOrder;
import com.autovision.platform.aftersales.ServiceOrderRepository;
import com.autovision.platform.workflow.ServiceWorkflowDefinition;
import com.autovision.platform.workflow.ServiceWorkflowDefinitionRepository;
import com.autovision.platform.workflow.ServiceWorkflowStage;
import com.autovision.platform.workflow.ServiceWorkflowStageRepository;
import com.autovision.platform.workflow.ServiceWorkflowStatus;
import com.autovision.platform.workflow.ServiceWorkflowStatusRepository;
import com.autovision.platform.workflow.ServiceWorkflowTransition;
import com.autovision.platform.workflow.ServiceWorkflowTransitionRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceOrderWorkflowTransitionHistoryRepositoryTests {

    @Autowired private JdbcClient jdbcClient;
    @Autowired private ServiceOrderWorkflowTransitionHistoryRepository historyRepository;
    @Autowired private ServiceOrderWorkflowExecutionRepository executionRepository;
    @Autowired private ServiceWorkflowTransitionRepository transitionRepository;
    @Autowired private ServiceWorkflowStageRepository stageRepository;
    @Autowired private ServiceWorkflowStatusRepository statusRepository;
    @Autowired private ServiceWorkflowVersionRepository versionRepository;
    @Autowired private ServiceWorkflowDefinitionRepository definitionRepository;
    @Autowired private ServiceOrderRepository serviceOrderRepository;

    @BeforeAll
    void addHistoryConstraintsToTestSchema() {
        exec("ALTER TABLE platform.service_workflow_definitions "
                + "ADD CONSTRAINT uq_test_history_definition_id_tenant "
                + "UNIQUE (id, tenant_id)");
        exec("ALTER TABLE platform.service_workflow_versions "
                + "ADD CONSTRAINT uq_test_history_version_id_definition "
                + "UNIQUE (id, workflow_definition_id)");
        exec("ALTER TABLE platform.service_workflow_stages "
                + "ADD CONSTRAINT uq_test_history_stage_id_version "
                + "UNIQUE (id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_statuses "
                + "ADD CONSTRAINT uq_test_history_status_id_stage "
                + "UNIQUE (id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT uq_test_history_transition_id_version "
                + "UNIQUE (id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_history_transition_from_stage "
                + "FOREIGN KEY (from_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_history_transition_from_status "
                + "FOREIGN KEY (from_status_id, from_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_history_transition_to_stage "
                + "FOREIGN KEY (to_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_history_transition_to_status "
                + "FOREIGN KEY (to_status_id, to_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT uq_test_history_execution_service_order "
                + "UNIQUE (service_order_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT uq_test_history_execution_id_tenant_order "
                + "UNIQUE (id, tenant_id, service_order_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_history_execution_order "
                + "FOREIGN KEY (service_order_id, tenant_id) "
                + "REFERENCES platform.service_orders(id, tenant_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_history_execution_stage "
                + "FOREIGN KEY (current_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_order_workflow_executions "
                + "ADD CONSTRAINT fk_test_history_execution_status "
                + "FOREIGN KEY (current_status_id, current_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_order "
                + "FOREIGN KEY (service_order_id, tenant_id) "
                + "REFERENCES platform.service_orders(id, tenant_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_execution "
                + "FOREIGN KEY (workflow_execution_id, tenant_id, service_order_id) "
                + "REFERENCES platform.service_order_workflow_executions("
                + "id, tenant_id, service_order_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_definition "
                + "FOREIGN KEY (workflow_definition_id, tenant_id) "
                + "REFERENCES platform.service_workflow_definitions(id, tenant_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_version "
                + "FOREIGN KEY (workflow_version_id, workflow_definition_id) "
                + "REFERENCES platform.service_workflow_versions(id, workflow_definition_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_transition "
                + "FOREIGN KEY (transition_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_transitions(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_from_stage "
                + "FOREIGN KEY (from_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_from_status "
                + "FOREIGN KEY (from_status_id, from_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_to_stage "
                + "FOREIGN KEY (to_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT fk_test_history_to_status "
                + "FOREIGN KEY (to_status_id, to_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_order_workflow_transition_histories "
                + "ADD CONSTRAINT ck_test_history_not_self_loop CHECK (NOT "
                + "(from_stage_id = to_stage_id AND from_status_id = to_status_id))");
    }

    @AfterEach
    void clearData() {
        historyRepository.deleteAll();
        executionRepository.deleteAll();
        transitionRepository.deleteAll();
        statusRepository.deleteAll();
        stageRepository.deleteAll();
        versionRepository.deleteAll();
        definitionRepository.deleteAll();
        serviceOrderRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsValidHistory() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowTransitionHistory saved = save(history(fixture));

        ServiceOrderWorkflowTransitionHistory reloaded = historyRepository
                .findById(saved.getId()).orElseThrow();

        assertEquals(fixture.tenantId, reloaded.getTenantId());
        assertEquals(fixture.order.getId(), reloaded.getServiceOrderId());
        assertEquals(fixture.execution.getId(), reloaded.getWorkflowExecutionId());
        assertEquals(fixture.definition.getId(), reloaded.getWorkflowDefinitionId());
        assertEquals(fixture.version.getId(), reloaded.getWorkflowVersionId());
        assertEquals(fixture.transition.getId(), reloaded.getTransitionId());
        assertEquals(fixture.fromStage.getId(), reloaded.getFromStageId());
        assertEquals(fixture.fromStatus.getId(), reloaded.getFromStatusId());
        assertEquals(fixture.toStage.getId(), reloaded.getToStageId());
        assertEquals(fixture.toStatus.getId(), reloaded.getToStatusId());
    }

    @Test
    void findsByServiceOrderAndTenantOrderedByExecutedAt() {
        Fixture fixture = fixture();
        OffsetDateTime first = fixture.now;
        OffsetDateTime second = fixture.now.plusMinutes(5);
        save(history(fixture, first));
        save(history(fixture, second));

        List<ServiceOrderWorkflowTransitionHistory> results = historyRepository
                .findByServiceOrderIdAndTenantIdOrderByExecutedAtAsc(
                        fixture.order.getId(), fixture.tenantId);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getExecutedAt().isBefore(results.get(1).getExecutedAt()));
    }

    @Test
    void findsByWorkflowExecutionAndTenantOrderedByExecutedAt() {
        Fixture fixture = fixture();
        save(history(fixture, fixture.now));
        save(history(fixture, fixture.now.plusMinutes(5)));

        List<ServiceOrderWorkflowTransitionHistory> results = historyRepository
                .findByWorkflowExecutionIdAndTenantIdOrderByExecutedAtAsc(
                        fixture.execution.getId(), fixture.tenantId);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getExecutedAt().isBefore(results.get(1).getExecutedAt()));
    }

    @Test
    void findsByIdAndTenantId() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowTransitionHistory saved = save(history(fixture));

        assertTrue(historyRepository.findByIdAndTenantId(
                saved.getId(), fixture.tenantId).isPresent());
    }

    @Test
    void crossTenantLookupDoesNotDiscloseRecord() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowTransitionHistory saved = save(history(fixture));

        assertFalse(historyRepository.findByIdAndTenantId(
                saved.getId(), UUID.randomUUID()).isPresent());
        assertTrue(historyRepository.findByServiceOrderIdAndTenantIdOrderByExecutedAtAsc(
                fixture.order.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void executionContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-EXEC");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        second.execution.getId(), first.definition.getId(),
                        first.version.getId(), first.transition.getId(),
                        first.fromStage.getId(), first.fromStatus.getId(),
                        first.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void serviceOrderTenantContainmentEnforced() {
        Fixture fixture = fixture();
        ServiceOrder otherTenantOrder = saveOrder(UUID.randomUUID(), "SO-OTHER-TENANT");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), fixture.tenantId, otherTenantOrder.getId(),
                        fixture.execution.getId(), fixture.definition.getId(),
                        fixture.version.getId(), fixture.transition.getId(),
                        fixture.fromStage.getId(), fixture.fromStatus.getId(),
                        fixture.toStage.getId(), fixture.toStatus.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void definitionTenantContainmentEnforced() {
        Fixture fixture = fixture();

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.execution.getId(), UUID.randomUUID(),
                        fixture.version.getId(), fixture.transition.getId(),
                        fixture.fromStage.getId(), fixture.fromStatus.getId(),
                        fixture.toStage.getId(), fixture.toStatus.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void versionDefinitionContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-VERSION");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        second.version.getId(), first.transition.getId(),
                        first.fromStage.getId(), first.fromStatus.getId(),
                        first.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void transitionVersionContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-TRANSITION");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        first.version.getId(), second.transition.getId(),
                        first.fromStage.getId(), first.fromStatus.getId(),
                        first.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void fromStageVersionContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-FROM-STAGE");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        first.version.getId(), first.transition.getId(),
                        second.fromStage.getId(), first.fromStatus.getId(),
                        first.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void fromStatusStageContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-FROM-STATUS");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        first.version.getId(), first.transition.getId(),
                        first.fromStage.getId(), second.fromStatus.getId(),
                        first.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void toStageVersionContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-TO-STAGE");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        first.version.getId(), first.transition.getId(),
                        first.fromStage.getId(), first.fromStatus.getId(),
                        second.toStage.getId(), first.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void toStatusStageContainmentEnforced() {
        Fixture first = fixture();
        Fixture second = fixtureWithTenant(first.tenantId, "SO-SECOND-TO-STATUS");

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), first.tenantId, first.order.getId(),
                        first.execution.getId(), first.definition.getId(),
                        first.version.getId(), first.transition.getId(),
                        first.fromStage.getId(), first.fromStatus.getId(),
                        first.toStage.getId(), second.toStatus.getId(),
                        first.principalId, first.now)));
    }

    @Test
    void nonexistentTransitionRejected() {
        Fixture fixture = fixture();

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.execution.getId(), fixture.definition.getId(),
                        fixture.version.getId(), UUID.randomUUID(),
                        fixture.fromStage.getId(), fixture.fromStatus.getId(),
                        fixture.toStage.getId(), fixture.toStatus.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void nonexistentStageOrStatusRejected() {
        Fixture fixture = fixture();

        assertThrows(DataIntegrityViolationException.class, () -> save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.execution.getId(), fixture.definition.getId(),
                        fixture.version.getId(), fixture.transition.getId(),
                        UUID.randomUUID(), fixture.fromStatus.getId(),
                        fixture.toStage.getId(), fixture.toStatus.getId(),
                        fixture.principalId, fixture.now)));
    }

    @Test
    void completeNoOpRejectedByDbConstraint() {
        Fixture fixture = fixture();

        assertThrows(org.springframework.dao.DataAccessException.class, () ->
                jdbcClient.sql("INSERT INTO "
                                + "platform.service_order_workflow_transition_histories "
                                + "(id, tenant_id, service_order_id, workflow_execution_id, "
                                + "workflow_definition_id, workflow_version_id, transition_id, "
                                + "from_stage_id, from_status_id, to_stage_id, to_status_id, "
                                + "executed_by_principal_id, executed_at) VALUES "
                                + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                        .params(UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                                fixture.execution.getId(), fixture.definition.getId(),
                                fixture.version.getId(), fixture.transition.getId(),
                                fixture.fromStage.getId(), fixture.fromStatus.getId(),
                                fixture.fromStage.getId(), fixture.fromStatus.getId(),
                                fixture.principalId, fixture.now)
                        .update());
    }

    @Test
    void sameStageDifferentStatusPersists() {
        Fixture fixture = fixture();
        ServiceWorkflowStatus otherStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(UUID.randomUUID(), fixture.fromStage.getId(),
                        "OTHER-" + UUID.randomUUID(), "Other", 2));
        ServiceWorkflowTransition sameStageTransition = transitionRepository.saveAndFlush(
                ServiceWorkflowTransition.create(UUID.randomUUID(), fixture.version.getId(),
                        fixture.fromStage.getId(), fixture.fromStatus.getId(),
                        fixture.fromStage.getId(), otherStatus.getId(),
                        "SAME_STAGE-" + UUID.randomUUID(), "Same stage", 2,
                        fixture.principalId, fixture.now));

        ServiceOrderWorkflowTransitionHistory saved = save(
                ServiceOrderWorkflowTransitionHistory.record(
                        UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                        fixture.execution.getId(), fixture.definition.getId(),
                        fixture.version.getId(), sameStageTransition.getId(),
                        fixture.fromStage.getId(), fixture.fromStatus.getId(),
                        fixture.fromStage.getId(), otherStatus.getId(),
                        fixture.principalId, fixture.now));

        assertEquals(fixture.fromStage.getId(), saved.getToStageId());
        assertEquals(otherStatus.getId(), saved.getToStatusId());
    }

    @Test
    void auditPrincipalAndTimePersist() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowTransitionHistory saved = save(history(fixture));
        ServiceOrderWorkflowTransitionHistory reloaded = historyRepository
                .findById(saved.getId()).orElseThrow();

        assertEquals(fixture.principalId, reloaded.getExecutedByPrincipalId());
        assertEquals(fixture.now.toInstant().toEpochMilli(),
                reloaded.getExecutedAt().toInstant().toEpochMilli());
    }

    @Test
    void referencedParentDeletionDoesNotCascadeDeleteHistory() {
        Fixture fixture = fixture();
        ServiceOrderWorkflowTransitionHistory saved = save(history(fixture));

        assertThrows(DataIntegrityViolationException.class, () -> {
            definitionRepository.delete(fixture.definition);
            definitionRepository.flush();
        });
        assertTrue(historyRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void multipleHistoryRowsAllowedForSameExecution() {
        Fixture fixture = fixture();
        save(history(fixture, fixture.now));
        save(history(fixture, fixture.now.plusMinutes(1)));

        assertEquals(2, historyRepository.findByWorkflowExecutionIdAndTenantIdOrderByExecutedAtAsc(
                fixture.execution.getId(), fixture.tenantId).size());
    }

    @Test
    void chronologicalOrderingIsDeterministic() {
        Fixture fixture = fixture();
        OffsetDateTime t1 = fixture.now;
        OffsetDateTime t2 = fixture.now.plusMinutes(3);
        OffsetDateTime t3 = fixture.now.plusMinutes(7);
        save(history(fixture, t2));
        save(history(fixture, t1));
        save(history(fixture, t3));

        List<Long> times = historyRepository
                .findByWorkflowExecutionIdAndTenantIdOrderByExecutedAtAsc(
                        fixture.execution.getId(), fixture.tenantId)
                .stream().map(record -> record.getExecutedAt().toInstant().toEpochMilli())
                .toList();

        assertEquals(List.of(t1.toInstant().toEpochMilli(), t2.toInstant().toEpochMilli(),
                t3.toInstant().toEpochMilli()), times);
    }

    @Test
    void persistenceDoesNotMutateRuntimeExecution() {
        Fixture fixture = fixture();
        UUID currentStageBefore = fixture.execution.getCurrentStageId();
        UUID currentStatusBefore = fixture.execution.getCurrentStatusId();

        save(history(fixture));

        ServiceOrderWorkflowExecution reloaded = executionRepository
                .findById(fixture.execution.getId()).orElseThrow();
        assertEquals(currentStageBefore, reloaded.getCurrentStageId());
        assertEquals(currentStatusBefore, reloaded.getCurrentStatusId());
    }

    @Test
    void persistenceDoesNotMutateWorkflowConfiguration() {
        Fixture fixture = fixture();
        String transitionCode = fixture.transition.getCode();

        save(history(fixture));

        assertEquals(transitionCode, transitionRepository.findById(fixture.transition.getId())
                .orElseThrow().getCode());
    }

    private ServiceOrderWorkflowTransitionHistory history(Fixture fixture) {
        return history(fixture, fixture.now);
    }

    private ServiceOrderWorkflowTransitionHistory history(
            Fixture fixture, OffsetDateTime executedAt
    ) {
        return ServiceOrderWorkflowTransitionHistory.record(
                UUID.randomUUID(), fixture.tenantId, fixture.order.getId(),
                fixture.execution.getId(), fixture.definition.getId(),
                fixture.version.getId(), fixture.transition.getId(),
                fixture.fromStage.getId(), fixture.fromStatus.getId(),
                fixture.toStage.getId(), fixture.toStatus.getId(),
                fixture.principalId, executedAt);
    }

    private ServiceOrderWorkflowTransitionHistory save(
            ServiceOrderWorkflowTransitionHistory history
    ) {
        return historyRepository.saveAndFlush(history);
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
        ServiceWorkflowStage fromStage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(UUID.randomUUID(), version.getId(),
                        "FROM-" + UUID.randomUUID(), "From stage", 1));
        ServiceWorkflowStatus fromStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(UUID.randomUUID(), fromStage.getId(),
                        "OPEN-" + UUID.randomUUID(), "Open", 1));
        ServiceWorkflowStage toStage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(UUID.randomUUID(), version.getId(),
                        "TO-" + UUID.randomUUID(), "To stage", 2));
        ServiceWorkflowStatus toStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(UUID.randomUUID(), toStage.getId(),
                        "READY-" + UUID.randomUUID(), "Ready", 1));
        ServiceWorkflowTransition transition = transitionRepository.saveAndFlush(
                ServiceWorkflowTransition.create(UUID.randomUUID(), version.getId(),
                        fromStage.getId(), fromStatus.getId(), toStage.getId(),
                        toStatus.getId(), "TRANSITION-" + UUID.randomUUID(), "Transition",
                        1, principalId, now));
        ServiceOrderWorkflowExecution execution = executionRepository.saveAndFlush(
                ServiceOrderWorkflowExecution.start(UUID.randomUUID(), tenantId,
                        order.getId(), definition.getId(), version.getId(),
                        fromStage.getId(), fromStatus.getId(), principalId, now));
        return new Fixture(tenantId, principalId, now, order, definition, version,
                fromStage, fromStatus, toStage, toStatus, transition, execution);
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
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStage toStage,
            ServiceWorkflowStatus toStatus,
            ServiceWorkflowTransition transition,
            ServiceOrderWorkflowExecution execution
    ) {
    }
}
