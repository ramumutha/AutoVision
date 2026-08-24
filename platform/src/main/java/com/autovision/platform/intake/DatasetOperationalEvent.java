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
@Table(name = "dataset_operational_events", schema = "platform")
public class DatasetOperationalEvent {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "dataset_processing_id", nullable = false) private UUID datasetProcessingId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 60) private DatasetOperationalEventType eventType;
    @Column(name = "materialization_key", length = 255) private String materializationKey;
    @Column(name = "materialization_version", length = 80) private String materializationVersion;
    @Column(name = "materialization_attempt") private Integer materializationAttempt;
    @Column(name = "actor_principal_id") private UUID actorPrincipalId;
    @Column(name = "application_id", length = 120) private String applicationId;
    @Column(name = "processing_correlation_id", nullable = false) private UUID processingCorrelationId;
    @Column(name = "stable_code", length = 100) private String stableCode;
    @Column(name = "safe_reason", length = 1000) private String safeReason;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;

    protected DatasetOperationalEvent() { }

    public static DatasetOperationalEvent record(UUID id, DatasetProcessing processing, DatasetOperationalEventType eventType,
            String materializationKey, String materializationVersion, Integer attempt, UUID actorPrincipalId,
            String applicationId, String stableCode, String safeReason, OffsetDateTime occurredAt) {
        if (id == null || processing == null || eventType == null || occurredAt == null
                || (actorPrincipalId == null && (applicationId == null || applicationId.isBlank()))) {
            throw new IllegalArgumentException("Event identity, actor/application, type, and time are required");
        }
        if (attempt != null && attempt < 1) throw new IllegalArgumentException("Materialization attempt must be positive");
        if ((materializationKey == null) != (materializationVersion == null)) {
            throw new IllegalArgumentException("Materialization key and version must be provided together");
        }
        DatasetOperationalEvent event = new DatasetOperationalEvent();
        event.id = id;
        event.tenantId = processing.getTenantId();
        event.datasetProcessingId = processing.getId();
        event.eventType = eventType;
        event.materializationKey = materializationKey == null ? null : materializationKey.trim();
        event.materializationVersion = materializationVersion == null ? null : materializationVersion.trim();
        event.materializationAttempt = attempt;
        event.actorPrincipalId = actorPrincipalId;
        event.applicationId = applicationId == null || applicationId.isBlank() ? null : applicationId.trim();
        event.processingCorrelationId = processing.getProcessingCorrelationId();
        event.stableCode = stableCode == null || stableCode.isBlank() ? null : stableCode.trim();
        event.safeReason = safeReason == null || safeReason.isBlank() ? null : safeReason.trim();
        event.occurredAt = occurredAt;
        return event;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDatasetProcessingId() { return datasetProcessingId; }
    public DatasetOperationalEventType getEventType() { return eventType; }
    public String getMaterializationKey() { return materializationKey; }
    public String getMaterializationVersion() { return materializationVersion; }
    public Integer getMaterializationAttempt() { return materializationAttempt; }
    public UUID getActorPrincipalId() { return actorPrincipalId; }
    public String getApplicationId() { return applicationId; }
    public UUID getProcessingCorrelationId() { return processingCorrelationId; }
    public String getStableCode() { return stableCode; }
    public String getSafeReason() { return safeReason; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
