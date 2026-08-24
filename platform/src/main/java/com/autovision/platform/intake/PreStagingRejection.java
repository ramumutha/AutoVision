package com.autovision.platform.intake;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pre_staging_rejections", schema = "platform")
public class PreStagingRejection {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "dataset_processing_id", nullable = false) private UUID datasetProcessingId;
    @Enumerated(EnumType.STRING) @Column(name = "source_record_type", length = 40) private StagedRecordType sourceRecordType;
    @Column(name = "safe_source_record_id", length = 160) private String safeSourceRecordId;
    @Enumerated(EnumType.STRING) @Column(name = "validation_stage", nullable = false, length = 40) private ValidationStage validationStage;
    @Enumerated(EnumType.STRING) @Column(name = "severity", nullable = false, length = 20) private ValidationSeverity severity;
    @Column(name = "rejection_code", nullable = false, length = 100) private String rejectionCode;
    @Column(name = "safe_field_path", length = 255) private String safeFieldPath;
    @Column(name = "processing_correlation_id", nullable = false) private UUID processingCorrelationId;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;

    protected PreStagingRejection() { }

    public static PreStagingRejection record(UUID id, DatasetProcessing processing, StagedRecordType type,
            String sourceRecordId, ValidationStage stage, ValidationSeverity severity, String code,
            String fieldPath, OffsetDateTime occurredAt) {
        if (id == null || processing == null || stage == null || severity == null || code == null || code.isBlank()
                || occurredAt == null) throw new IllegalArgumentException("Complete rejection evidence is required");
        PreStagingRejection rejection = new PreStagingRejection();
        rejection.id = id;
        rejection.tenantId = processing.getTenantId();
        rejection.datasetProcessingId = processing.getId();
        rejection.sourceRecordType = type;
        rejection.safeSourceRecordId = sourceRecordId == null || sourceRecordId.isBlank() ? null : sourceRecordId.trim();
        rejection.validationStage = stage;
        rejection.severity = severity;
        rejection.rejectionCode = code.trim();
        rejection.safeFieldPath = fieldPath == null || fieldPath.isBlank() ? null : fieldPath.trim();
        rejection.processingCorrelationId = processing.getProcessingCorrelationId();
        rejection.occurredAt = occurredAt;
        return rejection;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDatasetProcessingId() { return datasetProcessingId; }
    public StagedRecordType getSourceRecordType() { return sourceRecordType; }
    public String getSafeSourceRecordId() { return safeSourceRecordId; }
    public ValidationStage getValidationStage() { return validationStage; }
    public ValidationSeverity getSeverity() { return severity; }
    public String getRejectionCode() { return rejectionCode; }
    public String getSafeFieldPath() { return safeFieldPath; }
    public UUID getProcessingCorrelationId() { return processingCorrelationId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
