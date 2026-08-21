package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "service_workflow_transition_requirements",
    schema = "platform",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_service_workflow_transition_requirements_transition_key",
            columnNames = {
                "workflow_transition_id",
                "requirement_key"
            }
        )
    }
)
public class ServiceWorkflowTransitionRequirement {

    @Id
    private UUID id;

    @Column(name = "workflow_version_id", nullable = false)
    private UUID workflowVersionId;

    @Column(name = "workflow_transition_id", nullable = false)
    private UUID workflowTransitionId;

    @Column(name = "requirement_key", nullable = false, length = 120)
    private String requirementKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_mode", nullable = false, length = 32)
    private ProcessRequirementMode requirementMode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    protected ServiceWorkflowTransitionRequirement() {
    }

    public static ServiceWorkflowTransitionRequirement create(
            UUID id,
            UUID workflowVersionId,
            UUID workflowTransitionId,
            ProcessRequirementKey requirementKey,
            ProcessRequirementMode requirementMode,
            OffsetDateTime createdAt,
            UUID createdByPrincipalId
    ) {
        requireId(id, "Transition requirement ID");
        requireId(workflowVersionId, "Workflow version ID");
        requireId(workflowTransitionId, "Workflow transition ID");
        if (requirementKey == null) {
            throw new IllegalArgumentException("Process requirement key is required");
        }
        if (requirementMode == null) {
            throw new IllegalArgumentException("Process requirement mode is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Transition requirement creation time is required"
            );
        }

        ServiceWorkflowTransitionRequirement requirement =
                new ServiceWorkflowTransitionRequirement();
        requirement.id = id;
        requirement.workflowVersionId = workflowVersionId;
        requirement.workflowTransitionId = workflowTransitionId;
        requirement.requirementKey = requirementKey.value();
        requirement.requirementMode = requirementMode;
        requirement.createdAt = createdAt;
        requirement.createdByPrincipalId = createdByPrincipalId;
        return requirement;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowVersionId() { return workflowVersionId; }
    public UUID getWorkflowTransitionId() { return workflowTransitionId; }
    public ProcessRequirementKey getRequirementKey() {
        return new ProcessRequirementKey(requirementKey);
    }
    public ProcessRequirementMode getRequirementMode() { return requirementMode; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }

    private static void requireId(UUID value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}