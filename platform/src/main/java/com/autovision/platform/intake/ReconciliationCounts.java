package com.autovision.platform.intake;

public record ReconciliationCounts(
        int recordsReceived,
        int recordsParsed,
        int recordsStaged,
        int recordsQuarantined,
        int recordsExcluded,
        int recordsEligible,
        int recordsMapped,
        int opportunitiesDetected,
        int opportunitiesPersisted,
        int duplicateNoOpCount
) {
    public void validate() {
        if (recordsReceived < 0 || recordsParsed < 0 || recordsStaged < 0 || recordsQuarantined < 0
                || recordsExcluded < 0 || recordsEligible < 0 || recordsMapped < 0
                || opportunitiesDetected < 0 || opportunitiesPersisted < 0 || duplicateNoOpCount < 0) {
            throw new IllegalArgumentException("Reconciliation counts must not be negative");
        }
        if (recordsParsed > recordsReceived) throw new IllegalArgumentException("Parsed records exceed received records");
        if (recordsStaged + recordsQuarantined > recordsParsed) {
            throw new IllegalArgumentException("Staged and quarantined records exceed parsed records");
        }
        if (recordsEligible > recordsStaged) throw new IllegalArgumentException("Eligible records exceed staged records");
        if (opportunitiesPersisted > opportunitiesDetected) {
            throw new IllegalArgumentException("Persisted opportunities exceed detected opportunities");
        }
    }
}
