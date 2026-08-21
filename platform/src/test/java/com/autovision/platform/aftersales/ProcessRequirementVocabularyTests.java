package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRequirementVocabularyTests {

    private final ProcessRequirementModePolicy policy =
            new ProcessRequirementModePolicy();

    @Test
    void processRequirementKeyAcceptsAndPreservesCanonicalValue() {
        ProcessRequirementKey key =
                new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION");

        assertEquals("SERVICE.CUSTOMER_AUTHORIZATION", key.value());
    }

    @Test
    void processRequirementKeyRejectsNullAndBlankValues() {
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementKey(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessRequirementKey("   ")
        );
    }

    @Test
    void serviceProcessRequirementKeysExposeOnlyExactDistinctValues() {
        assertEquals(
                "SERVICE.CUSTOMER_AUTHORIZATION",
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION.value()
        );
        assertEquals(
                "SERVICE.INTERNAL_APPROVAL",
                ServiceProcessRequirementKeys.INTERNAL_APPROVAL.value()
        );
        assertEquals(
                "SERVICE.QUOTE",
                ServiceProcessRequirementKeys.QUOTE.value()
        );
        assertEquals(
                "SERVICE.QUOTE_CONFIRMATION",
                ServiceProcessRequirementKeys.QUOTE_CONFIRMATION.value()
        );
        assertEquals(
                "SERVICE.JOB_CONFIRMATION",
                ServiceProcessRequirementKeys.JOB_CONFIRMATION.value()
        );

        assertEquals(
                5,
                Set.of(
                        ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                        ServiceProcessRequirementKeys.INTERNAL_APPROVAL,
                        ServiceProcessRequirementKeys.QUOTE,
                        ServiceProcessRequirementKeys.QUOTE_CONFIRMATION,
                        ServiceProcessRequirementKeys.JOB_CONFIRMATION
                ).size()
        );
    }

    @Test
    void processRequirementDefinitionRequiresKeyAndMode() {
        ProcessRequirementDefinition definition =
                new ProcessRequirementDefinition(
                        ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                        ProcessRequirementMode.OPTIONAL
                );

        assertNotNull(definition);
        assertEquals(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                definition.key()
        );
        assertEquals(ProcessRequirementMode.OPTIONAL, definition.mode());
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementDefinition(
                        null,
                        ProcessRequirementMode.REQUIRED
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementDefinition(
                        ServiceProcessRequirementKeys.QUOTE,
                        null
                )
        );
    }

    @Test
    void requiredMayBlockAndIsActionable() {
        assertTrue(policy.mayBlock(ProcessRequirementMode.REQUIRED));
        assertTrue(policy.isActionableWhenUnresolved(ProcessRequirementMode.REQUIRED));
    }

    @Test
    void optionalCannotBlockButIsActionable() {
        assertFalse(policy.mayBlock(ProcessRequirementMode.OPTIONAL));
        assertTrue(policy.isActionableWhenUnresolved(ProcessRequirementMode.OPTIONAL));
    }

    @Test
    void conditionalMayBlockAndIsActionable() {
        assertTrue(policy.mayBlock(ProcessRequirementMode.CONDITIONAL));
        assertTrue(policy.isActionableWhenUnresolved(ProcessRequirementMode.CONDITIONAL));
    }

    @Test
    void automaticCannotBlockAndIsNotActionable() {
        assertFalse(policy.mayBlock(ProcessRequirementMode.AUTOMATIC));
        assertFalse(policy.isActionableWhenUnresolved(ProcessRequirementMode.AUTOMATIC));
    }

    @Test
    void notApplicableCannotBlockAndIsNotActionable() {
        assertFalse(policy.mayBlock(ProcessRequirementMode.NOT_APPLICABLE));
        assertFalse(policy.isActionableWhenUnresolved(ProcessRequirementMode.NOT_APPLICABLE));
    }

    @Test
    void nullModeIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> policy.mayBlock(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> policy.isActionableWhenUnresolved(null)
        );
    }
}
