package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.data.DealerDataCapabilityStatus;

public record ServiceProfitDemoManifestCapabilities(
        DealerDataCapabilityStatus DECLINED_WORK_EXPLICIT,
        DealerDataCapabilityStatus DECLINED_WORK_RECONSTRUCTION,
        DealerDataCapabilityStatus DEFERRED_WORK,
        DealerDataCapabilityStatus DUE_OVERDUE_SERVICE,
        DealerDataCapabilityStatus INACTIVE_CUSTOMER,
        DealerDataCapabilityStatus REVENUE_ATTRIBUTION,
        DealerDataCapabilityStatus GROSS_PROFIT_ATTRIBUTION
) {
}
