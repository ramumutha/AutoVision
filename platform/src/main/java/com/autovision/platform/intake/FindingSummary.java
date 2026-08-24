package com.autovision.platform.intake;

import java.util.Map;

public record FindingSummary(
        Map<ValidationSeverity, Integer> bySeverity,
        Map<ValidationStage, Integer> byStage,
        Map<String, Integer> byCode,
        Map<String, Integer> byCapability,
        int preStagingRejectionCount
) { }