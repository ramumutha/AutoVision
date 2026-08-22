package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceProfitDetectionResult(
        boolean detected,
        boolean suppressed,

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

        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID locationId,
        UUID customerId,
        UUID vehicleId,

        String sourceSystem,
        String sourceEntityType,
        String sourceEntityId,

        UUID sourceServiceOrderId,
        UUID sourceServiceJobId,
        UUID sourceServiceLineId,
        UUID sourceQuoteId,

        String policyVersion
) {

    public static ServiceProfitDetectionResult noMatch() {
        return new ServiceProfitDetectionResult(
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
