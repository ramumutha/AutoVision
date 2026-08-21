package com.autovision.platform.aftersales;

import java.util.Objects;

public final class RuntimeProcessRequirementStateResolver {

    public RuntimeProcessRequirementState resolve(
            EffectiveProcessRequirement requirement,
            boolean satisfied
    ) {
        Objects.requireNonNull(
                requirement,
                "Effective process requirement is required"
        );

        return new RuntimeProcessRequirementState(
                requirement.key(),
                requirement.applicable(),
                requirement.blocking(),
                requirement.actionable(),
                satisfied,
                requirement.applicable()
                        && requirement.blocking()
                        && !satisfied
        );
    }
}