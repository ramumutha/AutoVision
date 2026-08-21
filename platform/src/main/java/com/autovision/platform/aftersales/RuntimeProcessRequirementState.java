package com.autovision.platform.aftersales;

import java.util.Objects;

public record RuntimeProcessRequirementState(
        ProcessRequirementKey key,
        boolean applicable,
        boolean blocking,
        boolean actionable,
        boolean satisfied,
        boolean unresolvedBlocking
) {
    public RuntimeProcessRequirementState {
        Objects.requireNonNull(key, "Process requirement key is required");
        if (unresolvedBlocking != (applicable && blocking && !satisfied)) {
            throw new IllegalArgumentException(
                    "Unresolved blocking must match requirement state"
            );
        }
    }
}