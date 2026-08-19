package com.autovision.platform.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_workflow_transitions", schema = "platform")
public class ServiceWorkflowTransition {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "from_stage_id", nullable = false)
    private UUID fromStageId;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @Column(name = "to_stage_id", nullable = false)
    private UUID toStageId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by_principal_id", nullable = false)
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id", nullable = false)
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceWorkflowTransition() {
    }

    public static ServiceWorkflowTransition create(
            UUID id,
            UUID workflowVersionId,
            UUID fromStageId,
            UUID fromStatusId,
            UUID toStageId,
            UUID toStatusId,
            String code,
            String displayName,
            int sequence,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Workflow transition ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireId(fromStageId, "From workflow stage ID");
        requireId(fromStatusId, "From workflow status ID");
        requireId(toStageId, "To workflow stage ID");
        requireId(toStatusId, "To workflow status ID");
        requireText(code, "Workflow transition code");
        requireText(displayName, "Workflow transition display name");
        if (sequence < 0) {
            throw new IllegalArgumentException(
                    "Workflow transition sequence must not be negative");
        }
        requireId(principalId, "Principal ID");
        if (now == null) {
            throw new IllegalArgumentException(
                    "Workflow transition creation time is required");
        }
        if (fromStageId.equals(toStageId) && fromStatusId.equals(toStatusId)) {
            throw new IllegalArgumentException(
                    "Workflow transition must change stage or status");
        }

        ServiceWorkflowTransition transition = new ServiceWorkflowTransition();
        transition.id = id;
        transition.workflowVersionId = workflowVersionId;
        transition.fromStageId = fromStageId;
        transition.fromStatusId = fromStatusId;
        transition.toStageId = toStageId;
        transition.toStatusId = toStatusId;
        transition.code = code;
        transition.displayName = displayName;
        transition.sequence = sequence;
        transition.active = true;
        transition.createdByPrincipalId = principalId;
        transition.updatedByPrincipalId = principalId;
        transition.createdAt = now;
        transition.updatedAt = now;
        return transition;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowVersionId() { return workflowVersionId; }
    public UUID getFromStageId() { return fromStageId; }
    public UUID getFromStatusId() { return fromStatusId; }
    public UUID getToStageId() { return toStageId; }
    public UUID getToStatusId() { return toStatusId; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public int getSequence() { return sequence; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }
    public UUID getUpdatedByPrincipalId() { return updatedByPrincipalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    private static void requireId(UUID value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}