package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceJobAuthorizationReadinessPolicyTests {

    private final ServiceJobAuthorizationReadinessPolicy policy =
            new ServiceJobAuthorizationReadinessPolicy();

    private final UUID serviceJobId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();

    @Test
    void mapsNotRequiredToReadyWithNotRequiredReason() {
        ServiceJobAuthorizationReadiness result = evaluate(
                OperationalAuthorizationStatus.NOT_REQUIRED,
                3,
                0,
                0,
                0
        );

        assertTrue(result.ready());
        assertEquals(
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_NOT_REQUIRED,
                result.reason()
        );
        assertPreserved(result, OperationalAuthorizationStatus.NOT_REQUIRED);
    }

    @Test
    void mapsFullyAuthorizedToReady() {
        ServiceJobAuthorizationReadiness result = evaluate(
                OperationalAuthorizationStatus.FULLY_AUTHORIZED,
                2,
                2,
                0,
                0
        );

        assertTrue(result.ready());
        assertEquals(
                ServiceJobAuthorizationReadinessReason.FULLY_AUTHORIZED,
                result.reason()
        );
        assertPreserved(result, OperationalAuthorizationStatus.FULLY_AUTHORIZED);
    }

    @Test
    void mapsPartiallyAuthorizedToNotReady() {
        ServiceJobAuthorizationReadiness result = evaluate(
                OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED,
                2,
                1,
                1,
                0
        );

        assertFalse(result.ready());
        assertEquals(
                ServiceJobAuthorizationReadinessReason.PARTIAL_AUTHORIZATION,
                result.reason()
        );
        assertPreserved(result, OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED);
    }

    @Test
    void mapsPendingToNotReady() {
        ServiceJobAuthorizationReadiness result = evaluate(
                OperationalAuthorizationStatus.PENDING,
                2,
                0,
                2,
                0
        );

        assertFalse(result.ready());
        assertEquals(
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_PENDING,
                result.reason()
        );
        assertPreserved(result, OperationalAuthorizationStatus.PENDING);
    }

    @Test
    void mapsNotAuthorizedToNotReady() {
        ServiceJobAuthorizationReadiness result = evaluate(
                OperationalAuthorizationStatus.NOT_AUTHORIZED,
                2,
                0,
                0,
                2
        );

        assertFalse(result.ready());
        assertEquals(
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_MISSING,
                result.reason()
        );
        assertPreserved(result, OperationalAuthorizationStatus.NOT_AUTHORIZED);
    }

    @Test
    void rejectsNullEvaluation() {
        assertThrows(
                NullPointerException.class,
                () -> policy.evaluate(null)
        );
    }

    @Test
    void leavesSourceEvaluationUnchanged() {
        OperationalAuthorizationEvaluation evaluation = new OperationalAuthorizationEvaluation(
                serviceJobId,
                serviceOrderId,
                OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED,
                4,
                1,
                2,
                1
        );

        policy.evaluate(evaluation);

        assertEquals(serviceJobId, evaluation.serviceJobId());
        assertEquals(serviceOrderId, evaluation.serviceOrderId());
        assertEquals(
                OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED,
                evaluation.status()
        );
        assertEquals(4, evaluation.totalLineCount());
        assertEquals(1, evaluation.authorizedLineCount());
        assertEquals(2, evaluation.pendingLineCount());
        assertEquals(1, evaluation.notAuthorizedLineCount());
    }

    private ServiceJobAuthorizationReadiness evaluate(
            OperationalAuthorizationStatus status,
            int total,
            int authorized,
            int pending,
            int notAuthorized
    ) {
        return policy.evaluate(
                new OperationalAuthorizationEvaluation(
                        serviceJobId,
                        serviceOrderId,
                        status,
                        total,
                        authorized,
                        pending,
                        notAuthorized
                )
        );
    }

    private void assertPreserved(
            ServiceJobAuthorizationReadiness result,
            OperationalAuthorizationStatus status
    ) {
        assertEquals(serviceJobId, result.serviceJobId());
        assertEquals(serviceOrderId, result.serviceOrderId());
        assertEquals(status, result.operationalAuthorizationStatus());
    }
}
