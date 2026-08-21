package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveProcessRequirementResolverTests {

    private final EffectiveProcessRequirementResolver resolver =
            new EffectiveProcessRequirementResolver();

    @Test
    void requiredIsApplicableBlockingAndActionable() {
        assertOutcome(ProcessRequirementMode.REQUIRED, true, true, true, true);
    }

    @Test
    void optionalIsApplicableActionableAndNonBlocking() {
                assertOutcome(ProcessRequirementMode.OPTIONAL, true, true, false, true);
    }

    @Test
    void conditionalIsApplicableBlockingAndActionableWhenConditionApplies() {
        assertOutcome(ProcessRequirementMode.CONDITIONAL, true, true, true, true);
    }

    @Test
    void conditionalIsNotApplicableNonBlockingAndNotActionableWhenConditionDoesNotApply() {
        assertOutcome(ProcessRequirementMode.CONDITIONAL, false, false, false, false);
    }

    @Test
    void automaticIsApplicableNonBlockingAndNotActionable() {
        assertOutcome(ProcessRequirementMode.AUTOMATIC, true, true, false, false);
    }

    @Test
    void notApplicableIsNotApplicableNonBlockingAndNotActionable() {
        assertOutcome(
                ProcessRequirementMode.NOT_APPLICABLE,
                true,
                false,
                false,
                false
        );
    }

    @Test
    void conditionFlagIsIgnoredForNonConditionalModes() {
        for (ProcessRequirementMode mode : new ProcessRequirementMode[]{
                ProcessRequirementMode.REQUIRED,
                ProcessRequirementMode.OPTIONAL,
                ProcessRequirementMode.AUTOMATIC,
                ProcessRequirementMode.NOT_APPLICABLE
        }) {
            assertEquals(
                    resolver.resolve(definition(mode), false),
                    resolver.resolve(definition(mode), true)
            );
        }
    }

    @Test
    void keyAndConfiguredModeArePreserved() {
        ProcessRequirementKey key =
                new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION");
        ProcessRequirementDefinition definition =
                new ProcessRequirementDefinition(key, ProcessRequirementMode.CONDITIONAL);

        EffectiveProcessRequirement effective = resolver.resolve(definition, true);

        assertEquals(key, effective.key());
        assertEquals(ProcessRequirementMode.CONDITIONAL, effective.configuredMode());
    }

    @Test
    void nullDefinitionIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> resolver.resolve(null, true)
        );
    }

    @Test
    void sourceDefinitionRemainsUnchanged() {
        ProcessRequirementDefinition definition = definition(
                ProcessRequirementMode.CONDITIONAL
        );

        resolver.resolve(definition, false);

        assertEquals(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                definition.key()
        );
        assertEquals(ProcessRequirementMode.CONDITIONAL, definition.mode());
    }

    @Test
    void effectiveRequirementRejectsNullKeyAndMode() {
        assertThrows(
                NullPointerException.class,
                () -> new EffectiveProcessRequirement(
                        null,
                        ProcessRequirementMode.REQUIRED,
                        true,
                        true,
                        true
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new EffectiveProcessRequirement(
                        ServiceProcessRequirementKeys.QUOTE,
                        null,
                        true,
                        true,
                        true
                )
        );
    }

    private void assertOutcome(
            ProcessRequirementMode mode,
            boolean conditionApplies,
            boolean expectedApplicable,
            boolean expectedBlocking,
            boolean expectedActionable
    ) {
        EffectiveProcessRequirement effective = resolver.resolve(
                definition(mode),
                conditionApplies
        );

        assertEquals(expectedApplicable, effective.applicable());
        assertEquals(expectedBlocking, effective.blocking());
        assertEquals(expectedActionable, effective.actionable());
        assertTrue(effective.key() != null);
        assertFalse(effective.configuredMode() == null);
    }

    private ProcessRequirementDefinition definition(ProcessRequirementMode mode) {
        return new ProcessRequirementDefinition(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                mode
        );
    }
}