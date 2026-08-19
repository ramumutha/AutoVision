package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ServiceOrderStatus;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceWorkflowTransitionRepositoryTests {

    @Autowired private JdbcClient jdbcClient;
    @Autowired private ServiceWorkflowTransitionRepository transitionRepository;
    @Autowired private ServiceWorkflowDefinitionRepository definitionRepository;
    @Autowired private ServiceWorkflowVersionRepository versionRepository;
    @Autowired private ServiceWorkflowStageRepository stageRepository;
    @Autowired private ServiceWorkflowStatusRepository statusRepository;

    @BeforeAll
    void addTransitionConstraintsToTestSchema() {
        exec("ALTER TABLE platform.service_workflow_versions "
                + "ADD CONSTRAINT uq_test_transition_version_id_definition "
                + "UNIQUE (id, workflow_definition_id)");
        exec("ALTER TABLE platform.service_workflow_stages "
                + "ADD CONSTRAINT uq_test_transition_stage_id_version "
                + "UNIQUE (id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_statuses "
                + "ADD CONSTRAINT uq_test_transition_status_id_stage "
                + "UNIQUE (id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_transition_version "
                + "FOREIGN KEY (workflow_version_id) "
                + "REFERENCES platform.service_workflow_versions(id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_transition_from_stage "
                + "FOREIGN KEY (from_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_transition_from_status "
                + "FOREIGN KEY (from_status_id, from_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_transition_to_stage "
                + "FOREIGN KEY (to_stage_id, workflow_version_id) "
                + "REFERENCES platform.service_workflow_stages(id, workflow_version_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT fk_test_transition_to_status "
                + "FOREIGN KEY (to_status_id, to_stage_id) "
                + "REFERENCES platform.service_workflow_statuses(id, workflow_stage_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT uq_test_transition_version_code "
                + "UNIQUE (workflow_version_id, code)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT uq_test_transition_version_edge UNIQUE "
                + "(workflow_version_id, from_stage_id, from_status_id, to_stage_id, to_status_id)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT ck_test_transition_sequence CHECK (sequence >= 0)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT ck_test_transition_version CHECK (version >= 0)");
        exec("ALTER TABLE platform.service_workflow_transitions "
                + "ADD CONSTRAINT ck_test_transition_not_self_loop CHECK (NOT "
                + "(from_stage_id = to_stage_id AND from_status_id = to_status_id))");
    }

    @AfterEach
    void clearTransitions() {
        transitionRepository.deleteAll();
        statusRepository.deleteAll();
        stageRepository.deleteAll();
        versionRepository.deleteAll();
        definitionRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsSameStageTransition() {
        Fixture fixture = fixture();
        ServiceWorkflowTransition saved = save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus, "SAME_STAGE"));

        ServiceWorkflowTransition reloaded = transitionRepository.findById(saved.getId())
                .orElseThrow();
        assertEquals(fixture.version.getId(), reloaded.getWorkflowVersionId());
        assertEquals(fixture.fromStage.getId(), reloaded.getFromStageId());
        assertEquals(fixture.toStatus.getId(), reloaded.getToStatusId());
    }

    @Test
    void persistsCrossStageTransitionAndListsBySequence() {
        Fixture fixture = fixture();
        ServiceWorkflowStage toStage = stage(fixture.version, "TO", 2);
        ServiceWorkflowStatus toStatus = status(toStage, "READY", 1);
        stageRepository.saveAndFlush(toStage);
        statusRepository.saveAndFlush(toStatus);
        save(transition(fixture, fixture.fromStage, fixture.fromStatus,
                toStage, toStatus, "LATER", 20));
        save(transition(fixture, fixture.fromStage, fixture.fromStatus,
                fixture.fromStage, fixture.toStatus, "EARLIER", 10));

        assertEquals(List.of("EARLIER", "LATER"), transitionRepository
                .findByWorkflowVersionIdOrderBySequenceAsc(fixture.version.getId())
                .stream().map(ServiceWorkflowTransition::getCode).toList());
    }

    @Test
    void findsContainedAndActiveFromStateTransitions() {
        Fixture fixture = fixture();
        ServiceWorkflowTransition saved = save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus, "ACTIVE"));

        assertTrue(transitionRepository.findByIdAndWorkflowVersionId(
                saved.getId(), fixture.version.getId()).isPresent());
        assertFalse(transitionRepository.findByIdAndWorkflowVersionId(
                saved.getId(), UUID.randomUUID()).isPresent());
        assertEquals(1, transitionRepository
                .findByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndActiveTrueOrderBySequenceAsc(
                        fixture.version.getId(), fixture.fromStage.getId(),
                        fixture.fromStatus.getId()).size());
        assertTrue(transitionRepository.existsByWorkflowVersionIdAndCode(
                fixture.version.getId(), "ACTIVE"));
        assertTrue(transitionRepository
                .existsByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndToStageIdAndToStatusId(
                        fixture.version.getId(), fixture.fromStage.getId(),
                        fixture.fromStatus.getId(), fixture.fromStage.getId(),
                        fixture.toStatus.getId()));
    }

    @Test
    void transitionCodeIsUniquePerVersionButReusableAcrossVersions() {
        Fixture first = fixture();
        save(transition(first, first.fromStage, first.fromStatus,
                first.fromStage, first.toStatus, "SHARED"));
        Fixture second = fixture();
        save(transition(second, second.fromStage, second.fromStatus,
                second.fromStage, second.toStatus, "SHARED"));

        assertEquals(1, transitionRepository
                .findByWorkflowVersionIdOrderBySequenceAsc(first.version.getId()).size());
        assertEquals(1, transitionRepository
                .findByWorkflowVersionIdOrderBySequenceAsc(second.version.getId()).size());
    }

    @Test
    void duplicateIdenticalEdgeIsRejectedButBranchingAndConvergenceRemainValid() {
        Fixture fixture = fixture();
        save(transition(fixture, fixture.fromStage, fixture.fromStatus,
                fixture.fromStage, fixture.toStatus, "EDGE_A"));
        assertThrows(DataIntegrityViolationException.class, () -> save(
                transition(fixture, fixture.fromStage, fixture.fromStatus,
                        fixture.fromStage, fixture.toStatus, "EDGE_B")));

        ServiceWorkflowStatus otherStatus = status(fixture.fromStage, "OTHER", 2);
        statusRepository.saveAndFlush(otherStatus);
        save(transition(fixture, fixture.fromStage, fixture.fromStatus,
                fixture.fromStage, otherStatus, "BRANCH"));

        ServiceWorkflowStage otherStage = stage(fixture.version, "OTHER_STAGE", 3);
        ServiceWorkflowStatus otherStageStatus = status(otherStage, "READY", 1);
        stageRepository.saveAndFlush(otherStage);
        statusRepository.saveAndFlush(otherStageStatus);
        save(transition(fixture, otherStage, otherStageStatus,
                fixture.fromStage, fixture.toStatus, "CONVERGE"));
    }

    @Test
    void versionStageStatusAndSelfLoopConstraintsAreEnforced() {
        Fixture fixture = fixture();
        Fixture other = fixture();

        assertConstraint(() -> save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus,
                "MISSING_VERSION", UUID.randomUUID())));
        assertConstraint(() -> save(transition(fixture, other.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus, "BAD_FROM_STAGE")));
        assertConstraint(() -> save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, other.fromStage, fixture.toStatus, "BAD_TO_STAGE")));
        assertConstraint(() -> save(transition(fixture, fixture.fromStage,
                other.fromStatus, fixture.fromStage, fixture.toStatus, "BAD_FROM_STATUS")));
        assertConstraint(() -> save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, other.toStatus, "BAD_TO_STATUS")));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcClient.sql("""
                INSERT INTO platform.service_workflow_transitions (
                    id, workflow_version_id, from_stage_id, from_status_id,
                    to_stage_id, to_status_id, code, display_name, sequence,
                    active, version, created_by_principal_id,
                    updated_by_principal_id, created_at, updated_at
                ) VALUES (:id, :versionId, :fromStageId, :fromStatusId,
                          :toStageId, :toStatusId, :code, :displayName, 1,
                          TRUE, 0, :principalId, :principalId, :now, :now)
                """)
                .param("id", UUID.randomUUID())
                .param("versionId", fixture.version.getId())
                .param("fromStageId", fixture.fromStage.getId())
                .param("fromStatusId", fixture.fromStatus.getId())
                .param("toStageId", fixture.fromStage.getId())
                .param("toStatusId", fixture.fromStatus.getId())
                .param("code", "SELF_LOOP")
                .param("displayName", "Self loop")
                .param("principalId", fixture.principalId)
                .param("now", fixture.now)
                .update());
        assertThrows(DataIntegrityViolationException.class, () ->
                insertRawTransition(fixture, fixture.fromStage, fixture.fromStatus,
                        fixture.fromStage, fixture.toStatus, "NEGATIVE", -1));
    }

    @Test
    void auditAndVersionPersistWithoutMutatingConfigurationOrServiceOrderStatus() {
        Fixture fixture = fixture();
        ServiceWorkflowTransition saved = save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus, "AUDIT"));
        ServiceWorkflowTransition reloaded = transitionRepository.findById(saved.getId())
                .orElseThrow();

        assertEquals(fixture.principalId, reloaded.getCreatedByPrincipalId());
        assertEquals(fixture.principalId, reloaded.getUpdatedByPrincipalId());
        assertEquals(fixture.now.toInstant().toEpochMilli(),
                reloaded.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(0, reloaded.getVersion());
        assertEquals(fixture.fromStage.getCode(),
                stageRepository.findById(fixture.fromStage.getId()).orElseThrow().getCode());
        assertEquals(ServiceOrderStatus.OPEN.name(), "OPEN");
    }

    @Test
    void deletingReferencedConfigurationDoesNotCascadeTransition() {
        Fixture fixture = fixture();
        ServiceWorkflowTransition saved = save(transition(fixture, fixture.fromStage,
                fixture.fromStatus, fixture.fromStage, fixture.toStatus, "RESTRICTED"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            stageRepository.delete(fixture.fromStage);
            stageRepository.flush();
        });
        assertTrue(transitionRepository.findById(saved.getId()).isPresent());
    }

        private void exec(String sql) {
                jdbcClient.sql(sql).update();
        }

        private void insertRawTransition(
                        Fixture fixture,
                        ServiceWorkflowStage fromStage,
                        ServiceWorkflowStatus fromStatus,
                        ServiceWorkflowStage toStage,
                        ServiceWorkflowStatus toStatus,
                        String code,
                        int sequence
        ) {
                jdbcClient.sql("""
                                INSERT INTO platform.service_workflow_transitions (
                                        id, workflow_version_id, from_stage_id, from_status_id,
                                        to_stage_id, to_status_id, code, display_name, sequence,
                                        active, version, created_by_principal_id,
                                        updated_by_principal_id, created_at, updated_at
                                ) VALUES (:id, :versionId, :fromStageId, :fromStatusId,
                                                  :toStageId, :toStatusId, :code, :displayName,
                                                  :sequence, TRUE, 0, :principalId, :principalId,
                                                  :now, :now)
                                """)
                                .param("id", UUID.randomUUID())
                                .param("versionId", fixture.version.getId())
                                .param("fromStageId", fromStage.getId())
                                .param("fromStatusId", fromStatus.getId())
                                .param("toStageId", toStage.getId())
                                .param("toStatusId", toStatus.getId())
                                .param("code", code)
                                .param("displayName", code)
                                .param("sequence", sequence)
                                .param("principalId", fixture.principalId)
                                .param("now", fixture.now)
                                .update();
        }

    private ServiceWorkflowTransition save(ServiceWorkflowTransition transition) {
        return transitionRepository.saveAndFlush(transition);
    }

    private void assertConstraint(Runnable action) {
        assertThrows(DataIntegrityViolationException.class, action::run);
    }

    private Fixture fixture() {
        UUID versionId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ServiceWorkflowDefinition definition = definitionRepository.saveAndFlush(
                ServiceWorkflowDefinition.create(definitionId, UUID.randomUUID(), null, null,
                        "DEF-" + definitionId, "Definition", principalId, now));
        ServiceWorkflowVersion version = versionRepository.saveAndFlush(
                ServiceWorkflowVersion.draft(versionId, definition.getId(), 1,
                        principalId, now));
        ServiceWorkflowStage fromStage = stage(version, "FROM", 1);
        ServiceWorkflowStage toStage = stage(version, "TO", 2);
        stageRepository.saveAndFlush(fromStage);
        stageRepository.saveAndFlush(toStage);
        ServiceWorkflowStatus fromStatus = status(fromStage, "FROM_STATUS", 1);
        ServiceWorkflowStatus toStatus = status(fromStage, "TO_STATUS", 2);
        statusRepository.saveAndFlush(fromStatus);
        statusRepository.saveAndFlush(toStatus);
        return new Fixture(version, fromStage, fromStatus, toStatus, principalId, now);
    }

    private ServiceWorkflowStage stage(ServiceWorkflowVersion version, String code, int sequence) {
        return ServiceWorkflowStage.create(UUID.randomUUID(), version.getId(),
                code + UUID.randomUUID(), code, sequence);
    }

    private ServiceWorkflowStatus status(ServiceWorkflowStage stage, String code, int sequence) {
        return ServiceWorkflowStatus.create(UUID.randomUUID(), stage.getId(),
                code + UUID.randomUUID(), code, sequence);
    }

    private ServiceWorkflowTransition transition(
            Fixture fixture,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStage toStage,
            ServiceWorkflowStatus toStatus,
            String code
    ) {
        return transition(fixture, fromStage, fromStatus, toStage, toStatus, code, 1);
    }

    private ServiceWorkflowTransition transition(
            Fixture fixture,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStage toStage,
            ServiceWorkflowStatus toStatus,
            String code,
            int sequence
    ) {
        return ServiceWorkflowTransition.create(UUID.randomUUID(), fixture.version.getId(),
                fromStage.getId(), fromStatus.getId(), toStage.getId(), toStatus.getId(),
                code, code, sequence, fixture.principalId, fixture.now);
    }

    private ServiceWorkflowTransition transition(
            Fixture fixture,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStage toStage,
            ServiceWorkflowStatus toStatus,
            String code,
            UUID workflowVersionId
    ) {
        return ServiceWorkflowTransition.create(UUID.randomUUID(), workflowVersionId,
                fromStage.getId(), fromStatus.getId(), toStage.getId(), toStatus.getId(),
                code, code, 1, fixture.principalId, fixture.now);
    }

    private record Fixture(
            ServiceWorkflowVersion version,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStatus toStatus,
            UUID principalId,
            OffsetDateTime now
    ) {
    }
}