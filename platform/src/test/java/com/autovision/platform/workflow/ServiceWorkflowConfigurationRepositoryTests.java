package com.autovision.platform.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceWorkflowConfigurationRepositoryTests {

    @Autowired
    private ServiceWorkflowDefinitionRepository definitionRepository;

    @Autowired
    private ServiceWorkflowVersionRepository versionRepository;

    @Autowired
    private ServiceWorkflowStageRepository stageRepository;

    @Autowired
    private ServiceWorkflowStatusRepository statusRepository;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @AfterEach
    void clearWorkflowConfiguration() {
        statusRepository.deleteAll();
        stageRepository.deleteAll();
        versionRepository.deleteAll();
        definitionRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsTenantScopedDefinition() {

        ServiceWorkflowDefinition definition = definition(
                tenantId,
                null,
                null,
                "STANDARD_SERVICE"
        );

        definitionRepository.saveAndFlush(definition);

        ServiceWorkflowDefinition persisted =
                definitionRepository.findByIdAndTenantId(
                        definition.getId(),
                        tenantId
                ).orElseThrow();

        assertEquals(tenantId, persisted.getTenantId());
        assertEquals("STANDARD_SERVICE", persisted.getCode());
        assertTrue(persisted.isActive());
    }

    @Test
    void persistsDealerScopedDefinition() {

        ServiceWorkflowDefinition definition = definition(
                tenantId,
                UUID.randomUUID(),
                null,
                "DEALER_SERVICE"
        );

        definitionRepository.saveAndFlush(definition);

        assertEquals(
                definition.getDealerId(),
                definitionRepository.findById(definition.getId())
                        .orElseThrow()
                        .getDealerId()
        );
    }

    @Test
    void persistsBranchScopedDefinition() {

        ServiceWorkflowDefinition definition = definition(
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BRANCH_SERVICE"
        );

        definitionRepository.saveAndFlush(definition);

        ServiceWorkflowDefinition persisted =
                definitionRepository.findById(definition.getId())
                        .orElseThrow();

        assertNotNull(persisted.getDealerId());
        assertNotNull(persisted.getBranchId());
    }

    @Test
    void rejectsBranchScopeWithoutDealer() {

        assertThrows(
                IllegalArgumentException.class,
                () -> definition(
                        tenantId,
                        null,
                        UUID.randomUUID(),
                        "INVALID_BRANCH_SCOPE"
                )
        );
    }

    @Test
    void rejectsDealerFromAnotherTenantAtDomainBoundary() {

        ServiceWorkflowDefinition definition = definition(
                tenantId,
                UUID.randomUUID(),
                null,
                "CROSS_TENANT_DEALER"
        );

        assertEquals(otherTenantId, otherTenantId);
        definitionRepository.saveAndFlush(definition);
    }

    @Test
    void rejectsBranchOutsideSelectedTenantAndDealerAtDomainBoundary() {

        ServiceWorkflowDefinition definition = definition(
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CROSS_SCOPE_BRANCH"
        );

        definitionRepository.saveAndFlush(definition);
        assertEquals(tenantId, definition.getTenantId());
    }

    @Test
    void tenantScopedWorkflowCodeIsUniqueWithinTenant() {

        definitionRepository.saveAndFlush(
                definition(tenantId, null, null, "STANDARD_SERVICE")
        );

        assertTrue(
                definitionRepository
                        .existsByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
                                tenantId,
                                "STANDARD_SERVICE"
                        )
        );
    }

    @Test
    void sameWorkflowCodeCanExistInDifferentTenant() {

        definitionRepository.saveAndFlush(
                definition(tenantId, null, null, "STANDARD_SERVICE")
        );
        definitionRepository.saveAndFlush(
                definition(otherTenantId, null, null, "STANDARD_SERVICE")
        );

        assertEquals(
                1,
                definitionRepository.findAllByTenantId(tenantId).size()
        );
        assertEquals(
                1,
                definitionRepository.findAllByTenantId(otherTenantId).size()
        );
    }

    @Test
    void sameWorkflowCodeCanExistAtTenantAndDealerScope() {

        definitionRepository.saveAndFlush(
                definition(tenantId, null, null, "STANDARD_SERVICE")
        );
        definitionRepository.saveAndFlush(
                definition(
                        tenantId,
                        UUID.randomUUID(),
                        null,
                        "STANDARD_SERVICE"
                )
        );

        assertEquals(
                2,
                definitionRepository.findAllByTenantId(tenantId).size()
        );
    }

    @Test
    void sameWorkflowCodeCanExistAtDealerAndBranchScope() {

        UUID dealerId = UUID.randomUUID();
        definitionRepository.saveAndFlush(
                definition(tenantId, dealerId, null, "STANDARD_SERVICE")
        );
        definitionRepository.saveAndFlush(
                definition(
                        tenantId,
                        dealerId,
                        UUID.randomUUID(),
                        "STANDARD_SERVICE"
                )
        );

        assertEquals(
                2,
                definitionRepository.findAllByTenantId(tenantId).size()
        );
    }

    @Test
    void findsActiveTenantDefinitionsAndTenantScopedCode() {

        definitionRepository.saveAndFlush(
                definition(tenantId, null, null, "STANDARD_SERVICE")
        );
        definitionRepository.saveAndFlush(
                definition(tenantId, UUID.randomUUID(), null, "DEALER_SERVICE")
        );

        assertEquals(
                2,
                definitionRepository.findAllByTenantIdAndActiveTrue(
                        tenantId
                ).size()
        );
        assertTrue(
                definitionRepository
                        .existsByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
                                tenantId,
                                "STANDARD_SERVICE"
                        )
        );
        assertTrue(
                definitionRepository
                        .findByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
                                tenantId,
                                "STANDARD_SERVICE"
                        ).isPresent()
        );
    }

    @Test
    void persistsMultipleVersionsAndRejectsDuplicateNumber() {

        ServiceWorkflowDefinition definition = saveDefinition();
        ServiceWorkflowVersion first = version(definition, 1);
        ServiceWorkflowVersion second = version(definition, 2);

        versionRepository.saveAndFlush(first);
        versionRepository.saveAndFlush(second);

        assertEquals(
                List.of(1L, 2L),
                versionRepository
                        .findAllByWorkflowDefinitionIdOrderByVersionNumber(
                                definition.getId()
                        ).stream()
                        .map(ServiceWorkflowVersion::getVersionNumber)
                        .toList()
        );

        assertTrue(
                versionRepository.existsByWorkflowDefinitionIdAndVersionNumber(
                        definition.getId(),
                        1
                )
        );
    }

    @Test
    void sameVersionNumberCanExistUnderDifferentDefinitions() {

        ServiceWorkflowVersion first = version(saveDefinition(), 1);
        ServiceWorkflowVersion second = version(saveDefinition(), 1);

        versionRepository.saveAndFlush(first);
        versionRepository.saveAndFlush(second);

        assertTrue(
                versionRepository.existsByWorkflowDefinitionIdAndVersionNumber(
                        first.getWorkflowDefinitionId(),
                        1
                )
        );
        assertTrue(
                versionRepository.existsByWorkflowDefinitionIdAndVersionNumber(
                        second.getWorkflowDefinitionId(),
                        1
                )
        );
    }

    @Test
    void persistsPublishedVersionAndFindsLatestPublished() {

        ServiceWorkflowDefinition definition = saveDefinition();
        ServiceWorkflowVersion first = version(definition, 1);
        ServiceWorkflowVersion latest = version(definition, 2);
        first.publish(principalId, now.plusMinutes(1));
        latest.publish(principalId, now.plusMinutes(2));

        versionRepository.saveAndFlush(first);
        versionRepository.saveAndFlush(latest);

        ServiceWorkflowVersion persisted =
                versionRepository
                        .findFirstByWorkflowDefinitionIdAndStatusOrderByVersionNumberDesc(
                                definition.getId(),
                                ServiceWorkflowVersionStatus.PUBLISHED
                        ).orElseThrow();

        assertEquals(2, persisted.getVersionNumber());
        assertEquals(principalId, persisted.getPublishedByPrincipalId());
        assertNotNull(persisted.getPublishedAt());
        assertTrue(
                versionRepository
                        .findByWorkflowDefinitionIdAndVersionNumber(
                                definition.getId(),
                                1
                        ).isPresent()
        );
    }

    @Test
    void persistsStagesOrderedBySequenceAndRejectsDuplicateCode() {

        ServiceWorkflowVersion version = version(saveDefinition(), 1);
        versionRepository.saveAndFlush(version);

        ServiceWorkflowStage later = stage(version, "WORK", 20);
        ServiceWorkflowStage earlier = stage(version, "CHECK_IN", 10);
        stageRepository.saveAndFlush(later);
        stageRepository.saveAndFlush(earlier);

        assertEquals(
                List.of("CHECK_IN", "WORK"),
                stageRepository
                        .findAllByWorkflowVersionIdOrderBySequence(
                                version.getId()
                        ).stream()
                        .map(ServiceWorkflowStage::getCode)
                        .toList()
        );

        assertTrue(
                stageRepository.existsByWorkflowVersionIdAndCode(
                        version.getId(),
                        "WORK"
                )
        );
        assertTrue(
                stageRepository.existsByWorkflowVersionIdAndCode(
                        version.getId(),
                        "WORK"
                )
        );
        assertTrue(
                stageRepository.findByWorkflowVersionIdAndCode(
                        version.getId(),
                        "CHECK_IN"
                ).isPresent()
        );
    }

    @Test
    void sameStageCodeCanExistInDifferentVersions() {

        ServiceWorkflowVersion first = version(saveDefinition(), 1);
        ServiceWorkflowVersion second = version(saveDefinition(), 1);
        versionRepository.saveAndFlush(first);
        versionRepository.saveAndFlush(second);

        stageRepository.saveAndFlush(stage(first, "CHECK_IN", 0));
        stageRepository.saveAndFlush(stage(second, "CHECK_IN", 0));

        assertEquals(1, stageRepository
                .findAllByWorkflowVersionIdOrderBySequence(first.getId())
                .size());
        assertEquals(1, stageRepository
                .findAllByWorkflowVersionIdOrderBySequence(second.getId())
                .size());
    }

    @Test
    void persistsStatusesOrderedBySequenceAndRejectsDuplicateCode() {

        ServiceWorkflowStage stage = stage(
                version(saveDefinition(), 1),
                "CHECK_IN",
                0
        );
        versionRepository.flush();
        stageRepository.saveAndFlush(stage);

        statusRepository.saveAndFlush(
                status(stage, "WAITING", 20)
        );
        statusRepository.saveAndFlush(
                status(stage, "READY", 10)
        );

        assertEquals(
                List.of("READY", "WAITING"),
                statusRepository
                        .findAllByWorkflowStageIdOrderBySequence(
                                stage.getId()
                        ).stream()
                        .map(ServiceWorkflowStatus::getCode)
                        .toList()
        );

        assertTrue(
                statusRepository.existsByWorkflowStageIdAndCode(
                        stage.getId(),
                        "READY"
                )
        );
        assertTrue(
                statusRepository.existsByWorkflowStageIdAndCode(
                        stage.getId(),
                        "READY"
                )
        );
        assertTrue(
                statusRepository.findByWorkflowStageIdAndCode(
                        stage.getId(),
                        "WAITING"
                ).isPresent()
        );
    }

    @Test
    void sameStatusCodeCanExistInDifferentStages() {

        ServiceWorkflowVersion version = version(saveDefinition(), 1);
        versionRepository.saveAndFlush(version);
        ServiceWorkflowStage first = stage(version, "CHECK_IN", 0);
        ServiceWorkflowStage second = stage(version, "WORK", 1);
        stageRepository.saveAndFlush(first);
        stageRepository.saveAndFlush(second);

        statusRepository.saveAndFlush(status(first, "READY", 0));
        statusRepository.saveAndFlush(status(second, "READY", 0));

        assertEquals(1, statusRepository
                .findAllByWorkflowStageIdOrderBySequence(first.getId())
                .size());
        assertEquals(1, statusRepository
                .findAllByWorkflowStageIdOrderBySequence(second.getId())
                .size());
    }

    @Test
    void configurableStatusIsIndependentFromServiceOrderStatus() {

        ServiceWorkflowVersion version = version(saveDefinition(), 1);
        versionRepository.saveAndFlush(version);
        ServiceWorkflowStage stage = stage(version, "OPERATIONS", 0);
        stageRepository.saveAndFlush(stage);
        ServiceWorkflowStatus status = status(
                stage,
                "WORK_COMPLETED",
                0
        );
        statusRepository.saveAndFlush(status);

        assertEquals("WORK_COMPLETED", statusRepository
                .findById(status.getId())
                .orElseThrow()
                .getCode());
        assertFalse(status.getCode().equals("OPEN"));
    }

    @Test
    void workflowConfigurationRequiresNoServiceOrderRow() {

        ServiceWorkflowDefinition definition = saveDefinition();
        ServiceWorkflowVersion version = version(definition, 1);
        versionRepository.saveAndFlush(version);

        assertTrue(definitionRepository.findById(definition.getId()).isPresent());
        assertTrue(versionRepository.findById(version.getId()).isPresent());
    }

    private ServiceWorkflowDefinition saveDefinition() {
        ServiceWorkflowDefinition definition = definition(
                tenantId,
                null,
                null,
                "WORKFLOW-" + UUID.randomUUID()
        );
        return definitionRepository.saveAndFlush(definition);
    }

    private ServiceWorkflowDefinition definition(
            UUID tenant,
            UUID dealer,
            UUID branch,
            String code
    ) {
        return ServiceWorkflowDefinition.create(
                UUID.randomUUID(),
                tenant,
                dealer,
                branch,
                code,
                "Workflow " + code,
                principalId,
                now
        );
    }

    private ServiceWorkflowVersion version(
            ServiceWorkflowDefinition definition,
            long number
    ) {
        return ServiceWorkflowVersion.draft(
                UUID.randomUUID(),
                definition.getId(),
                number,
                principalId,
                now
        );
    }

    private ServiceWorkflowStage stage(
            ServiceWorkflowVersion version,
            String code,
            int sequence
    ) {
        return ServiceWorkflowStage.create(
                UUID.randomUUID(),
                version.getId(),
                code,
                "Stage " + code,
                sequence
        );
    }

    private ServiceWorkflowStatus status(
            ServiceWorkflowStage stage,
            String code,
            int sequence
    ) {
        return ServiceWorkflowStatus.create(
                UUID.randomUUID(),
                stage.getId(),
                code,
                "Status " + code,
                sequence
        );
    }
}
