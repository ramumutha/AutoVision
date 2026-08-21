package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.EffectiveProcessRequirement;
import com.autovision.platform.aftersales.EffectiveProcessRequirementResolver;
import com.autovision.platform.aftersales.ProcessRequirementDefinition;
import com.autovision.platform.aftersales.ProcessRequirementMode;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ServiceWorkflowTransitionRequirementSource {

    private final ServiceWorkflowTransitionRequirementRepository repository;
    private final EffectiveProcessRequirementResolver resolver;

    public ServiceWorkflowTransitionRequirementSource(
            ServiceWorkflowTransitionRequirementRepository repository,
            EffectiveProcessRequirementResolver resolver
    ) {
        this.repository = Objects.requireNonNull(repository, "Requirement repository is required");
        this.resolver = Objects.requireNonNull(resolver, "Requirement resolver is required");
    }

    public List<EffectiveProcessRequirement> resolve(
            UUID workflowVersionId,
            UUID workflowTransitionId
    ) {
        Objects.requireNonNull(workflowVersionId, "Workflow version ID is required");
        Objects.requireNonNull(workflowTransitionId, "Workflow transition ID is required");

        return repository
                .findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                        workflowVersionId,
                        workflowTransitionId
                )
                .stream()
                .map(this::resolveBinding)
                .toList();
    }

    private EffectiveProcessRequirement resolveBinding(
            ServiceWorkflowTransitionRequirement binding
    ) {
        ProcessRequirementDefinition definition = new ProcessRequirementDefinition(
                binding.getRequirementKey(),
                binding.getRequirementMode()
        );
        if (definition.mode() == ProcessRequirementMode.CONDITIONAL) {
            throw new IllegalStateException(
                    "Conditional transition requirements require external condition applicability"
            );
        }
        return resolver.resolve(definition, true);
    }
}