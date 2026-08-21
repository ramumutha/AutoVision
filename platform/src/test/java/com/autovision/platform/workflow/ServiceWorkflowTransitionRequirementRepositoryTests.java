package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ServiceWorkflowTransitionRequirementRepositoryTests {

    @Autowired private ServiceWorkflowTransitionRequirementRepository repository;
    @Autowired private ServiceWorkflowTransitionRepository transitionRepository;
    @Autowired private ServiceWorkflowStatusRepository statusRepository;
    @Autowired private ServiceWorkflowStageRepository stageRepository;
    @Autowired private ServiceWorkflowVersionRepository versionRepository;
    @Autowired private ServiceWorkflowDefinitionRepository definitionRepository;

    @AfterEach
    void clearData() {
        repository.deleteAll();
        transitionRepository.deleteAll();
        statusRepository.deleteAll();
        stageRepository.deleteAll();
        versionRepository.deleteAll();
        definitionRepository.deleteAll();
    }

    @Test
    void persistsModesAndReturnsDeterministicCreatedOrder() {
        Fixture fixture = fixture();
        ServiceWorkflowTransitionRequirement later = save(
                binding(fixture, "SERVICE.QUOTE", ProcessRequirementMode.OPTIONAL, 2)
        );
        ServiceWorkflowTransitionRequirement earlier = save(
                binding(fixture, "SERVICE.CUSTOMER_AUTHORIZATION", ProcessRequirementMode.REQUIRED, 1)
        );

        List<ServiceWorkflowTransitionRequirement> result = repository
                .findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                        fixture.version.getId(), fixture.transition.getId()
                );

        assertEquals(2, result.size());
        assertEquals("SERVICE.CUSTOMER_AUTHORIZATION", result.get(0).getRequirementKey().value());
        assertEquals(ProcessRequirementMode.REQUIRED, result.get(0).getRequirementMode());
        assertEquals("SERVICE.QUOTE", result.get(1).getRequirementKey().value());
        assertEquals(ProcessRequirementMode.OPTIONAL, result.get(1).getRequirementMode());
        assertEquals(earlier.getId(), result.get(0).getId());
        assertEquals(later.getId(), result.get(1).getId());
    }

    @Test
        void duplicateKeyOnSameTransitionIsRejected() {
        Fixture fixture = fixture();
        save(binding(fixture, "SERVICE.QUOTE", ProcessRequirementMode.REQUIRED, 1));

        assertThrows(DataIntegrityViolationException.class, () -> save(
                binding(fixture, "SERVICE.QUOTE", ProcessRequirementMode.OPTIONAL, 2)
        ));
        }

        @Test
        void sameKeyOnAnotherTransitionIsAllowed() {
                Fixture fixture = fixture();
                save(binding(fixture, "SERVICE.QUOTE", ProcessRequirementMode.REQUIRED, 1));
        ServiceWorkflowStage secondToStage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(
                        UUID.randomUUID(),
                        fixture.version.getId(),
                        "SECOND_TO",
                        "Second to",
                        3
                )
        );
        ServiceWorkflowStatus secondToStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(
                        UUID.randomUUID(),
                        secondToStage.getId(),
                        "SECOND_READY",
                        "Second ready",
                        1
                )
        );
        ServiceWorkflowTransition secondTransition = transition(
                fixture.version,
                fixture.fromStage,
                fixture.fromStatus,
                secondToStage,
                secondToStatus,
                "SECOND"
        );
        transitionRepository.saveAndFlush(secondTransition);
        ServiceWorkflowTransitionRequirement second =
                ServiceWorkflowTransitionRequirement.create(
                        UUID.randomUUID(),
                        fixture.version.getId(),
                        secondTransition.getId(),
                        new ProcessRequirementKey("SERVICE.QUOTE"),
                        ProcessRequirementMode.REQUIRED,
                        OffsetDateTime.now(),
                        fixture.principalId
                );
        ServiceWorkflowTransitionRequirement saved = repository.saveAndFlush(second);
        assertEquals(saved.getId(), repository
                .findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                        fixture.version.getId(), secondTransition.getId()
                ).get(0).getId());
    }

    private ServiceWorkflowTransitionRequirement save(
            ServiceWorkflowTransitionRequirement requirement
    ) {
        return repository.saveAndFlush(requirement);
    }

    private ServiceWorkflowTransitionRequirement binding(
            Fixture fixture,
            String key,
            ProcessRequirementMode mode,
            int seconds
    ) {
        return ServiceWorkflowTransitionRequirement.create(
                UUID.randomUUID(),
                fixture.version.getId(),
                fixture.transition.getId(),
                new ProcessRequirementKey(key),
                mode,
                fixture.now.plusSeconds(seconds),
                fixture.principalId
        );
    }

    private Fixture fixture() {
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ServiceWorkflowDefinition definition = definitionRepository.saveAndFlush(
                ServiceWorkflowDefinition.create(
                        UUID.randomUUID(), UUID.randomUUID(), null, null,
                        "DEF-" + UUID.randomUUID(), "Definition", principalId, now
                )
        );
        ServiceWorkflowVersion version = versionRepository.saveAndFlush(
                ServiceWorkflowVersion.draft(
                        UUID.randomUUID(), definition.getId(), 1, principalId, now
                )
        );
        ServiceWorkflowStage fromStage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(
                        UUID.randomUUID(), version.getId(), "FROM", "From", 1
                )
        );
        ServiceWorkflowStage toStage = stageRepository.saveAndFlush(
                ServiceWorkflowStage.create(
                        UUID.randomUUID(), version.getId(), "TO", "To", 2
                )
        );
        ServiceWorkflowStatus fromStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(
                        UUID.randomUUID(), fromStage.getId(), "OPEN", "Open", 1
                )
        );
        ServiceWorkflowStatus toStatus = statusRepository.saveAndFlush(
                ServiceWorkflowStatus.create(
                        UUID.randomUUID(), toStage.getId(), "READY", "Ready", 1
                )
        );
        ServiceWorkflowTransition transition = transition(
                version, fromStage, fromStatus, toStage, toStatus, "FIRST"
        );
        transitionRepository.saveAndFlush(transition);
        return new Fixture(version, fromStage, fromStatus, toStatus, transition, principalId, now);
    }

    private ServiceWorkflowTransition transition(
            ServiceWorkflowVersion version,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStage toStage,
            ServiceWorkflowStatus toStatus,
            String code
    ) {
        return ServiceWorkflowTransition.create(
                UUID.randomUUID(), version.getId(), fromStage.getId(), fromStatus.getId(),
                toStage.getId(), toStatus.getId(), code, code, 1,
                UUID.randomUUID(), OffsetDateTime.now()
        );
    }

    private record Fixture(
            ServiceWorkflowVersion version,
            ServiceWorkflowStage fromStage,
            ServiceWorkflowStatus fromStatus,
            ServiceWorkflowStatus toStatus,
            ServiceWorkflowTransition transition,
            UUID principalId,
            OffsetDateTime now
    ) {
    }
}