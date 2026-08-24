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
@Table(name = "validation_findings", schema = "platform")
public class ValidationFinding {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dataset_processing_id", nullable = false)
    private UUID datasetProcessingId;

    @Column(name = "staged_source_record_id")
    private UUID stagedSourceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_stage", nullable = false, length = 40)
    private ValidationStage validationStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ValidationSeverity severity;

    @Column(name = "finding_code", nullable = false, length = 100)
    private String findingCode;

    @Column(name = "field_path", length = 255)
    private String fieldPath;

    @Column(name = "safe_message", nullable = false, length = 1000)
    private String safeMessage;

    @Column(name = "capability_affected", length = 100)
    private String capabilityAffected;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ValidationFinding() {
    }

    public static ValidationFinding record(
            UUID id,
            UUID tenantId,
            UUID datasetProcessingId,
            UUID stagedSourceRecordId,
            ValidationStage validationStage,
            ValidationSeverity severity,
            String findingCode,
            String fieldPath,
            String safeMessage,
            String capabilityAffected,
            OffsetDateTime createdAt
    ) {
        requireId(id, "Validation finding ID is required");
        requireId(tenantId, "Tenant ID is required");
        requireId(datasetProcessingId, "Dataset processing ID is required");
        if (validationStage == null || severity == null) {
            throw new IllegalArgumentException("Validation stage and severity are required");
        }
        requireText(findingCode, "Finding code is required");
        requireText(safeMessage, "Safe message is required");
        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        ValidationFinding finding = new ValidationFinding();
        finding.id = id;
        finding.tenantId = tenantId;
        finding.datasetProcessingId = datasetProcessingId;
        finding.stagedSourceRecordId = stagedSourceRecordId;
        finding.validationStage = validationStage;
        finding.severity = severity;
        finding.findingCode = findingCode;
        finding.fieldPath = fieldPath;
        finding.safeMessage = safeMessage;
        finding.capabilityAffected = capabilityAffected;
        finding.createdAt = createdAt;
        return finding;
    }

    private static void requireId(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDatasetProcessingId() { return datasetProcessingId; }
    public UUID getStagedSourceRecordId() { return stagedSourceRecordId; }
    public ValidationStage getValidationStage() { return validationStage; }
    public ValidationSeverity getSeverity() { return severity; }
    public String getFindingCode() { return findingCode; }
    public String getFieldPath() { return fieldPath; }
    public String getSafeMessage() { return safeMessage; }
    public String getCapabilityAffected() { return capabilityAffected; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}