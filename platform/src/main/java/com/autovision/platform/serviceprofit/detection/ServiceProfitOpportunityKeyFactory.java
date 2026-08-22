package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import org.springframework.stereotype.Component;

@Component
public class ServiceProfitOpportunityKeyFactory {

    public String create(
            ServiceProfitDetectionInput input,
            ServiceProfitOpportunityType opportunityType
    ) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "Detection input is required"
            );
        }

        if (opportunityType == null) {
            throw new IllegalArgumentException(
                    "Opportunity type is required"
            );
        }

        return String.join(
                ":",
                "SP",
                opportunityType.name(),
                input.tenantId().toString(),
                input.sourceSystem(),
                input.sourceEntityType(),
                input.sourceEntityId()
        );
    }
}
