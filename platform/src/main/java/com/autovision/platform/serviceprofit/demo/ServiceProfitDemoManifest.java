package com.autovision.platform.serviceprofit.demo;

import java.util.UUID;

public record ServiceProfitDemoManifest(
        String datasetId,
        String version,
        String classification,
        String market,
        String defaultCurrency,
        UUID tenantId,
        String assessmentPolicyVersion,
        ServiceProfitDemoManifestCoverage expectedCoverage,
        ServiceProfitDemoManifestFeatureProfile featureProfile,
        ServiceProfitDemoManifestCapabilities expectedCapabilities
) {
}
