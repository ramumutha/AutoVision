package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeProcessRequirementStateResolverTests {

    private final RuntimeProcessRequirementStateResolver resolver =
            new RuntimeProcessRequirementStateResolver();

    @Test
    void requiredSatisfiedIsNotUnresolvedBlocking() {
        assertUnresolved(ProcessRequirementMode.REQUIRED, true, false);
    }

    @Test
    void requiredUnsatisfiedIsUnresolvedBlocking() {
        assertUnresolved(ProcessRequirementMode.REQUIRED, false, true);
    }

    @Test
    void optionalUnsatisfiedIsNotUnresolvedBlocking() {
        assertUnresolved(ProcessRequirementMode.OPTIONAL, false, false);
    }

    @Test
    void applicableConditionalUnsatisfiedIsUnresolvedBlocking() {
        assertUnresolved(
                effective(ProcessRequirementMode.CONDITIONAL, true, true, true),
                false,
                true
        );
    }

    @Test
    void nonApplicableConditionalUnsatisfiedIsNotUnresolvedBlocking() {
        assertUnresolved(
                effective(ProcessRequirementMode.CONDITIONAL, false, false, false),
                false,
                false
        );
    }

    @Test
    void automaticUnsatisfiedIsNotUnresolvedBlocking() {
        assertUnresolved(ProcessRequirementMode.AUTOMATIC, false, false);
    }

    @Test
    void notApplicableUnsatisfiedIsNotUnresolvedBlocking() {
        assertUnresolved(ProcessRequirementMode.NOT_APPLICABLE, false, false);
    }

    @Test
    void satisfiedAlwaysClearsUnresolvedBlocking() {
        for (EffectiveProcessRequirement requirement : new EffectiveProcessRequirement[]{
                effective(ProcessRequirementMode.REQUIRED, true, true, true),
                effective(ProcessRequirementMode.OPTIONAL, true, false, true),
                effective(ProcessRequirementMode.CONDITIONAL, true, true, true),
                effective(ProcessRequirementMode.CONDITIONAL, false, false, false),
                effective(ProcessRequirementMode.AUTOMATIC, true, false, false),
                effective(ProcessRequirementMode.NOT_APPLICABLE, false, false, false)
        }) {
            assertFalse(resolver.resolve(requirement, true).unresolvedBlocking());
        }
    }

    @Test
    void preservesKeyAndEffectiveFlagsAndExternalSatisfaction() {
        ProcessRequirementKey key = key("PRESERVED");
        EffectiveProcessRequirement requirement = effective(
                key,
                ProcessRequirementMode.REQUIRED,
                true,
                true,
                false
        );

        RuntimeProcessRequirementState state = resolver.resolve(requirement, false);

        assertEquals(key, state.key());
        assertTrue(state.applicable());
        assertTrue(state.blocking());
        assertFalse(state.actionable());
        assertFalse(state.satisfied());
        assertTrue(state.unresolvedBlocking());
    }

    @Test
    void nullRequirementIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> resolver.resolve(null, false)
        );
    }

    @Test
    void stateRejectsInconsistentUnresolvedBlocking() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeProcessRequirementState(
                        key("INCONSISTENT"),
                        true,
                        true,
                        true,
                        false,
                        false
                )
        );
    }

    @Test
    void sourceRequirementRemainsUnchanged() {
        EffectiveProcessRequirement requirement = effective(
                ProcessRequirementMode.REQUIRED,
                true,
                true,
                true
        );

        resolver.resolve(requirement, false);

        assertEquals(key("REQUIRED"), requirement.key());
        assertTrue(requirement.applicable());
        assertTrue(requirement.blocking());
        assertTrue(requirement.actionable());
    }

    @Test
    void stateHasImmutableValueBehavior() {
        RuntimeProcessRequirementState first = resolver.resolve(
                effective(ProcessRequirementMode.REQUIRED, true, true, true),
                false
        );
        RuntimeProcessRequirementState equal = new RuntimeProcessRequirementState(
                key("REQUIRED"),
                true,
                true,
                true,
                false,
                true
        );
        RuntimeProcessRequirementState satisfied = resolver.resolve(
                effective(ProcessRequirementMode.REQUIRED, true, true, true),
                true
        );

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, satisfied);
    }

    private void assertUnresolved(
            ProcessRequirementMode mode,
            boolean satisfied,
            boolean expectedUnresolvedBlocking
    ) {
        assertUnresolved(
                effective(mode, mode != ProcessRequirementMode.NOT_APPLICABLE, mode == ProcessRequirementMode.REQUIRED || mode == ProcessRequirementMode.CONDITIONAL, mode == ProcessRequirementMode.REQUIRED || mode == ProcessRequirementMode.OPTIONAL || mode == ProcessRequirementMode.CONDITIONAL),
                satisfied,
                expectedUnresolvedBlocking
        );
    }

    private void assertUnresolved(
            EffectiveProcessRequirement requirement,
            boolean satisfied,
            boolean expectedUnresolvedBlocking
    ) {
        RuntimeProcessRequirementState state = resolver.resolve(requirement, satisfied);

        assertEquals(expectedUnresolvedBlocking, state.unresolvedBlocking());
        assertEquals(satisfied, state.satisfied());
    }

    private EffectiveProcessRequirement effective(
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return effective(key(mode.name()), mode, applicable, blocking, actionable);
    }

    private EffectiveProcessRequirement effective(
            ProcessRequirementKey key,
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return new EffectiveProcessRequirement(
                key,
                mode,
                applicable,
                blocking,
                actionable
        );
    }

    private ProcessRequirementKey key(String value) {
        return new ProcessRequirementKey("TEST." + value);
    }
}