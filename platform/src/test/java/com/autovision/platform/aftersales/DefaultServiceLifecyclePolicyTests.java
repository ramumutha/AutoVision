package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultServiceLifecyclePolicyTests {

    private final ServiceLifecyclePolicy policy =
            new DefaultServiceLifecyclePolicy();

    @Test
    void allowsQuickServiceOrderToMoveDirectlyToWorkCompleted() {

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.OPEN,
                        ServiceOrderStatus.WORK_COMPLETED
                )
        );
    }

    @Test
    void allowsNormalServiceOrderExecutionPath() {

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.OPEN,
                        ServiceOrderStatus.IN_PROGRESS
                )
        );

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.IN_PROGRESS,
                        ServiceOrderStatus.WORK_COMPLETED
                )
        );

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.WORK_COMPLETED,
                        ServiceOrderStatus.CLOSED
                )
        );
    }

    @Test
    void allowsServiceOrderCancellationFromActiveStates() {

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.OPEN,
                        ServiceOrderStatus.CANCELLED
                )
        );

        assertTrue(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.IN_PROGRESS,
                        ServiceOrderStatus.CANCELLED
                )
        );
    }

    @Test
    void rejectsRepeatedAndTerminalServiceOrderTransitions() {

        assertFalse(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.OPEN,
                        ServiceOrderStatus.OPEN
                )
        );

        assertFalse(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.CLOSED,
                        ServiceOrderStatus.OPEN
                )
        );

        assertFalse(
                policy.isServiceOrderTransitionAllowed(
                        ServiceOrderStatus.CANCELLED,
                        ServiceOrderStatus.IN_PROGRESS
                )
        );
    }

    @Test
    void allowsCanonicalServiceJobExecutionPath() {

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.OPEN,
                        ServiceJobStatus.READY
                )
        );

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.READY,
                        ServiceJobStatus.IN_PROGRESS
                )
        );

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.IN_PROGRESS,
                        ServiceJobStatus.WORK_COMPLETED
                )
        );

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.WORK_COMPLETED,
                        ServiceJobStatus.CLOSED
                )
        );
    }

    @Test
    void allowsServiceJobCancellationFromActiveStates() {

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.OPEN,
                        ServiceJobStatus.CANCELLED
                )
        );

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.READY,
                        ServiceJobStatus.CANCELLED
                )
        );

        assertTrue(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.IN_PROGRESS,
                        ServiceJobStatus.CANCELLED
                )
        );
    }

    @Test
    void rejectsInvalidAndRepeatedServiceJobTransitions() {

        assertFalse(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.OPEN,
                        ServiceJobStatus.IN_PROGRESS
                )
        );

        assertFalse(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.READY,
                        ServiceJobStatus.WORK_COMPLETED
                )
        );

        assertFalse(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.OPEN,
                        ServiceJobStatus.OPEN
                )
        );

        assertFalse(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.CLOSED,
                        ServiceJobStatus.OPEN
                )
        );
    }

    @Test
    void permitsJobExecutionOnlyWhenApprovalIsSatisfied() {

        assertTrue(
                policy.isServiceJobReadyForExecution(
                        ServiceJobApprovalStatus.NOT_REQUIRED
                )
        );

        assertTrue(
                policy.isServiceJobReadyForExecution(
                        ServiceJobApprovalStatus.APPROVED
                )
        );

        assertFalse(
                policy.isServiceJobReadyForExecution(
                        ServiceJobApprovalStatus.PENDING
                )
        );

        assertFalse(
                policy.isServiceJobReadyForExecution(
                        ServiceJobApprovalStatus.DECLINED
                )
        );
    }

    @Test
    void rejectsNullLifecycleInputs() {

        assertFalse(
                policy.isServiceOrderTransitionAllowed(
                        null,
                        ServiceOrderStatus.OPEN
                )
        );

        assertFalse(
                policy.isServiceJobTransitionAllowed(
                        ServiceJobStatus.OPEN,
                        null
                )
        );

        assertFalse(
                policy.isServiceJobReadyForExecution(null)
        );
    }
}