package com.autovision.platform.aftersales;

import java.util.List;
import java.util.Objects;

public record RuntimeTransitionDecision(
        boolean canProceed,
        List<ProcessRequirementKey> unresolvedBlockingRequirements,
        List<ProcessRequirementKey> unresolvedActionableRequirements
) {
    public RuntimeTransitionDecision {
        Objects.requireNonNull(
                unresolvedBlockingRequirements,
                "Unresolved blocking requirements are required"
        );
        Objects.requireNonNull(
                unresolvedActionableRequirements,
                "Unresolved actionable requirements are required"
        );
        unresolvedBlockingRequirements = List.copyOf(
                unresolvedBlockingRequirements
        );
        unresolvedActionableRequirements = List.copyOf(
                unresolvedActionableRequirements
        );
        if (canProceed != unresolvedBlockingRequirements.isEmpty()) {
            throw new IllegalArgumentException(
                    "Can-proceed flag must match unresolved blocking requirements"
            );
        }
    }
}