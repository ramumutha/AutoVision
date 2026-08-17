package com.autovision.platform.aftersales;

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
        name = "aftersales_cases",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_aftersales_cases_tenant_case_number",
                        columnNames = {"tenant_id", "case_number"}
                ),
                @UniqueConstraint(
                        name = "uq_aftersales_cases_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class AfterSalesCase {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "case_number", nullable = false, length = 80)
    private String caseNumber;

    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private String lifecycleStatus;

    @Column(name = "source_channel", length = 32)
    private String sourceChannel;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id")
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AfterSalesCase() {
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

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
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
}
