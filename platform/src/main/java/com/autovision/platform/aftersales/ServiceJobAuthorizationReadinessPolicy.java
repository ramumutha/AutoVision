package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ServiceJobAuthorizationReadinessPolicy {

    public ServiceJobAuthorizationReadiness evaluate(
            OperationalAuthorizationEvaluation evaluation
    ) {
        Objects.requireNonNull(
                evaluation,
                "Operational authorization evaluation is required"
        );

        ServiceJobAuthorizationReadinessReason reason = switch (
                evaluation.status()
        ) {
            case NOT_REQUIRED ->
                    ServiceJobAuthorizationReadinessReason
                            .AUTHORIZATION_NOT_REQUIRED;
            case FULLY_AUTHORIZED ->
                    ServiceJobAuthorizationReadinessReason.FULLY_AUTHORIZED;
            case PARTIALLY_AUTHORIZED ->
                    ServiceJobAuthorizationReadinessReason.PARTIAL_AUTHORIZATION;
            case PENDING ->
                    ServiceJobAuthorizationReadinessReason.AUTHORIZATION_PENDING;
            case NOT_AUTHORIZED ->
                    ServiceJobAuthorizationReadinessReason.AUTHORIZATION_MISSING;
        };

        boolean ready = evaluation.status() ==
                OperationalAuthorizationStatus.NOT_REQUIRED
                || evaluation.status() ==
                OperationalAuthorizationStatus.FULLY_AUTHORIZED;

        return new ServiceJobAuthorizationReadiness(
                evaluation.serviceJobId(),
                evaluation.serviceOrderId(),
                ready,
                evaluation.status(),
                reason
        );
    }
}
