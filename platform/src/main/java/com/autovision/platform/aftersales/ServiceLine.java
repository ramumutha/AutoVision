package com.autovision.platform.aftersales;

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
import java.util.UUID;

@Entity
@Table(
        name = "service_lines",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_lines_order_line_number",
                        columnNames = {"service_order_id", "line_number"}
                )
        }
)
public class ServiceLine {

    @Id
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "service_job_id")
    private UUID serviceJobId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 32)
    private ServiceLineType lineType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(
            name = "quantity",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", nullable = false, length = 32)
    private String unitOfMeasure;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "gross_amount", precision = 19, scale = 4)
    private BigDecimal grossAmount;

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

    protected ServiceLine() {
    }

    public static ServiceLine create(
            UUID id,
            UUID serviceOrderId,
            UUID serviceJobId,
            int lineNumber,
            ServiceLineType lineType,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            UUID principalId,
            OffsetDateTime now
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Service line ID is required"
            );
        }

        if (serviceOrderId == null) {
            throw new IllegalArgumentException(
                    "Service order ID is required"
            );
        }

        if (lineNumber <= 0) {
            throw new IllegalArgumentException(
                    "Service line number must be greater than zero"
            );
        }

        requireLineType(lineType);
        requireDescription(description);
        requireQuantity(quantity);
        requireUnitOfMeasure(unitOfMeasure);

        if (now == null) {
            throw new IllegalArgumentException(
                    "Service line creation time is required"
            );
        }

        ServiceLine serviceLine = new ServiceLine();

        serviceLine.id = id;
        serviceLine.serviceOrderId = serviceOrderId;
        serviceLine.serviceJobId = serviceJobId;
        serviceLine.lineNumber = lineNumber;
        serviceLine.lineType = lineType;
        serviceLine.description = description;
        serviceLine.quantity = quantity;
        serviceLine.unitOfMeasure = unitOfMeasure;
        serviceLine.createdByPrincipalId = principalId;
        serviceLine.updatedByPrincipalId = principalId;
        serviceLine.createdAt = now;
        serviceLine.updatedAt = now;

        return serviceLine;
    }

    public void updateDetails(
            ServiceLineType lineType,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireLineType(lineType);
        requireDescription(description);
        requireQuantity(quantity);
        requireUnitOfMeasure(unitOfMeasure);
        requireMutationTime(now);

        this.lineType = lineType;
        this.description = description;
        this.quantity = quantity;
        this.unitOfMeasure = unitOfMeasure;

        touch(principalId, now);
    }

    public void assignToJob(
            UUID serviceJobId,
            UUID principalId,
            OffsetDateTime now
    ) {
        if (serviceJobId == null) {
            throw new IllegalArgumentException(
                    "Service job ID is required"
            );
        }

        requireMutationTime(now);

        this.serviceJobId = serviceJobId;

        touch(principalId, now);
    }

    public void unassignFromJob(
            UUID principalId,
            OffsetDateTime now
    ) {
        requireMutationTime(now);

        this.serviceJobId = null;

        touch(principalId, now);
    }

    public void applyCommercialSnapshot(
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireNonNegative(unitPrice, "Service line unit price must not be negative");
        requireCurrencyCode(currencyCode);
        requireNonNegative(netAmount, "Service line net amount must not be negative");
        requireNonNegative(taxAmount, "Service line tax amount must not be negative");
        requireNonNegative(grossAmount, "Service line gross amount must not be negative");
        if (principalId == null) {
            throw new IllegalArgumentException(
                    "Service line commercial snapshot principal is required"
            );
        }
        requireMutationTime(now);

        this.unitPrice = unitPrice;
        this.currencyCode = currencyCode;
        this.netAmount = netAmount;
        this.taxAmount = taxAmount;
        this.grossAmount = grossAmount;

        touch(principalId, now);
    }

    public boolean hasCommercialSnapshot() {
        return unitPrice != null
                && currencyCode != null
                && !currencyCode.isBlank()
                && netAmount != null
                && taxAmount != null
                && grossAmount != null;
    }

    private static void requireLineType(
            ServiceLineType lineType
    ) {
        if (lineType == null) {
            throw new IllegalArgumentException(
                    "Service line type is required"
            );
        }
    }

    private static void requireDescription(
            String description
    ) {
        if (
                description == null
                        || description.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Service line description is required"
            );
        }
    }

    private static void requireQuantity(
            BigDecimal quantity
    ) {
        if (
                quantity == null
                        || quantity.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new IllegalArgumentException(
                    "Service line quantity must be greater than zero"
            );
        }
    }

    private static void requireUnitOfMeasure(
            String unitOfMeasure
    ) {
        if (
                unitOfMeasure == null
                        || unitOfMeasure.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Service line unit of measure is required"
            );
        }
    }

    private static void requireCurrencyCode(
            String currencyCode
    ) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Service line currency code is required"
            );
        }
    }

    private static void requireNonNegative(
            BigDecimal amount,
            String message
    ) {
        if (
                amount == null
                        || amount.compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireMutationTime(
            OffsetDateTime now
    ) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "Service line mutation time is required"
            );
        }
    }

    private void touch(
            UUID principalId,
            OffsetDateTime now
    ) {
        this.updatedByPrincipalId = principalId;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getServiceJobId() {
        return serviceJobId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ServiceLineType getLineType() {
        return lineType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
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