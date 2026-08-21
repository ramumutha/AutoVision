package com.autovision.platform.aftersales;

import java.util.List;
import java.util.Objects;

public record ProcessRequirementRuntimeAssembly(
        List<RuntimeProcessRequirementState> states,
        RuntimeTransitionDecision decision
) {
    public ProcessRequirementRuntimeAssembly {
        Objects.requireNonNull(states, "Runtime requirement states are required");
        Objects.requireNonNull(decision, "Runtime transition decision is required");
        states = List.copyOf(states);
    }
}