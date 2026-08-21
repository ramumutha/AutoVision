package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.EffectiveProcessRequirement;
import com.autovision.platform.aftersales.EffectiveProcessRequirementResolver;
import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceWorkflowTransitionRequirementSourceTests {

    private final ServiceWorkflowTransitionRequirementRepository repository =
            mock(ServiceWorkflowTransitionRequirementRepository.class);
    private final ServiceWorkflowTransitionRequirementSource source =
            new ServiceWorkflowTransitionRequirementSource(
                    repository,
                    new EffectiveProcessRequirementResolver()
            );
    private final UUID versionId = UUID.randomUUID();
    private final UUID transitionId = UUID.randomUUID();

    @Test
    void resolvesRequiredOptionalAutomaticAndNotApplicableWithoutMutation() {
        List<ServiceWorkflowTransitionRequirement> bindings = List.of(
                binding("REQUIRED", ProcessRequirementMode.REQUIRED),
                binding("OPTIONAL", ProcessRequirementMode.OPTIONAL),
                binding("AUTOMATIC", ProcessRequirementMode.AUTOMATIC),
                binding("NOT_APPLICABLE", ProcessRequirementMode.NOT_APPLICABLE)
        );
        when(repository.findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                versionId, transitionId
        )).thenReturn(bindings);

        List<EffectiveProcessRequirement> result = source.resolve(versionId, transitionId);

        assertEquals(List.of(
                effective("REQUIRED", ProcessRequirementMode.REQUIRED, true, true, true),
                effective("OPTIONAL", ProcessRequirementMode.OPTIONAL, true, false, true),
                effective("AUTOMATIC", ProcessRequirementMode.AUTOMATIC, true, false, false),
                effective("NOT_APPLICABLE", ProcessRequirementMode.NOT_APPLICABLE, false, false, false)
        ), result);
        assertEquals(ProcessRequirementMode.REQUIRED, bindings.get(0).getRequirementMode());
    }

    @Test
    void preservesEmptySetAndPersistedOrder() {
        when(repository.findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                versionId, transitionId
        )).thenReturn(List.of());

        assertEquals(List.of(), source.resolve(versionId, transitionId));
    }

    @Test
    void conditionalRequirementFailsExplicitlyWithoutSilentInterpretation() {
        when(repository.findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                versionId, transitionId
        )).thenReturn(List.of(binding("CONDITIONAL", ProcessRequirementMode.CONDITIONAL)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> source.resolve(versionId, transitionId)
        );
        assertEquals(
                "Conditional transition requirements require external condition applicability",
                exception.getMessage()
        );
    }

    @Test
    void rejectsNullLookupIds() {
        assertThrows(NullPointerException.class, () -> source.resolve(null, transitionId));
        assertThrows(NullPointerException.class, () -> source.resolve(versionId, null));
    }

    private ServiceWorkflowTransitionRequirement binding(
            String key,
            ProcessRequirementMode mode
    ) {
        return ServiceWorkflowTransitionRequirement.create(
                UUID.randomUUID(),
                versionId,
                transitionId,
                new ProcessRequirementKey(key),
                mode,
                OffsetDateTime.now(),
                UUID.randomUUID()
        );
    }

    private EffectiveProcessRequirement effective(
            String key,
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return new EffectiveProcessRequirement(
                new ProcessRequirementKey(key),
                mode,
                applicable,
                blocking,
                actionable
        );
    }
}