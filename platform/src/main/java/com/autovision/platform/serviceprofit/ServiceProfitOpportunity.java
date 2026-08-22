package com.autovision.platform.serviceprofit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "service_profit_opportunities",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_profit_opportunities_tenant_key",
                        columnNames = {"tenant_id", "opportunity_key"}
                ),
                @UniqueConstraint(
                        name = "uq_service_profit_opportunities_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class ServiceProfitOpportunity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "opportunity_key", nullable = false, length = 200)
    private String opportunityKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "opportunity_type", nullable = false, length = 40)
    private ServiceProfitOpportunityType opportunityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ServiceProfitOpportunityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_class", nullable = false, length = 32)
    private ServiceProfitEvidenceClass evidenceClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_strength", nullable = false, length = 32)
    private ServiceProfitEvidenceStrength evidenceStrength;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private ServiceProfitPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "actionability", nullable = false, length = 40)
    private ServiceProfitActionability actionability;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "potential_amount", precision = 19, scale = 4)
    private BigDecimal potentialAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "source_system", nullable = false, length = 80)
    private String sourceSystem;

    @Column(name = "source_entity_type", nullable = false, length = 80)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false, length = 200)
    private String sourceEntityId;

    @Column(name = "source_service_order_id")
    private UUID sourceServiceOrderId;

    @Column(name = "source_service_job_id")
    private UUID sourceServiceJobId;

    @Column(name = "source_service_line_id")
    private UUID sourceServiceLineId;

    @Column(name = "source_quote_id")
    private UUID sourceQuoteId;

    @Column(name = "policy_version", nullable = false, length = 80)
    private String policyVersion;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

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

    protected ServiceProfitOpportunity() {
    }

    public static ServiceProfitOpportunity detect(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            UUID customerId,
            UUID vehicleId,
            String opportunityKey,
            ServiceProfitOpportunityType opportunityType,
            ServiceProfitEvidenceClass evidenceClass,
            ServiceProfitEvidenceStrength evidenceStrength,
            ServiceProfitPriority priority,
            ServiceProfitActionability actionability,
            String title,
            String summary,
            BigDecimal potentialAmount,
            String currencyCode,
            String sourceSystem,
            String sourceEntityType,
            String sourceEntityId,
            UUID sourceServiceOrderId,
            UUID sourceServiceJobId,
            UUID sourceServiceLineId,
            UUID sourceQuoteId,
            String policyVersion,
            UUID principalId,
            OffsetDateTime detectedAt
    ) {
        requireId(id);
        requireId(tenantId, "Service profit opportunity tenant ID is required");
        requireText(
                opportunityKey,
                "Service profit opportunity key is required"
        );
        requireValue(
                opportunityType,
                "Service profit opportunity type is required"
        );
        requireValue(
                evidenceClass,
                "Service profit evidence class is required"
        );
        requireValue(
                evidenceStrength,
                "Service profit evidence strength is required"
        );
        requireValue(
                priority,
                "Service profit priority is required"
        );
        requireValue(
                actionability,
                "Service profit actionability is required"
        );
        requireText(
                title,
                "Service profit opportunity title is required"
        );
        requireText(
                sourceSystem,
                "Service profit source system is required"
        );
        requireText(
                sourceEntityType,
                "Service profit source entity type is required"
        );
        requireText(
                sourceEntityId,
                "Service profit source entity ID is required"
        );
        requireText(
                policyVersion,
                "Service profit policy version is required"
        );
        requirePotentialValue(potentialAmount, currencyCode);

        if (detectedAt == null) {
            throw new IllegalArgumentException(
                    "Service profit opportunity detection time is required"
            );
        }

        ServiceProfitOpportunity opportunity =
                new ServiceProfitOpportunity();

        opportunity.id = id;
        opportunity.tenantId = tenantId;
        opportunity.dealerId = dealerId;
        opportunity.branchId = branchId;
        opportunity.locationId = locationId;
        opportunity.customerId = customerId;
        opportunity.vehicleId = vehicleId;
        opportunity.opportunityKey = opportunityKey.trim();
        opportunity.opportunityType = opportunityType;
        opportunity.status = ServiceProfitOpportunityStatus.DETECTED;
        opportunity.evidenceClass = evidenceClass;
        opportunity.evidenceStrength = evidenceStrength;
        opportunity.priority = priority;
        opportunity.actionability = actionability;
        opportunity.title = title.trim();
        opportunity.summary = normalizeOptionalText(summary);
        opportunity.potentialAmount = potentialAmount;
        opportunity.currencyCode = normalizeCurrencyCode(currencyCode);
        opportunity.sourceSystem = sourceSystem.trim();
        opportunity.sourceEntityType = sourceEntityType.trim();
        opportunity.sourceEntityId = sourceEntityId.trim();
        opportunity.sourceServiceOrderId = sourceServiceOrderId;
        opportunity.sourceServiceJobId = sourceServiceJobId;
        opportunity.sourceServiceLineId = sourceServiceLineId;
        opportunity.sourceQuoteId = sourceQuoteId;
        opportunity.policyVersion = policyVersion.trim();
        opportunity.detectedAt = detectedAt;
        opportunity.createdByPrincipalId = principalId;
        opportunity.updatedByPrincipalId = principalId;
        opportunity.createdAt = detectedAt;
        opportunity.updatedAt = detectedAt;

        return opportunity;
    }

    private static void requireId(
            UUID id
    ) {
        requireId(
                id,
                "Service profit opportunity ID is required"
        );
    }

    private static void requireId(
            UUID id,
            String message
    ) {
        if (id == null) {
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

    private static void requireValue(
            Object value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePotentialValue(
            BigDecimal potentialAmount,
            String currencyCode
    ) {
        if (potentialAmount == null && currencyCode == null) {
            return;
        }

        if (potentialAmount == null) {
            throw new IllegalArgumentException(
                    "Service profit potential amount is required when currency is provided"
            );
        }

        if (potentialAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Service profit potential amount must not be negative"
            );
        }

        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Service profit currency code is required when potential amount is provided"
            );
        }

        String normalized = currencyCode.trim();

        if (normalized.length() != 3) {
            throw new IllegalArgumentException(
                    "Service profit currency code must contain exactly 3 characters"
            );
        }
    }

    private static String normalizeCurrencyCode(
            String currencyCode
    ) {
        if (currencyCode == null) {
            return null;
        }

        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptionalText(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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

    public UUID getLocationId() {
        return locationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public String getOpportunityKey() {
        return opportunityKey;
    }

    public ServiceProfitOpportunityType getOpportunityType() {
        return opportunityType;
    }

    public ServiceProfitOpportunityStatus getStatus() {
        return status;
    }

    public ServiceProfitEvidenceClass getEvidenceClass() {
        return evidenceClass;
    }

    public ServiceProfitEvidenceStrength getEvidenceStrength() {
        return evidenceStrength;
    }

    public ServiceProfitPriority getPriority() {
        return priority;
    }

    public ServiceProfitActionability getActionability() {
        return actionability;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getPotentialAmount() {
        return potentialAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public String getSourceEntityId() {
        return sourceEntityId;
    }

    public UUID getSourceServiceOrderId() {
        return sourceServiceOrderId;
    }

    public UUID getSourceServiceJobId() {
        return sourceServiceJobId;
    }

    public UUID getSourceServiceLineId() {
        return sourceServiceLineId;
    }

    public UUID getSourceQuoteId() {
        return sourceQuoteId;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public OffsetDateTime getDetectedAt() {
        return detectedAt;
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
