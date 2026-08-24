package com.autovision.platform.intake;

public enum ValidationStage {
    ENVELOPE,
    SCHEMA,
    TYPE_FORMAT,
    REFERENTIAL_INTEGRITY,
    CONTAINMENT,
    BUSINESS_SEMANTIC,
    CAPABILITY_READINESS,
    MATERIALIZATION_ELIGIBILITY
}