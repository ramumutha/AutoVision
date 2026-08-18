package com.autovision.platform.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "service_workflow_definitions",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_workflow_definitions_tenant_scope_code",
                        columnNames = {
                                "tenant_id",
                                "dealer_id",
                                "branch_id",
                                "code"
                        }
                )
        }
)
public class ServiceWorkflowDefinition {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(nullable = false)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id")
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceWorkflowDefinition() {
    }

    public static ServiceWorkflowDefinition create(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            String code,
            String displayName,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Workflow definition ID");
        requireId(tenantId, "Tenant ID");
        if (branchId != null && dealerId == null) {
            throw new IllegalArgumentException(
                    "Dealer ID is required when branch ID is provided"
            );
        }
        requireText(code, "Workflow definition code");
        requireText(displayName, "Workflow definition display name");
        requireTime(now);

        ServiceWorkflowDefinition definition =
                new ServiceWorkflowDefinition();

        definition.id = id;
        definition.tenantId = tenantId;
        definition.dealerId = dealerId;
        definition.branchId = branchId;
        definition.code = code;
        definition.displayName = displayName;
        definition.active = true;
        definition.createdByPrincipalId = principalId;
        definition.updatedByPrincipalId = principalId;
        definition.createdAt = now;
        definition.updatedAt = now;

        return definition;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDealerId() {
        return dealerId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }

    public UUID getCreatedByPrincipalId() {
        return createdByPrincipalId;
    }

    public UUID getUpdatedByPrincipalId() {
        return updatedByPrincipalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
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

    private static void requireTime(OffsetDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Workflow definition creation time is required"
            );
        }
    }
}
