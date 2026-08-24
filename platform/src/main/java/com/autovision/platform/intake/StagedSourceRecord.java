package com.autovision.platform.intake;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "staged_source_records",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_staged_source_records_identity",
                columnNames = {"tenant_id", "dataset_processing_id", "record_type", "source_record_id"}
        )
)
public class StagedSourceRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dataset_processing_id", nullable = false)
    private UUID datasetProcessingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 40)
    private StagedRecordType recordType;

    @Column(name = "source_record_id", nullable = false, length = 160)
    private String sourceRecordId;

    @Column(name = "source_parent_id", length = 160)
    private String sourceParentId;

    @Column(name = "source_version", length = 80)
    private String sourceVersion;

    @Column(name = "source_hash", length = 128)
    private String sourceHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private JsonNode rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private StagedRecordValidationStatus validationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private StagedRecordState state;

    @Column(name = "materialization_eligible", nullable = false)
    private boolean materializationEligible;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StagedSourceRecord() {
    }

    public static StagedSourceRecord stage(
            UUID id,
            UUID tenantId,
            UUID datasetProcessingId,
            StagedRecordType recordType,
            String sourceRecordId,
            String sourceParentId,
            String sourceVersion,
            String sourceHash,
            JsonNode rawPayload,
            OffsetDateTime createdAt
    ) {
        requireId(id, "Staged record ID is required");
        requireId(tenantId, "Tenant ID is required");
        requireId(datasetProcessingId, "Dataset processing ID is required");
        if (recordType == null) {
            throw new IllegalArgumentException("Record type is required");
        }
        if (sourceRecordId == null || sourceRecordId.isBlank()) {
            throw new IllegalArgumentException("Source record ID is required");
        }
        if (rawPayload != null && !rawPayload.isObject()) {
            throw new IllegalArgumentException("Raw payload must be a JSON object");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        StagedSourceRecord record = new StagedSourceRecord();
        record.id = id;
        record.tenantId = tenantId;
        record.datasetProcessingId = datasetProcessingId;
        record.recordType = recordType;
        record.sourceRecordId = sourceRecordId;
        record.sourceParentId = sourceParentId;
        record.sourceVersion = sourceVersion;
        record.sourceHash = sourceHash;
        record.rawPayload = rawPayload;
        record.validationStatus = StagedRecordValidationStatus.PENDING;
        record.state = StagedRecordState.STAGED;
        record.materializationEligible = false;
        record.createdAt = createdAt;
        return record;
    }

    public void markValidationPassed() {
        requireStaged();
        validationStatus = StagedRecordValidationStatus.PASSED;
        materializationEligible = true;
    }

    public void markValidationFailed() {
        validationStatus = StagedRecordValidationStatus.FAILED;
    }

    public void quarantine() {
        state = StagedRecordState.QUARANTINED;
        materializationEligible = false;
    }

    private void requireStaged() {
        if (state != StagedRecordState.STAGED) {
            throw new IllegalStateException("Only staged records can be validated");
        }
    }

    private static void requireId(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDatasetProcessingId() { return datasetProcessingId; }
    public StagedRecordType getRecordType() { return recordType; }
    public String getSourceRecordId() { return sourceRecordId; }
    public String getSourceParentId() { return sourceParentId; }
    public String getSourceVersion() { return sourceVersion; }
    public String getSourceHash() { return sourceHash; }
    public JsonNode getRawPayload() { return rawPayload; }
    public StagedRecordValidationStatus getValidationStatus() { return validationStatus; }
    public StagedRecordState getState() { return state; }
    public boolean isMaterializationEligible() { return materializationEligible; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}