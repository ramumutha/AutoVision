package com.autovision.platform.serviceprofit.data;

public record DealerDataFeatureProfile(
        DealerDataFeatureAvailability structuredDisposition,
        DealerDataFeatureAvailability technicianAdvisorNotes,
        DealerDataFeatureAvailability recommendationHistory,
        DealerDataFeatureAvailability serviceHistory,
        DealerDataFeatureAvailability mileageHistory,
        DealerDataFeatureAvailability customerActivityHistory,
        DealerDataFeatureAvailability invoiceLinkage,
        DealerDataFeatureAvailability costData
) {

    public DealerDataFeatureProfile {
        require(structuredDisposition, "structuredDisposition");
        require(technicianAdvisorNotes, "technicianAdvisorNotes");
        require(recommendationHistory, "recommendationHistory");
        require(serviceHistory, "serviceHistory");
        require(mileageHistory, "mileageHistory");
        require(customerActivityHistory, "customerActivityHistory");
        require(invoiceLinkage, "invoiceLinkage");
        require(costData, "costData");
    }

    private static void require(
            DealerDataFeatureAvailability availability,
            String name
    ) {
        if (availability == null) {
            throw new IllegalArgumentException(
                    name + " availability is required"
            );
        }
    }
}
