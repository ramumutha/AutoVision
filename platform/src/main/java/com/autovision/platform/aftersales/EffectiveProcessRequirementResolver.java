package com.autovision.platform.aftersales;

import java.util.Objects;

public final class EffectiveProcessRequirementResolver {

    public EffectiveProcessRequirement resolve(
            ProcessRequirementDefinition definition,
            boolean conditionApplies
    ) {
        Objects.requireNonNull(
                definition,
                "Process requirement definition is required"
        );

        ProcessRequirementMode mode = definition.mode();
        return switch (mode) {
            case REQUIRED -> effective(definition, true, true, true);
            case OPTIONAL -> effective(definition, true, false, true);
            case CONDITIONAL -> conditionApplies
                    ? effective(definition, true, true, true)
                    : effective(definition, false, false, false);
            case AUTOMATIC -> effective(definition, true, false, false);
            case NOT_APPLICABLE -> effective(definition, false, false, false);
        };
    }

    private EffectiveProcessRequirement effective(
            ProcessRequirementDefinition definition,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return new EffectiveProcessRequirement(
                definition.key(),
                definition.mode(),
                applicable,
                blocking,
                actionable
        );
    }
}