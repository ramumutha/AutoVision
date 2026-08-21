package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitionRequirementComposerTests {

    private final TransitionRequirementComposer composer =
            new TransitionRequirementComposer();

    @Test
    void emptyListProducesEmptyComposition() {
        TransitionRequirementComposition composition = composer.compose(List.of());

        assertFalse(composition.hasBlockingRequirements());
        assertTrue(composition.blockingRequirements().isEmpty());
        assertTrue(composition.actionableRequirements().isEmpty());
    }

    @Test
    void requiredIsBlockingAndActionable() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.REQUIRED, true, true, true)),
                List.of(key("REQUIRED")),
                List.of(key("REQUIRED"))
        );
    }

    @Test
    void optionalIsActionableOnly() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.OPTIONAL, true, false, true)),
                List.of(),
                List.of(key("OPTIONAL"))
        );
    }

    @Test
    void applicableConditionalIsBlockingAndActionable() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.CONDITIONAL, true, true, true)),
                List.of(key("CONDITIONAL")),
                List.of(key("CONDITIONAL"))
        );
    }

    @Test
    void notApplicableConditionalIsInNeitherList() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.CONDITIONAL, false, false, false)),
                List.of(),
                List.of()
        );
    }

    @Test
    void automaticIsInNeitherList() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.AUTOMATIC, true, false, false)),
                List.of(),
                List.of()
        );
    }

    @Test
    void notApplicableIsInNeitherList() {
        assertComposition(
                List.of(effective(ProcessRequirementMode.NOT_APPLICABLE, false, false, false)),
                List.of(),
                List.of()
        );
    }

    @Test
    void mixtureOfAllModesComposesOnlyApplicableFlags() {
        assertComposition(
                List.of(
                        effective(ProcessRequirementMode.REQUIRED, true, true, true),
                        effective(ProcessRequirementMode.OPTIONAL, true, false, true),
                        effective(ProcessRequirementMode.CONDITIONAL, true, true, true),
                        effective(ProcessRequirementMode.CONDITIONAL, false, false, false),
                        effective(ProcessRequirementMode.AUTOMATIC, true, false, false),
                        effective(ProcessRequirementMode.NOT_APPLICABLE, false, false, false)
                ),
                List.of(key("REQUIRED"), key("CONDITIONAL")),
                List.of(key("REQUIRED"), key("OPTIONAL"), key("CONDITIONAL"))
        );
    }

    @Test
    void preservesFirstSeenOrderIndependentlyForEachList() {
        assertComposition(
                List.of(
                        effective(key("ACTIONABLE_FIRST"), ProcessRequirementMode.OPTIONAL, true, false, true),
                        effective(key("BLOCKING_FIRST"), ProcessRequirementMode.REQUIRED, true, true, true),
                        effective(key("ACTIONABLE_SECOND"), ProcessRequirementMode.OPTIONAL, true, false, true)
                ),
                List.of(key("BLOCKING_FIRST")),
                List.of(key("ACTIONABLE_FIRST"), key("BLOCKING_FIRST"), key("ACTIONABLE_SECOND"))
        );
    }

    @Test
    void deduplicatesDuplicateKeysPreservingFirstSeenOrder() {
        ProcessRequirementKey duplicate = key("DUPLICATE");

        TransitionRequirementComposition composition = composer.compose(List.of(
                effective(duplicate, ProcessRequirementMode.REQUIRED, true, true, true),
                effective(duplicate, ProcessRequirementMode.REQUIRED, true, true, true),
                effective(key("OTHER"), ProcessRequirementMode.REQUIRED, true, true, true)
        ));

        assertEquals(List.of(duplicate, key("OTHER")), composition.blockingRequirements());
        assertEquals(List.of(duplicate, key("OTHER")), composition.actionableRequirements());
    }

    @Test
    void sameKeyInBothListsIsValid() {
        TransitionRequirementComposition composition = composer.compose(List.of(
                effective(ProcessRequirementMode.REQUIRED, true, true, true)
        ));

        assertEquals(composition.blockingRequirements(), composition.actionableRequirements());
    }

    @Test
    void blockingFlagMatchesWhetherBlockingListIsEmpty() {
        TransitionRequirementComposition empty = composer.compose(List.of(
                effective(ProcessRequirementMode.OPTIONAL, true, false, true)
        ));
        TransitionRequirementComposition nonEmpty = composer.compose(List.of(
                effective(ProcessRequirementMode.REQUIRED, true, true, true)
        ));

        assertFalse(empty.hasBlockingRequirements());
        assertTrue(nonEmpty.hasBlockingRequirements());
    }

    @Test
    void nullListAndEntriesAreRejected() {
        assertThrows(NullPointerException.class, () -> composer.compose(null));
        assertThrows(
                NullPointerException.class,
                () -> composer.compose(List.of((EffectiveProcessRequirement) null))
        );
    }

    @Test
    void inputListAndRequirementsRemainUnchanged() {
        EffectiveProcessRequirement requirement =
                effective(ProcessRequirementMode.REQUIRED, true, true, true);
        List<EffectiveProcessRequirement> input = new ArrayList<>(List.of(requirement));

        composer.compose(input);

        assertEquals(List.of(requirement), input);
        assertEquals(ProcessRequirementMode.REQUIRED, requirement.configuredMode());
        assertTrue(requirement.applicable());
        assertTrue(requirement.blocking());
        assertTrue(requirement.actionable());
    }

    @Test
    void resultListsAreImmutableAndDefensive() {
        List<ProcessRequirementKey> blocking = new ArrayList<>(List.of(key("BLOCKING")));
        List<ProcessRequirementKey> actionable = new ArrayList<>(List.of(key("ACTIONABLE")));
        TransitionRequirementComposition composition =
                new TransitionRequirementComposition(true, blocking, actionable);

        blocking.add(key("LATER"));
        actionable.clear();

        assertEquals(List.of(key("BLOCKING")), composition.blockingRequirements());
        assertEquals(List.of(key("ACTIONABLE")), composition.actionableRequirements());
        assertThrows(
                UnsupportedOperationException.class,
                () -> composition.blockingRequirements().add(key("NOPE"))
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> composition.actionableRequirements().clear()
        );
    }

    @Test
    void compositionRejectsInconsistentBlockingFlagAndNullListsOrKeys() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TransitionRequirementComposition(false, List.of(key("BLOCKING")), List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new TransitionRequirementComposition(false, null, List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new TransitionRequirementComposition(false, List.of(), null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new TransitionRequirementComposition(false, List.of((ProcessRequirementKey) null), List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new TransitionRequirementComposition(false, List.of(), List.of((ProcessRequirementKey) null))
        );
    }

    private void assertComposition(
            List<EffectiveProcessRequirement> requirements,
            List<ProcessRequirementKey> expectedBlocking,
            List<ProcessRequirementKey> expectedActionable
    ) {
        TransitionRequirementComposition composition = composer.compose(requirements);

        assertEquals(expectedBlocking, composition.blockingRequirements());
        assertEquals(expectedActionable, composition.actionableRequirements());
        assertEquals(
                !expectedBlocking.isEmpty(),
                composition.hasBlockingRequirements()
        );
    }

    private EffectiveProcessRequirement effective(
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return effective(
                key(mode.name()),
                mode,
                applicable,
                blocking,
                actionable
        );
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