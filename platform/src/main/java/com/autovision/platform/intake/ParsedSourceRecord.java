package com.autovision.platform.intake;

import tools.jackson.databind.JsonNode;

public record ParsedSourceRecord(
        StagedRecordType recordType,
        String sourceRecordId,
        String sourceParentId,
        String sourceVersion,
        JsonNode payload
) {
}