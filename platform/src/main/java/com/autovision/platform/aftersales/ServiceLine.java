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