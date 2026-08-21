package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeTransitionDecisionComposerTests {

    private final RuntimeTransitionDecisionComposer composer =
            new RuntimeTransitionDecisionComposer();

    @Test
    void emptyInputCanProceedWithNoUnresolvedRequirements() {
        RuntimeTransitionDecision decision = composer.compose(List.of());

        assertTrue(decision.canProceed());
        assertTrue(decision.unresolvedBlockingRequirements().isEmpty());
        assertTrue(decision.unresolvedActionableRequirements().isEmpty());
    }

    @Test
    void unresolvedRequiredIsBlockingAndActionable() {
        assertDecision(
                List.of(state(ProcessRequirementMode.REQUIRED, true, true, true, false, true)),
                false,
                List.of(key("REQUIRED")),
                List.of(key("REQUIRED"))
        );
    }

    @Test
    void satisfiedRequiredIsNeitherAndCanProceed() {
        assertDecision(
                List.of(state(ProcessRequirementMode.REQUIRED, true, true, true, true, false)),
                true,
                List.of(),
                List.of()
        );
    }

    @Test
    void unresolvedOptionalIsActionableOnlyAndCanProceed() {
        assertDecision(
                List.of(state(ProcessRequirementMode.OPTIONAL, true, false, true, false, false)),
                true,
                List.of(),
                List.of(key("OPTIONAL"))
        );
    }

    @Test
    void satisfiedOptionalIsNeither() {
        assertDecision(
                List.of(state(ProcessRequirementMode.OPTIONAL, true, false, true, true, false)),
                true,
                List.of(),
                List.of()
        );
    }

    @Test
    void unresolvedApplicableConditionalIsBlockingAndActionable() {
        assertDecision(
                List.of(state(ProcessRequirementMode.CONDITIONAL, true, true, true, false, true)),
                false,
                List.of(key("CONDITIONAL")),
                List.of(key("CONDITIONAL"))
        );
    }

    @Test
    void nonApplicableConditionalIsNeither() {
        assertDecision(
                List.of(state(ProcessRequirementMode.CONDITIONAL, false, false, false, false, false)),
                true,
                List.of(),
                List.of()
        );
    }

    @Test
    void unresolvedAutomaticIsNeither() {
        assertDecision(
                List.of(state(ProcessRequirementMode.AUTOMATIC, true, false, false, false, false)),
                true,
                List.of(),
                List.of()
        );
    }

    @Test
    void notApplicableIsNeither() {
        assertDecision(
                List.of(state(ProcessRequirementMode.NOT_APPLICABLE, false, false, false, false, false)),
                true,
                List.of(),
                List.of()
        );
    }

    @Test
    void mixtureIncludesBlockingAndOptionalActionableRequirements() {
        assertDecision(
                List.of(
                        state(ProcessRequirementMode.REQUIRED, true, true, true, false, true),
                        state(ProcessRequirementMode.OPTIONAL, true, false, true, false, false),
                        state(ProcessRequirementMode.AUTOMATIC, true, false, false, false, false)
                ),
                false,
                List.of(key("REQUIRED")),
                List.of(key("REQUIRED"), key("OPTIONAL"))
        );
    }

    @Test
    void canProceedMatchesWhetherBlockingListIsEmpty() {
        RuntimeTransitionDecision blocked = composer.compose(List.of(
                state(ProcessRequirementMode.REQUIRED, true, true, true, false, true)
        ));
        RuntimeTransitionDecision clear = composer.compose(List.of(
                state(ProcessRequirementMode.OPTIONAL, true, false, true, false, false)
        ));

        assertFalse(blocked.canProceed());
        assertFalse(blocked.unresolvedBlockingRequirements().isEmpty());
        assertTrue(clear.canProceed());
        assertTrue(clear.unresolvedBlockingRequirements().isEmpty());
    }

    @Test
    void preservesFirstSeenOrderIndependently() {
        assertDecision(
                List.of(
                        state(key("ACTIONABLE_FIRST"), true, false, true, false, false),
                        state(key("BLOCKING_FIRST"), true, true, true, false, true),
                        state(key("ACTIONABLE_SECOND"), true, false, true, false, false)
                ),
                false,
                List.of(key("BLOCKING_FIRST")),
                List.of(key("ACTIONABLE_FIRST"), key("BLOCKING_FIRST"), key("ACTIONABLE_SECOND"))
        );
    }

    @Test
    void deduplicatesKeysPreservingFirstSeenOrder() {
        ProcessRequirementKey duplicate = key("DUPLICATE");
        RuntimeTransitionDecision decision = composer.compose(List.of(
                state(duplicate, true, true, true, false, true),
                state(duplicate, true, true, true, false, true),
                state(key("OTHER"), true, true, true, false, true)
        ));

        assertEquals(
                List.of(duplicate, key("OTHER")),
                decision.unresolvedBlockingRequirements()
        );
        assertEquals(
                List.of(duplicate, key("OTHER")),
                decision.unresolvedActionableRequirements()
        );
    }

    @Test
    void sameKeyInBothResultListsIsValid() {
        RuntimeTransitionDecision decision = composer.compose(List.of(
                state(ProcessRequirementMode.REQUIRED, true, true, true, false, true)
        ));

        assertEquals(
                decision.unresolvedBlockingRequirements(),
                decision.unresolvedActionableRequirements()
        );
    }

    @Test
    void nullInputAndEntriesAreRejected() {
        assertThrows(NullPointerException.class, () -> composer.compose(null));
        assertThrows(
                NullPointerException.class,
                () -> composer.compose(List.of((RuntimeProcessRequirementState) null))
        );
    }

    @Test
    void sourceStateListRemainsUnchanged() {
        RuntimeProcessRequirementState state =
                state(ProcessRequirementMode.REQUIRED, true, true, true, false, true);
        List<RuntimeProcessRequirementState> states = new ArrayList<>(List.of(state));

        composer.compose(states);

        assertEquals(List.of(state), states);
        assertFalse(state.satisfied());
        assertTrue(state.unresolvedBlocking());
    }

    @Test
    void resultListsAreImmutableAndDefensive() {
        List<ProcessRequirementKey> blocking = new ArrayList<>(List.of(key("BLOCKING")));
        List<ProcessRequirementKey> actionable = new ArrayList<>(List.of(key("ACTIONABLE")));
        RuntimeTransitionDecision decision =
                new RuntimeTransitionDecision(false, blocking, actionable);

        blocking.add(key("LATER"));
        actionable.clear();

        assertEquals(List.of(key("BLOCKING")), decision.unresolvedBlockingRequirements());
        assertEquals(List.of(key("ACTIONABLE")), decision.unresolvedActionableRequirements());
        assertThrows(
                UnsupportedOperationException.class,
                () -> decision.unresolvedBlockingRequirements().add(key("NOPE"))
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> decision.unresolvedActionableRequirements().clear()
        );
    }

    @Test
    void decisionRejectsInconsistentCanProceedAndNullListsOrKeys() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeTransitionDecision(true, List.of(key("BLOCKING")), List.of())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeTransitionDecision(false, List.of(), List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new RuntimeTransitionDecision(true, null, List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new RuntimeTransitionDecision(true, List.of(), null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new RuntimeTransitionDecision(true, List.of((ProcessRequirementKey) null), List.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new RuntimeTransitionDecision(true, List.of(), List.of((ProcessRequirementKey) null))
        );
    }

    private void assertDecision(
            List<RuntimeProcessRequirementState> states,
            boolean expectedCanProceed,
            List<ProcessRequirementKey> expectedBlocking,
            List<ProcessRequirementKey> expectedActionable
    ) {
        RuntimeTransitionDecision decision = composer.compose(states);

        assertEquals(expectedCanProceed, decision.canProceed());
        assertEquals(expectedBlocking, decision.unresolvedBlockingRequirements());
        assertEquals(expectedActionable, decision.unresolvedActionableRequirements());
        assertEquals(expectedCanProceed, decision.unresolvedBlockingRequirements().isEmpty());
    }

    private RuntimeProcessRequirementState state(
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable,
            boolean satisfied,
            boolean unresolvedBlocking
    ) {
        return state(
                key(mode.name()),
                applicable,
                blocking,
                actionable,
                satisfied,
                unresolvedBlocking
        );
    }

    private RuntimeProcessRequirementState state(
            ProcessRequirementKey key,
            boolean applicable,
            boolean blocking,
            boolean actionable,
            boolean satisfied,
            boolean unresolvedBlocking
    ) {
        return new RuntimeProcessRequirementState(
                key,
                applicable,
                blocking,
                actionable,
                satisfied,
                unresolvedBlocking
        );
    }

    private ProcessRequirementKey key(String value) {
        return new ProcessRequirementKey("TEST." + value);
    }
}