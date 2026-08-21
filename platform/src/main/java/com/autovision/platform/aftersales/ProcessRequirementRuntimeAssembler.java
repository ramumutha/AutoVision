package com.autovision.platform.aftersales;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProcessRequirementRuntimeAssembler {

    private final ProcessRequirementSatisfactionProviderRegistry providerRegistry;
    private final RuntimeProcessRequirementStateResolver stateResolver;
    private final RuntimeTransitionDecisionComposer decisionComposer;

    public ProcessRequirementRuntimeAssembler(
            ProcessRequirementSatisfactionProviderRegistry providerRegistry,
            RuntimeProcessRequirementStateResolver stateResolver,
            RuntimeTransitionDecisionComposer decisionComposer
    ) {
        this.providerRegistry = Objects.requireNonNull(
                providerRegistry,
                "Provider registry is required"
        );
        this.stateResolver = Objects.requireNonNull(
                stateResolver,
                "State resolver is required"
        );
        this.decisionComposer = Objects.requireNonNull(
                decisionComposer,
                "Decision composer is required"
        );
    }

    public ProcessRequirementRuntimeAssembly assemble(
            List<EffectiveProcessRequirement> requirements,
            ProcessRequirementEvaluationContext context
    ) {
        Objects.requireNonNull(requirements, "Requirements are required");
        Objects.requireNonNull(context, "Evaluation context is required");

        List<RuntimeProcessRequirementState> states = new ArrayList<>(
                requirements.size()
        );
        for (EffectiveProcessRequirement requirement : requirements) {
            Objects.requireNonNull(
                    requirement,
                    "Effective process requirement is required"
            );
            boolean satisfied = true;
            if (requirement.applicable()) {
                ProcessRequirementSatisfactionProvider provider =
                        providerRegistry.require(requirement.key());
                satisfied = provider.isSatisfied(context);
            }
            states.add(stateResolver.resolve(requirement, satisfied));
        }

        RuntimeTransitionDecision decision = decisionComposer.compose(states);
        return new ProcessRequirementRuntimeAssembly(states, decision);
    }
}