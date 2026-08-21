package com.autovision.platform.aftersales;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RuntimeTransitionDecisionComposer {

    public RuntimeTransitionDecision compose(
            List<RuntimeProcessRequirementState> states
    ) {
        Objects.requireNonNull(states, "Runtime requirement states are required");

        Set<ProcessRequirementKey> unresolvedBlockingRequirements =
                new LinkedHashSet<>();
        Set<ProcessRequirementKey> unresolvedActionableRequirements =
                new LinkedHashSet<>();

        for (RuntimeProcessRequirementState state : states) {
            Objects.requireNonNull(
                    state,
                    "Runtime process requirement state is required"
            );
            if (state.unresolvedBlocking()) {
                unresolvedBlockingRequirements.add(state.key());
            }
            if (state.applicable()
                    && state.actionable()
                    && !state.satisfied()) {
                unresolvedActionableRequirements.add(state.key());
            }
        }

        return new RuntimeTransitionDecision(
                unresolvedBlockingRequirements.isEmpty(),
                new ArrayList<>(unresolvedBlockingRequirements),
                new ArrayList<>(unresolvedActionableRequirements)
        );
    }
}