package com.autovision.platform.serviceprofit;

import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceRef;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceSourceType;
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
        name = "service_profit_opportunity_evidence",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_profit_opportunity_evidence_identity",
                        columnNames = {
                                "tenant_id",
                                "opportunity_id",
                                "evidence_source_type",
                                "source_system",
                                "source_record_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_service_profit_opportunity_evidence_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class ServiceProfitOpportunityEvidence {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_source_type", nullable = false, length = 40)
    private ServiceProfitEvidenceSourceType evidenceSourceType;

    @Column(name = "source_system", nullable = false, length = 80)
    private String sourceSystem;

    @Column(name = "source_record_id", nullable = false, length = 200)
    private String sourceRecordId;

    @Column(name = "source_parent_record_id", length = 200)
    private String sourceParentRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_classification", nullable = false, length = 32)
    private ServiceProfitEvidenceClass evidenceClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_strength", nullable = false, length = 32)
    private ServiceProfitEvidenceStrength evidenceStrength;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ServiceProfitOpportunityEvidence() {
    }

    public static ServiceProfitOpportunityEvidence capture(
            UUID id,
            ServiceProfitOpportunity opportunity,
            ServiceProfitEvidenceRef reference,
            String sourceSystem,
            ServiceProfitEvidenceClass evidenceClassification,
            ServiceProfitEvidenceStrength evidenceStrength,
            OffsetDateTime createdAt
    ) {
        if (id == null || opportunity == null || reference == null || createdAt == null) {
            throw new IllegalArgumentException("Complete opportunity evidence is required");
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("Evidence source system is required");
        }
        if (evidenceClassification == null || evidenceStrength == null) {
            throw new IllegalArgumentException("Evidence classification and strength are required");
        }

        ServiceProfitOpportunityEvidence evidence = new ServiceProfitOpportunityEvidence();
        evidence.id = id;
        evidence.tenantId = opportunity.getTenantId();
        evidence.opportunityId = opportunity.getId();
        evidence.evidenceSourceType = reference.sourceType();
        evidence.sourceSystem = sourceSystem.trim();
        evidence.sourceRecordId = reference.sourceId();
        evidence.sourceParentRecordId = reference.sourceParentId();
        evidence.evidenceClassification = evidenceClassification;
        evidence.evidenceStrength = evidenceStrength;
        evidence.createdAt = createdAt;
        return evidence;
    }

    public UUID getId() { return id; }

    public UUID getTenantId() { return tenantId; }

    public UUID getOpportunityId() { return opportunityId; }

    public ServiceProfitEvidenceSourceType getEvidenceSourceType() { return evidenceSourceType; }

    public String getSourceSystem() { return sourceSystem; }

    public String getSourceRecordId() { return sourceRecordId; }

    public String getSourceParentRecordId() { return sourceParentRecordId; }

    public ServiceProfitEvidenceClass getEvidenceClassification() { return evidenceClassification; }

    public ServiceProfitEvidenceStrength getEvidenceStrength() { return evidenceStrength; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}