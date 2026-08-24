package com.autovision.platform.intake;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ControlledDatasetEnvelope(
        String contractId,
        String contractVersion,
        String datasetId,
        String datasetVersion,
        UUID tenantId,
        UUID dealerId,
        UUID locationId,
        String sourceSystem,
        String sourceProvider,
        String sourceSchemaVersion,
        String deliveryType,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        String mappingVersion,
        Map<String, String> recordFiles
) {
}