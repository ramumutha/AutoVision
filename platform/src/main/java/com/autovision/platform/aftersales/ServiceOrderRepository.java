package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderRepository
        extends JpaRepository<ServiceOrder, UUID> {

    List<ServiceOrder> findAllByTenantId(
            UUID tenantId
    );

    List<ServiceOrder> findAllByTenantIdAndStatus(
            UUID tenantId,
            ServiceOrderStatus status
    );

    List<ServiceOrder> findAllByTenantIdAndVehicleId(
            UUID tenantId,
            UUID vehicleId
    );

    Optional<ServiceOrder> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Optional<ServiceOrder> findByTenantIdAndOrderNumber(
            UUID tenantId,
            String orderNumber
    );

    boolean existsByTenantIdAndOrderNumber(
            UUID tenantId,
            String orderNumber
    );
}
