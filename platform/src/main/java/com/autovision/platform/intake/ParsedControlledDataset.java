package com.autovision.platform.intake;

import java.nio.file.Path;
import java.util.List;

public record ParsedControlledDataset(
        ControlledDatasetEnvelope envelope,
        List<ParsedSourceRecord> records,
        String contentChecksum,
        long contentByteSize,
        Path sourcePath
) {
}