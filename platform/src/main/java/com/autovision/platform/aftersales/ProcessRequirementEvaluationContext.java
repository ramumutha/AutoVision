package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import java.util.Objects;
import java.util.UUID;

public record ProcessRequirementEvaluationContext(
        AuthenticatedTenantContext tenantContext,
        UUID serviceOrderId,
        UUID serviceJobId
) {
    public ProcessRequirementEvaluationContext {
        Objects.requireNonNull(tenantContext, "Tenant context is required");
        Objects.requireNonNull(serviceOrderId, "Service order ID is required");
        Objects.requireNonNull(serviceJobId, "Service job ID is required");
    }
}