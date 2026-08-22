package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.data.DealerDataFeatureAvailability;

public record ServiceProfitDemoManifestFeatureProfile(
        DealerDataFeatureAvailability structuredDisposition,
        DealerDataFeatureAvailability technicianAdvisorNotes,
        DealerDataFeatureAvailability recommendationHistory,
        DealerDataFeatureAvailability serviceHistory,
        DealerDataFeatureAvailability mileageHistory,
        DealerDataFeatureAvailability customerActivityHistory,
        DealerDataFeatureAvailability invoiceLinkage,
        DealerDataFeatureAvailability costData
) {
}
