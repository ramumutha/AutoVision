package com.autovision.platform.serviceprofit.demo;

import java.util.UUID;

public record ServiceProfitDemoBootstrapResult(
        UUID tenantId,
        UUID managerUserRefId,
        int dealerGroups,
        int dealers,
        int branches,
        int locations
) {
}