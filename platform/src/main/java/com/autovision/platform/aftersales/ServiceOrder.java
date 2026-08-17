package com.autovision.platform.aftersales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "service_orders",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_orders_tenant_order_number",
                        columnNames = {"tenant_id", "order_number"}
                ),
                @UniqueConstraint(
                        name = "uq_service_orders_id_tenant",
                        columnNames = {"id", "tenant_id"}
                )
        }
)
public class ServiceOrder {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "dealer_id")
    private UUID dealerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "order_number", nullable = false, length = 80)
    private String orderNumber;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ServiceOrderStatus status;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

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

    protected ServiceOrder() {
    }

    public static ServiceOrder open(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            String orderNumber,
            UUID vehicleId,
            UUID principalId,
            OffsetDateTime now
    ) {
        ServiceOrder serviceOrder = new ServiceOrder();

        serviceOrder.id = id;
        serviceOrder.tenantId = tenantId;
        serviceOrder.dealerId = dealerId;
        serviceOrder.branchId = branchId;
        serviceOrder.orderNumber = orderNumber;
        serviceOrder.vehicleId = vehicleId;
        serviceOrder.status = ServiceOrderStatus.OPEN;
        serviceOrder.openedAt = now;
        serviceOrder.createdByPrincipalId = principalId;
        serviceOrder.updatedByPrincipalId = principalId;
        serviceOrder.createdAt = now;
        serviceOrder.updatedAt = now;

        return serviceOrder;
    }

    public void start(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceOrderStatus.IN_PROGRESS,
                policy,
                principalId,
                now
        );
    }

    public void completeWork(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceOrderStatus.WORK_COMPLETED,
                policy,
                principalId,
                now
        );

        completedAt = now;
    }

    public void close(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceOrderStatus.CLOSED,
                policy,
                principalId,
                now
        );

        closedAt = now;
    }

    public void cancel(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceOrderStatus.CANCELLED,
                policy,
                principalId,
                now
        );

        cancelledAt = now;
    }

    private void transition(
            ServiceOrderStatus targetStatus,
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Service lifecycle policy is required"
            );
        }

        if (!policy.isServiceOrderTransitionAllowed(
                status,
                targetStatus
        )) {
            throw new IllegalStateException(
                    "Service order transition is not allowed: "
                            + status
                            + " -> "
                            + targetStatus
            );
        }

        status = targetStatus;
        updatedByPrincipalId = principalId;
        updatedAt = now;
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

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public ServiceOrderStatus getStatus() {
        return status;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
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