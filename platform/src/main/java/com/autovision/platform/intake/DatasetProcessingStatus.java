package com.autovision.platform.intake;

public enum DatasetProcessingStatus {
    RECEIVED,
    STAGED,
    READY_FOR_MATERIALIZATION,
    MATERIALIZING,
    MATERIALIZED,
    MATERIALIZATION_FAILED,
    VALIDATION_FAILED,
    QUARANTINED
}