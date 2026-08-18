package com.autovision.platform.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
    name = "service_workflow_stages",
    schema = "platform",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_service_workflow_stages_version_code",
            columnNames = {"workflow_version_id", "code"}
        )
    }
)
public class ServiceWorkflowStage {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean active;

    protected ServiceWorkflowStage() {
    }

    public static ServiceWorkflowStage create(
            UUID id,
            UUID workflowVersionId,
            String code,
            String displayName,
            int sequence
    ) {
        requireId(id, "Workflow stage ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireText(code, "Workflow stage code");
        requireText(displayName, "Workflow stage display name");
        requireSequence(sequence);

        ServiceWorkflowStage stage = new ServiceWorkflowStage();

        stage.id = id;
        stage.workflowVersionId = workflowVersionId;
        stage.code = code;
        stage.displayName = displayName;
        stage.sequence = sequence;
        stage.active = true;

        return stage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowVersionId() {
        return workflowVersionId;
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
                    "Workflow stage sequence must not be negative"
            );
        }
    }
}
