package com.autovision.platform.intake;

import java.util.List;

public record DatasetOperationalStatus(
        DatasetReconciliationResult reconciliation,
        List<DatasetOperationalEvent> events
) { }