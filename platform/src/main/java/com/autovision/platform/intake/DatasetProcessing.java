package com.autovision.platform.intake;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dataset_processings",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dataset_processings_identity",
                columnNames = {"tenant_id", "dataset_id", "dataset_version"}
        )
)
public class DatasetProcessing {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "dataset_id", nullable = false, length = 160)
    private String datasetId;

    @Column(name = "dataset_version", nullable = false, length = 80)
    private String datasetVersion;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_provider", length = 100)
    private String sourceProvider;

    @Column(name = "source_schema_version", nullable = false, length = 100)
    private String sourceSchemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    private DatasetDeliveryType deliveryType;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "checksum_algorithm", length = 30)
    private String checksumAlgorithm;

    @Column(name = "content_checksum", length = 128)
    private String contentChecksum;

    @Column(name = "content_byte_size")
    private Long contentByteSize;

    @Column(name = "mapping_version", length = 100)
    private String mappingVersion;

    @Column(name = "processing_correlation_id", nullable = false)
    private UUID processingCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DatasetProcessingStatus status;

    @Column(name = "materialization_key", length = 255)
    private String materializationKey;

    @Column(name = "materialization_version", length = 80)
    private String materializationVersion;

    @Column(name = "materialization_correlation_id")
    private UUID materializationCorrelationId;

    @Column(name = "materialization_started_at")
    private OffsetDateTime materializationStartedAt;

    @Column(name = "materialized_at")
    private OffsetDateTime materializedAt;

    @Column(name = "materialization_failed_at")
    private OffsetDateTime materializationFailedAt;

    @Column(name = "materialization_attempt_count", nullable = false)
    private int materializationAttemptCount;

    @Column(name = "materialization_failure_reason", length = 1000)
    private String materializationFailureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "materialization_failure_stage", length = 40)
    private MaterializationFailureStage materializationFailureStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "materialization_failure_code", length = 100)
    private MaterializationFailureCode materializationFailureCode;

    @Column(name = "materialization_failure_attempt")
    private Integer materializationFailureAttempt;

    @Column(name = "materialization_failure_replayable")
    private Boolean materializationFailureReplayable;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DatasetProcessing() {
    }

    public static DatasetProcessing receive(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID locationId,
            String datasetId,
            String datasetVersion,
            String sourceSystem,
            String sourceProvider,
            String sourceSchemaVersion,
            OffsetDateTime receivedAt,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo,
            String checksumAlgorithm,
            String contentChecksum,
            Long contentByteSize,
            UUID processingCorrelationId
    ) {
        requireId(id, "Dataset processing ID is required");
        requireId(tenantId, "Tenant ID is required");
        requireText(datasetId, "Dataset ID is required");
        requireText(datasetVersion, "Dataset version is required");
        requireText(sourceSystem, "Source system is required");
        requireText(sourceSchemaVersion, "Source schema version is required");
        requireTime(receivedAt, "Received time is required");
        requireId(processingCorrelationId, "Processing correlation ID is required");
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective end must not precede effective start");
        }
        if ((checksumAlgorithm == null) != (contentChecksum == null)) {
            throw new IllegalArgumentException("Checksum algorithm and checksum must be provided together");
        }
        if (contentByteSize != null && contentByteSize < 0) {
            throw new IllegalArgumentException("Content byte size must not be negative");
        }

        DatasetProcessing processing = new DatasetProcessing();
        processing.id = id;
        processing.tenantId = tenantId;
        processing.dealerId = dealerId;
        processing.locationId = locationId;
        processing.datasetId = datasetId;
        processing.datasetVersion = datasetVersion;
        processing.sourceSystem = sourceSystem;
        processing.sourceProvider = sourceProvider;
        processing.sourceSchemaVersion = sourceSchemaVersion;
        processing.deliveryType = DatasetDeliveryType.FULL;
        processing.receivedAt = receivedAt;
        processing.effectiveFrom = effectiveFrom;
        processing.effectiveTo = effectiveTo;
        processing.checksumAlgorithm = checksumAlgorithm;
        processing.contentChecksum = contentChecksum;
        processing.contentByteSize = contentByteSize;
        processing.processingCorrelationId = processingCorrelationId;
        processing.status = DatasetProcessingStatus.RECEIVED;
        processing.materializationAttemptCount = 0;
        processing.createdAt = receivedAt;
        processing.updatedAt = receivedAt;
        return processing;
    }

    public void markStaged(OffsetDateTime now) {
        requireTime(now, "Update time is required");
        if (status == DatasetProcessingStatus.RECEIVED) {
            status = DatasetProcessingStatus.STAGED;
            updatedAt = now;
        } else if (status != DatasetProcessingStatus.STAGED) {
            throw new IllegalStateException("Dataset processing is not in the expected state");
        }
    }

    public void markValidationFailed(OffsetDateTime now) {
        requireTime(now, "Update time is required");
        status = DatasetProcessingStatus.VALIDATION_FAILED;
        updatedAt = now;
    }

    public void quarantine(OffsetDateTime now) {
        requireTime(now, "Update time is required");
        status = DatasetProcessingStatus.QUARANTINED;
        updatedAt = now;
    }

    public void markReadyForMaterialization(OffsetDateTime now) {
        requireTime(now, "Update time is required");
        if (status != DatasetProcessingStatus.STAGED) {
            throw new IllegalStateException("Only staged datasets can become materialization-ready");
        }
        status = DatasetProcessingStatus.READY_FOR_MATERIALIZATION;
        updatedAt = now;
    }

    public void beginMaterialization(
            String key,
            String materializationVersion,
            UUID correlationId,
            OffsetDateTime now
    ) {
        requireText(key, "Materialization key is required");
        requireText(materializationVersion, "Materialization version is required");
        requireId(correlationId, "Materialization correlation ID is required");
        requireTime(now, "Materialization start time is required");
        if (status == DatasetProcessingStatus.MATERIALIZED) {
            if (sameMaterialization(key, materializationVersion)) return;
            throw new IllegalStateException("Dataset has already been materialized with another identity");
        }
        if (status != DatasetProcessingStatus.READY_FOR_MATERIALIZATION
                && status != DatasetProcessingStatus.MATERIALIZATION_FAILED) {
            throw new IllegalStateException("Dataset is not ready for materialization");
        }
        if (materializationKey != null && !sameMaterialization(key, materializationVersion)) {
            throw new IllegalStateException("Dataset materialization identity cannot change");
        }
        materializationKey = key.trim();
        this.materializationVersion = materializationVersion.trim();
        materializationCorrelationId = correlationId;
        materializationStartedAt = now;
        materializationFailedAt = null;
        materializationFailureReason = null;
        materializationAttemptCount++;
        status = DatasetProcessingStatus.MATERIALIZING;
        updatedAt = now;
    }

    public void markMaterialized(
            String key,
            String materializationVersion,
            OffsetDateTime now
    ) {
        requireText(key, "Materialization key is required");
        requireText(materializationVersion, "Materialization version is required");
        requireTime(now, "Materialization completion time is required");
        if (!sameMaterialization(key, materializationVersion)) {
            throw new IllegalStateException("Materialization identity does not match dataset");
        }
        if (status == DatasetProcessingStatus.MATERIALIZED) return;
        if (status != DatasetProcessingStatus.MATERIALIZING) {
            throw new IllegalStateException("Dataset is not being materialized");
        }
        status = DatasetProcessingStatus.MATERIALIZED;
        materializedAt = now;
        updatedAt = now;
    }

    public void markMaterializationFailed(
            String key,
            String materializationVersion,
            String failureReason,
            OffsetDateTime now
    ) {
        requireText(key, "Materialization key is required");
        requireText(materializationVersion, "Materialization version is required");
        requireText(failureReason, "Materialization failure reason is required");
        requireTime(now, "Materialization failure time is required");
        if (!sameMaterialization(key, materializationVersion)) {
            throw new IllegalStateException("Materialization identity does not match dataset");
        }
        if (status != DatasetProcessingStatus.MATERIALIZING) {
            throw new IllegalStateException("Dataset is not being materialized");
        }
        status = DatasetProcessingStatus.MATERIALIZATION_FAILED;
        materializationFailedAt = now;
        materializationFailureReason = failureReason.trim();
        updatedAt = now;
    }

    public void recordMaterializationFailure(
            String key,
            String materializationVersion,
            String failureReason,
            MaterializationFailureStage failureStage,
            MaterializationFailureCode failureCode,
            boolean replayable,
            OffsetDateTime now
    ) {
        if (failureStage == null || failureCode == null) {
            throw new IllegalArgumentException("Materialization failure stage and code are required");
        }
        markMaterializationFailed(key, materializationVersion, failureReason, now);
        materializationFailureStage = failureStage;
        materializationFailureCode = failureCode;
        materializationFailureAttempt = materializationAttemptCount;
        materializationFailureReplayable = replayable;
    }

    public void markAbandonedMaterializationFailed(
            String failureReason,
            MaterializationFailureStage failureStage,
            MaterializationFailureCode failureCode,
            boolean replayable,
            OffsetDateTime now
    ) {
        if (status != DatasetProcessingStatus.MATERIALIZING) {
            throw new IllegalStateException("Only materializing datasets can be recovered");
        }
        if (failureStage == null || failureCode == null) {
            throw new IllegalArgumentException("Recovery failure stage and code are required");
        }
        requireText(failureReason, "Recovery failure reason is required");
        requireTime(now, "Recovery time is required");
        status = DatasetProcessingStatus.MATERIALIZATION_FAILED;
        materializationFailedAt = now;
        materializationFailureReason = failureReason.trim();
        materializationFailureStage = failureStage;
        materializationFailureCode = failureCode;
        materializationFailureAttempt = materializationAttemptCount;
        materializationFailureReplayable = replayable;
        updatedAt = now;
    }

    private boolean sameMaterialization(String key, String version) {
        return key != null && version != null
                && key.trim().equals(materializationKey)
                && version.trim().equals(materializationVersion);
    }

    private void requireStatus(DatasetProcessingStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Dataset processing is not in the expected state");
        }
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

    private static void requireTime(OffsetDateTime value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDealerId() { return dealerId; }
    public UUID getLocationId() { return locationId; }
    public String getDatasetId() { return datasetId; }
    public String getDatasetVersion() { return datasetVersion; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSourceProvider() { return sourceProvider; }
    public String getSourceSchemaVersion() { return sourceSchemaVersion; }
    public DatasetDeliveryType getDeliveryType() { return deliveryType; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public OffsetDateTime getEffectiveTo() { return effectiveTo; }
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public String getContentChecksum() { return contentChecksum; }
    public Long getContentByteSize() { return contentByteSize; }
    public String getMappingVersion() { return mappingVersion; }
    public UUID getProcessingCorrelationId() { return processingCorrelationId; }
    public DatasetProcessingStatus getStatus() { return status; }
    public String getMaterializationKey() { return materializationKey; }
    public String getMaterializationVersion() { return materializationVersion; }
    public UUID getMaterializationCorrelationId() { return materializationCorrelationId; }
    public OffsetDateTime getMaterializationStartedAt() { return materializationStartedAt; }
    public OffsetDateTime getMaterializedAt() { return materializedAt; }
    public OffsetDateTime getMaterializationFailedAt() { return materializationFailedAt; }
    public int getMaterializationAttemptCount() { return materializationAttemptCount; }
    public String getMaterializationFailureReason() { return materializationFailureReason; }
    public MaterializationFailureStage getMaterializationFailureStage() { return materializationFailureStage; }
    public MaterializationFailureCode getMaterializationFailureCode() { return materializationFailureCode; }
    public Integer getMaterializationFailureAttempt() { return materializationFailureAttempt; }
    public Boolean getMaterializationFailureReplayable() { return materializationFailureReplayable; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}