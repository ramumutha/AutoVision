package com.autovision.platform.aftersales;

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
@Table(name = "service_quotes", schema = "platform")
public class ServiceQuote {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "after_sales_case_id")
    private UUID afterSalesCaseId;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "quote_number", nullable = false, length = 80)
    private String quoteNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ServiceQuoteStatus status;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "terms_snapshot", length = 4000)
    private String termsSnapshot;

    @Column(name = "disclaimer_snapshot", length = 4000)
    private String disclaimerSnapshot;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @Column(name = "superseded_at")
    private OffsetDateTime supersededAt;

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

    protected ServiceQuote() {
    }

    public static ServiceQuote create(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            UUID afterSalesCaseId,
            UUID serviceOrderId,
            String quoteNumber,
            String currencyCode,
            OffsetDateTime validUntil,
            String termsSnapshot,
            String disclaimerSnapshot,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Service quote ID is required");
        requireId(tenantId, "Tenant ID is required");
        requireId(serviceOrderId, "Service order ID is required");
        requireText(quoteNumber, "Quote number is required");
        requireText(currencyCode, "Currency code is required");
        requireId(principalId, "Principal ID is required");
        requireTime(now, "Service quote creation time is required");

        if (branchId != null && dealerId == null) {
            throw new IllegalArgumentException(
                    "Dealer ID is required when branch ID is provided"
            );
        }

        ServiceQuote quote = new ServiceQuote();
        quote.id = id;
        quote.tenantId = tenantId;
        quote.dealerId = dealerId;
        quote.branchId = branchId;
        quote.afterSalesCaseId = afterSalesCaseId;
        quote.serviceOrderId = serviceOrderId;
        quote.quoteNumber = quoteNumber;
        quote.status = ServiceQuoteStatus.DRAFT;
        quote.currencyCode = currencyCode;
        quote.validUntil = validUntil;
        quote.termsSnapshot = termsSnapshot;
        quote.disclaimerSnapshot = disclaimerSnapshot;
        quote.createdByPrincipalId = principalId;
        quote.updatedByPrincipalId = principalId;
        quote.createdAt = now;
        quote.updatedAt = now;
        return quote;
    }

    public void issue(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.ISSUED, principalId, now);
    }

    public void accept(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.ACCEPTED, principalId, now);
    }

    public void decline(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.DECLINED, principalId, now);
    }

    public void cancel(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.CANCELLED, principalId, now);
    }

    public void expire(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.EXPIRED, principalId, now);
    }

    public void supersede(UUID principalId, OffsetDateTime now) {
        changeStatus(ServiceQuoteStatus.SUPERSEDED, principalId, now);
    }

    public void changeStatus(
            ServiceQuoteStatus targetStatus,
            UUID principalId,
            OffsetDateTime now
    ) {
        if (targetStatus == null) {
            throw new IllegalArgumentException(
                    "Service quote target status is required"
            );
        }

        requireId(principalId, "Principal ID is required");
        requireTime(now, "Service quote mutation time is required");

        if (status == targetStatus) {
            throw new IllegalStateException(
                    "Service quote status is already "
                            + status + " -> " + targetStatus
            );
        }

        status = targetStatus;
        updatedByPrincipalId = principalId;
        updatedAt = now;

        switch (targetStatus) {
            case ISSUED -> issuedAt = now;
            case ACCEPTED -> acceptedAt = now;
            case DECLINED -> declinedAt = now;
            case CANCELLED -> cancelledAt = now;
            case EXPIRED -> expiredAt = now;
            case SUPERSEDED -> supersededAt = now;
            case DRAFT -> { }
        }
    }

    private static void requireId(UUID value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(OffsetDateTime value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDealerId() { return dealerId; }
    public UUID getBranchId() { return branchId; }
    public UUID getAfterSalesCaseId() { return afterSalesCaseId; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public String getQuoteNumber() { return quoteNumber; }
    public ServiceQuoteStatus getStatus() { return status; }
    public String getCurrencyCode() { return currencyCode; }
    public OffsetDateTime getValidUntil() { return validUntil; }
    public String getTermsSnapshot() { return termsSnapshot; }
    public String getDisclaimerSnapshot() { return disclaimerSnapshot; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public OffsetDateTime getDeclinedAt() { return declinedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public OffsetDateTime getSupersededAt() { return supersededAt; }
    public long getVersion() { return version; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }
    public UUID getUpdatedByPrincipalId() { return updatedByPrincipalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}