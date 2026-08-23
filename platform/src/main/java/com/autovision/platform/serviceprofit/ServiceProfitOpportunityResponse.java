package com.autovision.platform.serviceprofit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceProfitOpportunityResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID locationId,
        UUID customerId,
        UUID vehicleId,
        String opportunityKey,
        ServiceProfitOpportunityType opportunityType,
        ServiceProfitOpportunityStatus status,
        ServiceProfitEvidenceClass evidenceClass,
        ServiceProfitEvidenceStrength evidenceStrength,
        ServiceProfitPriority priority,
        ServiceProfitActionability actionability,
        String title,
        String summary,
        BigDecimal potentialAmount,
        String currencyCode,
        String sourceSystem,
        String sourceEntityType,
        String sourceEntityId,
        UUID sourceServiceOrderId,
        UUID sourceServiceJobId,
        UUID sourceServiceLineId,
        UUID sourceQuoteId,
        String policyVersion,
        OffsetDateTime detectedAt,
        ServiceProfitSuppressionReason suppressionReason,
        OffsetDateTime suppressedAt,
        ServiceProfitOpportunityExplanation explanation,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        ServiceProfitOpportunityContextResponse context
) {

    public static ServiceProfitOpportunityResponse from(
            ServiceProfitOpportunity opportunity
    ) {
        return from(opportunity, null);
    }

    public static ServiceProfitOpportunityResponse from(
            ServiceProfitOpportunity opportunity,
            ServiceProfitOpportunityContext context
    ) {
        return new ServiceProfitOpportunityResponse(
                opportunity.getId(),
                opportunity.getTenantId(),
                opportunity.getDealerId(),
                opportunity.getBranchId(),
                opportunity.getLocationId(),
                opportunity.getCustomerId(),
                opportunity.getVehicleId(),
                opportunity.getOpportunityKey(),
                opportunity.getOpportunityType(),
                opportunity.getStatus(),
                opportunity.getEvidenceClass(),
                opportunity.getEvidenceStrength(),
                opportunity.getPriority(),
                opportunity.getActionability(),
                opportunity.getTitle(),
                opportunity.getSummary(),
                opportunity.getPotentialAmount(),
                opportunity.getCurrencyCode(),
                opportunity.getSourceSystem(),
                opportunity.getSourceEntityType(),
                opportunity.getSourceEntityId(),
                opportunity.getSourceServiceOrderId(),
                opportunity.getSourceServiceJobId(),
                opportunity.getSourceServiceLineId(),
                opportunity.getSourceQuoteId(),
                opportunity.getPolicyVersion(),
                opportunity.getDetectedAt(),
                opportunity.getSuppressionReason(),
                opportunity.getSuppressedAt(),
                ServiceProfitOpportunityExplanationResolver.resolve(
                        opportunity
                ),
                opportunity.getVersion(),
                opportunity.getCreatedAt(),
                                opportunity.getUpdatedAt(),
                                ServiceProfitOpportunityContextResponse.from(context)
        );
    }
}