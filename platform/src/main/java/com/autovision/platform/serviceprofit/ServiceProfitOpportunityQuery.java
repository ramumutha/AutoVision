package com.autovision.platform.serviceprofit;

import java.util.UUID;

public record ServiceProfitOpportunityQuery(
        ServiceProfitOpportunityStatus status,
        ServiceProfitPriority priority,
        ServiceProfitOpportunityType opportunityType,
        ServiceProfitEvidenceClass evidenceClass,
        ServiceProfitEvidenceStrength evidenceStrength,
        ServiceProfitActionability actionability,
        UUID dealerId,
        UUID branchId,
        UUID locationId,
        int page,
        int size,
        ServiceProfitOpportunitySort sort
) {

    public ServiceProfitOpportunityQuery {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to zero"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }

        if (sort == null) {
            sort = ServiceProfitOpportunitySort.DETECTED_DESC;
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
