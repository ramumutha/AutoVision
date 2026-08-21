package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/aftersales/service-orders/{orderId}/service-jobs/{jobId}"
)
public class ServiceJobAuthorizationReadinessController {

    private final TenantContextResolver tenantContextResolver;
    private final OperationalAuthorizationEvaluationService evaluationService;
    private final ServiceJobAuthorizationReadinessPolicy readinessPolicy;

    public ServiceJobAuthorizationReadinessController(
            TenantContextResolver tenantContextResolver,
            OperationalAuthorizationEvaluationService evaluationService,
            ServiceJobAuthorizationReadinessPolicy readinessPolicy
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.evaluationService = evaluationService;
        this.readinessPolicy = readinessPolicy;
    }

    @GetMapping("/authorization-readiness")
    public ServiceJobAuthorizationReadinessResponse evaluate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @PathVariable UUID jobId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);
        OperationalAuthorizationEvaluation evaluation =
                evaluationService.evaluate(context, orderId, jobId);
        ServiceJobAuthorizationReadiness readiness =
                readinessPolicy.evaluate(evaluation);

        return new ServiceJobAuthorizationReadinessResponse(
                readiness.serviceJobId(),
                readiness.serviceOrderId(),
                readiness.ready(),
                readiness.operationalAuthorizationStatus(),
                readiness.reason()
        );
    }
}
