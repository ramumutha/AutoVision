package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class CustomerAuthorizationRequirementSatisfactionProvider
        implements ProcessRequirementSatisfactionProvider {

    private final OperationalAuthorizationEvaluationService evaluationService;
    private final ServiceJobAuthorizationReadinessPolicy readinessPolicy;

    public CustomerAuthorizationRequirementSatisfactionProvider(
            OperationalAuthorizationEvaluationService evaluationService,
            ServiceJobAuthorizationReadinessPolicy readinessPolicy
    ) {
        this.evaluationService = Objects.requireNonNull(
                evaluationService,
                "Operational authorization evaluation service is required"
        );
        this.readinessPolicy = Objects.requireNonNull(
                readinessPolicy,
                "Service job authorization readiness policy is required"
        );
    }

    @Override
    public ProcessRequirementKey key() {
        return ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION;
    }

    @Override
    public boolean isSatisfied(ProcessRequirementEvaluationContext context) {
        Objects.requireNonNull(context, "Evaluation context is required");
        AuthenticatedTenantContext tenantContext = context.tenantContext();
        OperationalAuthorizationEvaluation evaluation = evaluationService.evaluate(
                tenantContext,
                context.serviceOrderId(),
                context.serviceJobId()
        );
        return readinessPolicy.evaluate(evaluation).ready();
    }
}