package com.autovision.platform.aftersales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_quote_lines", schema = "platform")
public class ServiceQuoteLine {

    @Id
    private UUID id;

    @Column(name = "service_quote_id", nullable = false)
    private UUID serviceQuoteId;

    @Column(name = "service_line_id", nullable = false)
    private UUID serviceLineId;

    @Column(name = "service_job_id")
    private UUID serviceJobId;

    @Column(name = "description_snapshot", nullable = false, length = 500)
    private String descriptionSnapshot;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id", nullable = false)
    private UUID createdByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ServiceQuoteLine() {
    }

    public static ServiceQuoteLine create(
            UUID id,
            UUID serviceQuoteId,
            UUID serviceLineId,
            UUID serviceJobId,
            String descriptionSnapshot,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            int sequence,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireId(id, "Service quote line ID is required");
        requireId(serviceQuoteId, "Service quote ID is required");
        requireId(serviceLineId, "Service line ID is required");
        requireText(descriptionSnapshot, "Description snapshot is required");
        requirePositive(quantity, "Quantity must be greater than zero");
        requireNonNegative(unitPrice, "Unit price must not be negative");
        requireText(currencyCode, "Currency code is required");
        requireNonNegative(netAmount, "Net amount must not be negative");
        requireNonNegative(taxAmount, "Tax amount must not be negative");
        requireNonNegative(grossAmount, "Gross amount must not be negative");
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence must not be negative");
        }
        requireId(principalId, "Principal ID is required");
        requireTime(now, "Service quote line creation time is required");

        ServiceQuoteLine line = new ServiceQuoteLine();
        line.id = id;
        line.serviceQuoteId = serviceQuoteId;
        line.serviceLineId = serviceLineId;
        line.serviceJobId = serviceJobId;
        line.descriptionSnapshot = descriptionSnapshot;
        line.quantity = quantity;
        line.unitPrice = unitPrice;
        line.currencyCode = currencyCode;
        line.netAmount = netAmount;
        line.taxAmount = taxAmount;
        line.grossAmount = grossAmount;
        line.sequence = sequence;
        line.createdByPrincipalId = principalId;
        line.createdAt = now;
        return line;
    }

    private static void requireId(UUID value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private static void requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(OffsetDateTime value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }

    public UUID getId() { return id; }
    public UUID getServiceQuoteId() { return serviceQuoteId; }
    public UUID getServiceLineId() { return serviceLineId; }
    public UUID getServiceJobId() { return serviceJobId; }
    public String getDescriptionSnapshot() { return descriptionSnapshot; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public int getSequence() { return sequence; }
    public long getVersion() { return version; }
    public UUID getCreatedByPrincipalId() { return createdByPrincipalId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}