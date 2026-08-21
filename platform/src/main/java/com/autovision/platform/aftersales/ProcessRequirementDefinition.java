package com.autovision.platform.aftersales;

import java.util.Objects;

public record ProcessRequirementDefinition(
        ProcessRequirementKey key,
        ProcessRequirementMode mode
) {
    public ProcessRequirementDefinition {
        Objects.requireNonNull(key, "Process requirement key is required");
        Objects.requireNonNull(mode, "Process requirement mode is required");
    }
}
