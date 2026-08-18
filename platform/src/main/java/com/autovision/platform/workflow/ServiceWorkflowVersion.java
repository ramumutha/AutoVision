package com.autovision.platform.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_workflow_versions", schema = "platform")
public class ServiceWorkflowVersion {

    @Id
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceWorkflowVersionStatus status;

    @Version
    @Column(nullable = false)
    private long lockVersion;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_by_principal_id")
    private UUID publishedByPrincipalId;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected ServiceWorkflowVersion() {
    }

    public static ServiceWorkflowVersion draft(
            UUID id,
            UUID workflowDefinitionId,
            long versionNumber,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Workflow version ID");
        requireId(workflowDefinitionId, "Workflow definition ID");
        if (versionNumber <= 0) {
            throw new IllegalArgumentException(
                    "Workflow version number must be greater than zero"
            );
        }
        if (now == null) {
            throw new IllegalArgumentException(
                    "Workflow version creation time is required"
            );
        }

        ServiceWorkflowVersion version = new ServiceWorkflowVersion();

        version.id = id;
        version.workflowDefinitionId = workflowDefinitionId;
        version.versionNumber = versionNumber;
        version.status = ServiceWorkflowVersionStatus.DRAFT;
        version.createdByPrincipalId = principalId;
        version.createdAt = now;

        return version;
    }

    public void publish(
            UUID principalId,
            OffsetDateTime now
    ) {
        if (status != ServiceWorkflowVersionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft workflow versions can be published"
            );
        }
        if (principalId == null) {
            throw new IllegalArgumentException(
                    "Publishing principal ID is required"
            );
        }
        if (now == null) {
            throw new IllegalArgumentException(
                    "Workflow version publication time is required"
            );
        }

        status = ServiceWorkflowVersionStatus.PUBLISHED;
        publishedByPrincipalId = principalId;
        publishedAt = now;
    }

    public void retire() {
        if (status != ServiceWorkflowVersionStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Only published workflow versions can be retired"
            );
        }

        status = ServiceWorkflowVersionStatus.RETIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public ServiceWorkflowVersionStatus getStatus() {
        return status;
    }

    public long getLockVersion() {
        return lockVersion;
    }

    public UUID getCreatedByPrincipalId() {
        return createdByPrincipalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getPublishedByPrincipalId() {
        return publishedByPrincipalId;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    private static void requireId(UUID value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
