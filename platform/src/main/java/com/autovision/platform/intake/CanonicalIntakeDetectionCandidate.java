package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextSnapshot;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionInput;

public record CanonicalIntakeDetectionCandidate(
        StagedSourceRecord sourceRecord,
        ServiceProfitDetectionInput input,
        ServiceProfitOpportunityContextSnapshot context
) {
}
