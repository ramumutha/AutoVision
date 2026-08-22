package com.autovision.platform.serviceprofit.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dealer_data_assessments", schema = "platform")
public class DealerDataAssessment {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DealerDataAssessmentStatus status;

    @Column(name = "identity_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal identityCoverage;

    @Column(name = "vehicle_linkage_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal vehicleLinkageCoverage;

    @Column(name = "service_transaction_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceTransactionCoverage;

    @Column(name = "recommendation_evidence_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal recommendationEvidenceCoverage;

    @Column(name = "disposition_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal dispositionCoverage;

    @Column(name = "mileage_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal mileageCoverage;

    @Column(name = "invoice_linkage_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal invoiceLinkageCoverage;

    @Column(name = "cost_coverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal costCoverage;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "assessment_policy_version", nullable = false, length = 100)
    private String assessmentPolicyVersion;

    @Column(name = "assessed_at")
    private OffsetDateTime assessedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id", nullable = false)
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id", nullable = false)
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DealerDataAssessment() {
    }

    public static DealerDataAssessment createDraft(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            String sourceName,
            String sourceSystem,
            DealerDataCoverage coverage,
            BigDecimal overallScore,
            String assessmentPolicyVersion,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Dealer data assessment ID is required");
        requireId(tenantId, "Tenant ID is required");
        requireText(sourceName, "Source name is required");

        if (coverage == null) {
            throw new IllegalArgumentException(
                    "Dealer data coverage is required"
            );
        }

        requirePercentage(
                overallScore,
                "Overall score must be between 0 and 100"
        );

        requireText(
                assessmentPolicyVersion,
                "Assessment policy version is required"
        );

        requireId(principalId, "Principal ID is required");
        requireTime(now, "Assessment creation time is required");

        if (branchId != null && dealerId == null) {
            throw new IllegalArgumentException(
                    "Dealer ID is required when branch ID is provided"
            );
        }

        DealerDataAssessment assessment =
                new DealerDataAssessment();

        assessment.id = id;
        assessment.tenantId = tenantId;
        assessment.dealerId = dealerId;
        assessment.branchId = branchId;
        assessment.sourceName = sourceName;
        assessment.sourceSystem = sourceSystem;
        assessment.status = DealerDataAssessmentStatus.DRAFT;

        assessment.identityCoverage = coverage.identity();
        assessment.vehicleLinkageCoverage = coverage.vehicleLinkage();
        assessment.serviceTransactionCoverage =
                coverage.serviceTransaction();
        assessment.recommendationEvidenceCoverage =
                coverage.recommendationEvidence();
        assessment.dispositionCoverage = coverage.disposition();
        assessment.mileageCoverage = coverage.mileage();
        assessment.invoiceLinkageCoverage =
                coverage.invoiceLinkage();
        assessment.costCoverage = coverage.cost();

        assessment.overallScore = overallScore;
        assessment.assessmentPolicyVersion =
                assessmentPolicyVersion;

        assessment.createdByPrincipalId = principalId;
        assessment.updatedByPrincipalId = principalId;
        assessment.createdAt = now;
        assessment.updatedAt = now;

        return assessment;
    }

    public void complete(
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(principalId, "Principal ID is required");
        requireTime(now, "Assessment completion time is required");

        if (status == DealerDataAssessmentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Dealer data assessment is already completed"
            );
        }

        status = DealerDataAssessmentStatus.COMPLETED;
        assessedAt = now;
        updatedByPrincipalId = principalId;
        updatedAt = now;
    }

    private static void requireId(
            UUID value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(
            OffsetDateTime value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePercentage(
            BigDecimal value,
            String message
    ) {
        if (value == null ||
                value.compareTo(BigDecimal.ZERO) < 0 ||
                value.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDealerId() { return dealerId; }
    public UUID getBranchId() { return branchId; }
    public String getSourceName() { return sourceName; }
    public String getSourceSystem() { return sourceSystem; }
    public DealerDataAssessmentStatus getStatus() { return status; }
    public BigDecimal getIdentityCoverage() { return identityCoverage; }
    public BigDecimal getVehicleLinkageCoverage() { return vehicleLinkageCoverage; }
    public BigDecimal getServiceTransactionCoverage() { return serviceTransactionCoverage; }
    public BigDecimal getRecommendationEvidenceCoverage() { return recommendationEvidenceCoverage; }
    public BigDecimal getDispositionCoverage() { return dispositionCoverage; }
    public BigDecimal getMileageCoverage() { return mileageCoverage; }
    public BigDecimal getInvoiceLinkageCoverage() { return invoiceLinkageCoverage; }
    public BigDecimal getCostCoverage() { return costCoverage; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getAssessmentPolicyVersion() { return assessmentPolicyVersion; }
    public OffsetDateTime getAssessedAt() { return assessedAt; }
    public long getVersion() { return version; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }
    public UUID getUpdatedByPrincipalId() { return updatedByPrincipalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
