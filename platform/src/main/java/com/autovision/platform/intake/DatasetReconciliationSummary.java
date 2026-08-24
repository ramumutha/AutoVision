package com.autovision.platform.intake;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_reconciliation_summaries", schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uq_dataset_reconciliation_summaries_dataset",
                columnNames = {"tenant_id", "dataset_processing_id"}))
public class DatasetReconciliationSummary {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "dataset_processing_id", nullable = false) private UUID datasetProcessingId;
    @Column(name = "dataset_id", nullable = false, length = 160) private String datasetId;
    @Column(name = "dataset_version", nullable = false, length = 80) private String datasetVersion;
    @Column(name = "processing_correlation_id", nullable = false) private UUID processingCorrelationId;
    @Column(name = "records_received", nullable = false) private int recordsReceived;
    @Column(name = "records_parsed", nullable = false) private int recordsParsed;
    @Column(name = "records_staged", nullable = false) private int recordsStaged;
    @Column(name = "records_quarantined", nullable = false) private int recordsQuarantined;
    @Column(name = "records_excluded", nullable = false) private int recordsExcluded;
    @Column(name = "records_eligible", nullable = false) private int recordsEligible;
    @Column(name = "records_mapped", nullable = false) private int recordsMapped;
    @Column(name = "opportunities_detected", nullable = false) private int opportunitiesDetected;
    @Column(name = "opportunities_persisted", nullable = false) private int opportunitiesPersisted;
    @Column(name = "duplicate_no_op_count", nullable = false) private int duplicateNoOpCount;
    @Column(name = "fatal_count", nullable = false) private int fatalCount;
    @Column(name = "error_count", nullable = false) private int errorCount;
    @Column(name = "warning_count", nullable = false) private int warningCount;
    @Column(name = "info_count", nullable = false) private int infoCount;
    @Column(name = "materialization_key", length = 255) private String materializationKey;
    @Column(name = "materialization_version", length = 80) private String materializationVersion;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 40) private DatasetProcessingStatus status;
    @Column(name = "observed_at", nullable = false) private OffsetDateTime observedAt;

    protected DatasetReconciliationSummary() { }

    public static DatasetReconciliationSummary record(
            UUID id, DatasetProcessing processing, ReconciliationCounts counts,
            int fatalCount, int errorCount, int warningCount, int infoCount, OffsetDateTime observedAt) {
        if (id == null || processing == null || counts == null || observedAt == null) {
            throw new IllegalArgumentException("Summary identity, dataset, counts, and observation time are required");
        }
        counts.validate();
        DatasetReconciliationSummary summary = new DatasetReconciliationSummary();
        summary.id = id;
        summary.tenantId = processing.getTenantId();
        summary.datasetProcessingId = processing.getId();
        summary.datasetId = processing.getDatasetId();
        summary.datasetVersion = processing.getDatasetVersion();
        summary.processingCorrelationId = processing.getProcessingCorrelationId();
        summary.recordsReceived = counts.recordsReceived();
        summary.recordsParsed = counts.recordsParsed();
        summary.recordsStaged = counts.recordsStaged();
        summary.recordsQuarantined = counts.recordsQuarantined();
        summary.recordsExcluded = counts.recordsExcluded();
        summary.recordsEligible = counts.recordsEligible();
        summary.recordsMapped = counts.recordsMapped();
        summary.opportunitiesDetected = counts.opportunitiesDetected();
        summary.opportunitiesPersisted = counts.opportunitiesPersisted();
        summary.duplicateNoOpCount = counts.duplicateNoOpCount();
        summary.fatalCount = nonNegative(fatalCount, "Fatal count");
        summary.errorCount = nonNegative(errorCount, "Error count");
        summary.warningCount = nonNegative(warningCount, "Warning count");
        summary.infoCount = nonNegative(infoCount, "Info count");
        summary.materializationKey = processing.getMaterializationKey();
        summary.materializationVersion = processing.getMaterializationVersion();
        summary.status = processing.getStatus();
        summary.observedAt = observedAt;
        return summary;
    }

    private static int nonNegative(int value, String label) {
        if (value < 0) throw new IllegalArgumentException(label + " must not be negative");
        return value;
    }

    public void updateMaterialization(int recordsMapped, int opportunitiesDetected, int opportunitiesPersisted,
            int duplicateNoOpCount, DatasetProcessing processing, OffsetDateTime observedAt) {
        if (processing == null || observedAt == null) throw new IllegalArgumentException("Dataset and observation time are required");
        this.recordsMapped = nonNegative(recordsMapped, "Mapped records");
        this.opportunitiesDetected = nonNegative(opportunitiesDetected, "Detected opportunities");
        this.opportunitiesPersisted = nonNegative(opportunitiesPersisted, "Persisted opportunities");
        this.duplicateNoOpCount = nonNegative(duplicateNoOpCount, "Duplicate no-op count");
        if (opportunitiesPersisted > opportunitiesDetected) throw new IllegalArgumentException("Persisted opportunities exceed detected opportunities");
        this.materializationKey = processing.getMaterializationKey();
        this.materializationVersion = processing.getMaterializationVersion();
        this.status = processing.getStatus();
        this.observedAt = observedAt;
    }

    public void updateLifecycle(DatasetProcessing processing, OffsetDateTime observedAt) {
        if (processing == null || observedAt == null) throw new IllegalArgumentException("Dataset and observation time are required");
        this.materializationKey = processing.getMaterializationKey();
        this.materializationVersion = processing.getMaterializationVersion();
        this.status = processing.getStatus();
        this.observedAt = observedAt;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDatasetProcessingId() { return datasetProcessingId; }
    public String getDatasetId() { return datasetId; }
    public String getDatasetVersion() { return datasetVersion; }
    public UUID getProcessingCorrelationId() { return processingCorrelationId; }
    public int getRecordsReceived() { return recordsReceived; }
    public int getRecordsParsed() { return recordsParsed; }
    public int getRecordsStaged() { return recordsStaged; }
    public int getRecordsQuarantined() { return recordsQuarantined; }
    public int getRecordsExcluded() { return recordsExcluded; }
    public int getRecordsEligible() { return recordsEligible; }
    public int getRecordsMapped() { return recordsMapped; }
    public int getOpportunitiesDetected() { return opportunitiesDetected; }
    public int getOpportunitiesPersisted() { return opportunitiesPersisted; }
    public int getDuplicateNoOpCount() { return duplicateNoOpCount; }
    public int getFatalCount() { return fatalCount; }
    public int getErrorCount() { return errorCount; }
    public int getWarningCount() { return warningCount; }
    public int getInfoCount() { return infoCount; }
    public String getMaterializationKey() { return materializationKey; }
    public String getMaterializationVersion() { return materializationVersion; }
    public DatasetProcessingStatus getStatus() { return status; }
    public OffsetDateTime getObservedAt() { return observedAt; }
}
