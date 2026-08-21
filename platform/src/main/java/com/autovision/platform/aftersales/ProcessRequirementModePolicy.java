package com.autovision.platform.aftersales;

import java.util.Objects;

public final class ProcessRequirementModePolicy {

    public boolean mayBlock(ProcessRequirementMode mode) {
        return switch (requireMode(mode)) {
            case REQUIRED, CONDITIONAL -> true;
            case OPTIONAL, AUTOMATIC, NOT_APPLICABLE -> false;
        };
    }

    public boolean isActionableWhenUnresolved(ProcessRequirementMode mode) {
        return switch (requireMode(mode)) {
            case REQUIRED, OPTIONAL, CONDITIONAL -> true;
            case AUTOMATIC, NOT_APPLICABLE -> false;
        };
    }

    private static ProcessRequirementMode requireMode(
            ProcessRequirementMode mode
    ) {
        return Objects.requireNonNull(
                mode,
                "Process requirement mode is required"
        );
    }
}
