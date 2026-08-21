package com.autovision.platform.aftersales;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TransitionRequirementComposer {

    public TransitionRequirementComposition compose(
            List<EffectiveProcessRequirement> requirements
    ) {
        Objects.requireNonNull(requirements, "Requirements are required");

        Set<ProcessRequirementKey> blockingRequirements =
                new LinkedHashSet<>();
        Set<ProcessRequirementKey> actionableRequirements =
                new LinkedHashSet<>();

        for (EffectiveProcessRequirement requirement : requirements) {
            Objects.requireNonNull(
                    requirement,
                    "Effective process requirement is required"
            );
            if (!requirement.applicable()) {
                continue;
            }
            if (requirement.blocking()) {
                blockingRequirements.add(requirement.key());
            }
            if (requirement.actionable()) {
                actionableRequirements.add(requirement.key());
            }
        }

        return new TransitionRequirementComposition(
                !blockingRequirements.isEmpty(),
                new ArrayList<>(blockingRequirements),
                new ArrayList<>(actionableRequirements)
        );
    }
}