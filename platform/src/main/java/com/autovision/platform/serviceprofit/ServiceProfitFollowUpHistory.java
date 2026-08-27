package com.autovision.platform.serviceprofit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_profit_follow_up_history", schema = "platform")
/** Append-only audit of AutoVision internal handling activity for a follow-up. */
public class ServiceProfitFollowUpHistory {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "follow_up_id", nullable = false)
    private UUID followUpId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ServiceProfitFollowUpHistoryActorType actorType;

    @Column(name = "actor_principal_id")
    private UUID actorPrincipalId;

    @Column(name = "application_id", length = 120)
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private ServiceProfitFollowUpEventType eventType;

    @Column(name = "previous_value", length = 80)
    private String previousValue;

    @Column(name = "new_value", length = 80)
    private String newValue;

    @Column(name = "observed_version", nullable = false)
    private long observedVersion;

    @Column(name = "written_version", nullable = false)
    private long writtenVersion;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected ServiceProfitFollowUpHistory() {
    }

    public static ServiceProfitFollowUpHistory record(
            UUID id,
            UUID tenantId,
            UUID followUpId,
            UUID opportunityId,
            UUID actorPrincipalId,
            ServiceProfitFollowUpEventType eventType,
            String previousValue,
            String newValue,
            long observedVersion,
            long writtenVersion,
            OffsetDateTime occurredAt
    ) {
            return recordHuman(
                id, tenantId, followUpId, opportunityId, actorPrincipalId,
                eventType, previousValue, newValue, observedVersion, writtenVersion, occurredAt);
            }

            public static ServiceProfitFollowUpHistory recordSystem(
                UUID id,
                UUID tenantId,
                UUID followUpId,
                UUID opportunityId,
                ServiceProfitFollowUpEventType eventType,
                String previousValue,
                String newValue,
                long observedVersion,
                long writtenVersion,
                OffsetDateTime occurredAt
            ) {
            requireCommon(
                id, tenantId, followUpId, opportunityId, eventType,
                observedVersion, writtenVersion, occurredAt);

            ServiceProfitFollowUpHistory history = new ServiceProfitFollowUpHistory();
            history.actorType = ServiceProfitFollowUpHistoryActorType.SYSTEM;
            history.applicationId = ServiceProfitFollowUpAuditApplication.AUTOVISION_SERVICE_PROFIT;
            history.assignCommon(
                id, tenantId, followUpId, opportunityId, eventType,
                previousValue, newValue, observedVersion, writtenVersion, occurredAt);
            return history;
            }

            private static ServiceProfitFollowUpHistory recordHuman(
                UUID id,
                UUID tenantId,
                UUID followUpId,
                UUID opportunityId,
                UUID actorPrincipalId,
                ServiceProfitFollowUpEventType eventType,
                String previousValue,
                String newValue,
                long observedVersion,
                long writtenVersion,
                OffsetDateTime occurredAt
            ) {
            requireCommon(
                id, tenantId, followUpId, opportunityId, eventType,
                observedVersion, writtenVersion, occurredAt);
        requireId(id, "History ID is required");
        requireId(actorPrincipalId, "History actor principal ID is required");

        ServiceProfitFollowUpHistory history = new ServiceProfitFollowUpHistory();
            history.actorType = ServiceProfitFollowUpHistoryActorType.HUMAN;
        history.actorPrincipalId = actorPrincipalId;
            history.assignCommon(
                id, tenantId, followUpId, opportunityId, eventType,
                previousValue, newValue, observedVersion, writtenVersion, occurredAt);
        return history;
    }

            private void assignCommon(
                UUID id,
                UUID tenantId,
                UUID followUpId,
                UUID opportunityId,
                ServiceProfitFollowUpEventType eventType,
                String previousValue,
                String newValue,
                long observedVersion,
                long writtenVersion,
                OffsetDateTime occurredAt
            ) {
            this.id = id;
            this.tenantId = tenantId;
            this.followUpId = followUpId;
            this.opportunityId = opportunityId;
            this.eventType = eventType;
            this.previousValue = previousValue;
            this.newValue = newValue;
            this.observedVersion = observedVersion;
            this.writtenVersion = writtenVersion;
            this.occurredAt = occurredAt;
            }

            private static void requireCommon(
                UUID id,
                UUID tenantId,
                UUID followUpId,
                UUID opportunityId,
                ServiceProfitFollowUpEventType eventType,
                long observedVersion,
                long writtenVersion,
                OffsetDateTime occurredAt
            ) {
            requireId(id, "History ID is required");
            requireId(tenantId, "History tenant ID is required");
            requireId(followUpId, "History follow-up ID is required");
            requireId(opportunityId, "History opportunity ID is required");
            if (eventType == null) throw new IllegalArgumentException("History event type is required");
            if (observedVersion < 0 || writtenVersion < 0) {
                throw new IllegalArgumentException("History versions must not be negative");
            }
            if (occurredAt == null) throw new IllegalArgumentException("History occurrence time is required");
            }

    public UUID getId() { return id; }

    public UUID getTenantId() { return tenantId; }

    public UUID getFollowUpId() { return followUpId; }

    public UUID getOpportunityId() { return opportunityId; }

    public UUID getActorPrincipalId() { return actorPrincipalId; }

    public ServiceProfitFollowUpHistoryActorType getActorType() { return actorType; }

    public String getApplicationId() { return applicationId; }

    public ServiceProfitFollowUpEventType getEventType() { return eventType; }

    public String getPreviousValue() { return previousValue; }

    public String getNewValue() { return newValue; }

    public long getObservedVersion() { return observedVersion; }

    public long getWrittenVersion() { return writtenVersion; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }

    private static void requireId(UUID value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
}