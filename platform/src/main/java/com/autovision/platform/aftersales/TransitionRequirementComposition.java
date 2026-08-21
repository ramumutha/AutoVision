package com.autovision.platform.aftersales;

import java.util.List;
import java.util.Objects;

public record TransitionRequirementComposition(
        boolean hasBlockingRequirements,
        List<ProcessRequirementKey> blockingRequirements,
        List<ProcessRequirementKey> actionableRequirements
) {
    public TransitionRequirementComposition {
        Objects.requireNonNull(
                blockingRequirements,
                "Blocking requirements are required"
        );
        Objects.requireNonNull(
                actionableRequirements,
                "Actionable requirements are required"
        );
        blockingRequirements = List.copyOf(blockingRequirements);
        actionableRequirements = List.copyOf(actionableRequirements);
        if (hasBlockingRequirements != !blockingRequirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "Blocking requirement flag must match blocking requirements"
            );
        }
    }
}