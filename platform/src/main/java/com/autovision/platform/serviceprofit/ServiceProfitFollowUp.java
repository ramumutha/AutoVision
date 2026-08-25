package com.autovision.platform.serviceprofit;

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
        name = "service_profit_follow_ups",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_profit_follow_ups_tenant_opportunity",
                        columnNames = {"tenant_id", "opportunity_id"}
                ),
                @UniqueConstraint(
                        name = "uq_service_profit_follow_ups_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
/**
 * Durable internal work item for handling one unresolved Service Profit opportunity.
 * It is not a customer interaction, source-system disposition, service transaction,
 * authorization, or commercial outcome.
 */
public class ServiceProfitFollowUp {

    public static final int MAX_INTERNAL_NOTE_LENGTH = 500;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "owner_principal_id")
    private UUID ownerPrincipalId;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "handling_status", nullable = false, length = 32)
    private ServiceProfitFollowUpHandlingStatus handlingStatus;

    @Column(name = "next_action_due_at")
    private OffsetDateTime nextActionDueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_disposition", nullable = false, length = 40)
    private ServiceProfitFollowUpDisposition currentDisposition;

    @Column(name = "internal_note", length = MAX_INTERNAL_NOTE_LENGTH)
    private String internalNote;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceProfitFollowUp() {
    }

    public static ServiceProfitFollowUp open(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            OffsetDateTime now
    ) {
        requireId(id, "Follow-up ID is required");
        requireId(tenantId, "Follow-up tenant ID is required");
        requireId(opportunityId, "Follow-up opportunity ID is required");
        requireTime(now, "Follow-up creation time is required");

        ServiceProfitFollowUp followUp = new ServiceProfitFollowUp();
        followUp.id = id;
        followUp.tenantId = tenantId;
        followUp.opportunityId = opportunityId;
        followUp.handlingStatus = ServiceProfitFollowUpHandlingStatus.OPEN;
        followUp.currentDisposition = ServiceProfitFollowUpDisposition.NONE;
        followUp.createdAt = now;
        followUp.updatedAt = now;
        return followUp;
    }

    public UUID getId() { return id; }

    public UUID getTenantId() { return tenantId; }

    public UUID getOpportunityId() { return opportunityId; }

    public UUID getOwnerPrincipalId() { return ownerPrincipalId; }

    public OffsetDateTime getClaimedAt() { return claimedAt; }

    public ServiceProfitFollowUpHandlingStatus getHandlingStatus() { return handlingStatus; }

    public OffsetDateTime getNextActionDueAt() { return nextActionDueAt; }

    public ServiceProfitFollowUpDisposition getCurrentDisposition() { return currentDisposition; }

    public String getInternalNote() { return internalNote; }

    public long getVersion() { return version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

        void claim(UUID principalId, OffsetDateTime claimedAt) {
                requireId(principalId, "Follow-up owner principal ID is required");
                requireTime(claimedAt, "Follow-up claim time is required");
                if (ownerPrincipalId != null) {
                        throw new IllegalStateException("Follow-up is already owned");
                }
                ownerPrincipalId = principalId;
                this.claimedAt = claimedAt;
                updatedAt = claimedAt;
        }

        void changeDueAt(OffsetDateTime dueAt, OffsetDateTime changedAt) {
                requireTime(dueAt, "Next action due time is required");
                requireTime(changedAt, "Follow-up update time is required");
                nextActionDueAt = dueAt;
                updatedAt = changedAt;
        }

        void changeDisposition(ServiceProfitFollowUpDisposition disposition, OffsetDateTime changedAt) {
                if (disposition == null) throw new IllegalArgumentException("Follow-up disposition is required");
                requireTime(changedAt, "Follow-up update time is required");
                currentDisposition = disposition;
                updatedAt = changedAt;
        }

        void complete(OffsetDateTime completedAt) {
                requireTime(completedAt, "Follow-up completion time is required");
                if (handlingStatus == ServiceProfitFollowUpHandlingStatus.COMPLETED) return;
                handlingStatus = ServiceProfitFollowUpHandlingStatus.COMPLETED;
                updatedAt = completedAt;
        }

    private static void requireId(UUID value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }

    private static void requireTime(OffsetDateTime value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
}