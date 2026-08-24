package com.autovision.platform.intake;

public record IntakeFinding(
        ValidationStage stage,
        ValidationSeverity severity,
        IntakeValidationCode code,
        String fieldPath,
        String safeMessage,
        String sourceRecordId
) {
}