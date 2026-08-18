package com.autovision.platform.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "service_workflow_statuses", schema = "platform")
public class ServiceWorkflowStatus {

    @Id
    private UUID id;

    @Column(name = "workflow_stage_id", nullable = false)
    private UUID workflowStageId;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean active;

    protected ServiceWorkflowStatus() {
    }

    public static ServiceWorkflowStatus create(
            UUID id,
            UUID workflowStageId,
            String code,
            String displayName,
            int sequence
    ) {
        requireId(id, "Workflow status ID");
        requireId(workflowStageId, "Workflow stage ID");
        requireText(code, "Workflow status code");
        requireText(displayName, "Workflow status display name");
        requireSequence(sequence);

        ServiceWorkflowStatus status = new ServiceWorkflowStatus();

        status.id = id;
        status.workflowStageId = workflowStageId;
        status.code = code;
        status.displayName = displayName;
        status.sequence = sequence;
        status.active = true;

        return status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowStageId() {
        return workflowStageId;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSequence() {
        return sequence;
    }

    public boolean isActive() {
        return active;
    }

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

    private static void requireSequence(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Workflow status sequence must not be negative"
            );
        }
    }
}
