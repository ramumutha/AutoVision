package com.autovision.platform.aftersales;

import java.util.Objects;

public record EffectiveProcessRequirement(
        ProcessRequirementKey key,
        ProcessRequirementMode configuredMode,
        boolean applicable,
        boolean blocking,
        boolean actionable
) {
    public EffectiveProcessRequirement {
        Objects.requireNonNull(key, "Process requirement key is required");
        Objects.requireNonNull(
                configuredMode,
                "Process requirement mode is required"
        );
    }
}