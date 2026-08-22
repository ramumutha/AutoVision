package com.autovision.platform.serviceprofit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateServiceProfitOpportunityRequest(
        UUID dealerId,
        UUID branchId,
        UUID locationId,
        UUID customerId,
        UUID vehicleId,
        String opportunityKey,
        ServiceProfitOpportunityType opportunityType,
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
        OffsetDateTime detectedAt
) {
}
