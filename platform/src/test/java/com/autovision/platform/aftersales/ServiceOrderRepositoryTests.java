package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceOrderRepositoryTests {

    @Autowired
    private ServiceOrderRepository repository;

    @AfterEach
    void clearServiceOrders() {
        repository.deleteAll();
    }

    @Test
    void persistsAndReloadsOpenServiceOrderWithoutOptionalDependencies() {

        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        ServiceOrder serviceOrder = ServiceOrder.open(
                id,
                tenantId,
                dealerId,
                branchId,
                "SO-000001",
                vehicleId,
                principalId,
                now
        );

        repository.saveAndFlush(serviceOrder);

        Optional<ServiceOrder> reloaded =
                repository.findByIdAndTenantId(id, tenantId);

        assertTrue(reloaded.isPresent());

        ServiceOrder persisted = reloaded.orElseThrow();

        assertEquals(id, persisted.getId());
        assertEquals(tenantId, persisted.getTenantId());
        assertEquals(dealerId, persisted.getDealerId());
        assertEquals(branchId, persisted.getBranchId());
        assertEquals("SO-000001", persisted.getOrderNumber());
        assertEquals(vehicleId, persisted.getVehicleId());
        assertEquals(ServiceOrderStatus.OPEN, persisted.getStatus());

        assertNotNull(persisted.getOpenedAt());

        assertNull(persisted.getCompletedAt());
        assertNull(persisted.getClosedAt());
        assertNull(persisted.getCancelledAt());

        assertEquals(principalId, persisted.getCreatedByPrincipalId());
        assertEquals(principalId, persisted.getUpdatedByPrincipalId());

        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());

        /*
         * Architectural acceptance:
         *
         * Persistence above requires no AfterSalesCase,
         * CustomerAuthorization, Inspection, Finding, AnalysisRun,
         * Quote, Appointment, Invoice or OrderLine association.
         *
         * ServiceOrder therefore remains an independently creatable
         * Core DMS transactional aggregate.
         */
    }

    @Test
    void persistsServiceOrderWithoutDealerOrBranch() {

        UUID tenantId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        ServiceOrder serviceOrder = ServiceOrder.open(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                "SO-NO-DEALER-1",
                vehicleId,
                null,
                OffsetDateTime.now()
        );

        repository.saveAndFlush(serviceOrder);

        ServiceOrder persisted =
                repository.findByIdAndTenantId(
                        serviceOrder.getId(),
                        tenantId
                ).orElseThrow();

        assertNull(persisted.getDealerId());
        assertNull(persisted.getBranchId());
        assertEquals(vehicleId, persisted.getVehicleId());
        assertEquals(ServiceOrderStatus.OPEN, persisted.getStatus());
    }

    @Test
    void findByIdAndTenantIdDoesNotReturnAnotherTenantsServiceOrder() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        ServiceOrder serviceOrder =
                newServiceOrder(
                        tenantId,
                        "SO-TENANT-1"
                );

        repository.saveAndFlush(serviceOrder);

        assertTrue(
                repository.findByIdAndTenantId(
                        serviceOrder.getId(),
                        tenantId
                ).isPresent()
        );

        assertFalse(
                repository.findByIdAndTenantId(
                        serviceOrder.getId(),
                        otherTenantId
                ).isPresent()
        );
    }

    @Test
    void findsServiceOrderByTenantAndOrderNumber() {

        UUID tenantId = UUID.randomUUID();

        ServiceOrder serviceOrder =
                newServiceOrder(
                        tenantId,
                        "SO-NUMBER-1"
                );

        repository.saveAndFlush(serviceOrder);

        assertTrue(
                repository.existsByTenantIdAndOrderNumber(
                        tenantId,
                        "SO-NUMBER-1"
                )
        );

        assertTrue(
                repository.findByTenantIdAndOrderNumber(
                        tenantId,
                        "SO-NUMBER-1"
                ).isPresent()
        );

        assertFalse(
                repository.existsByTenantIdAndOrderNumber(
                        UUID.randomUUID(),
                        "SO-NUMBER-1"
                )
        );
    }

    @Test
    void allowsSameOrderNumberAcrossDifferentTenants() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newServiceOrder(
                        tenantId,
                        "SO-SHARED-1"
                )
        );

        repository.saveAndFlush(
                newServiceOrder(
                        otherTenantId,
                        "SO-SHARED-1"
                )
        );

        assertTrue(
                repository.existsByTenantIdAndOrderNumber(
                        tenantId,
                        "SO-SHARED-1"
                )
        );

        assertTrue(
                repository.existsByTenantIdAndOrderNumber(
                        otherTenantId,
                        "SO-SHARED-1"
                )
        );

        assertEquals(
                1,
                repository.findAllByTenantId(tenantId).size()
        );

        assertEquals(
                1,
                repository.findAllByTenantId(otherTenantId).size()
        );
    }

    @Test
    void filtersServiceOrdersByTenantAndStatus() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newServiceOrder(
                        tenantId,
                        "SO-STATUS-1"
                )
        );

        repository.saveAndFlush(
                newServiceOrder(
                        otherTenantId,
                        "SO-STATUS-2"
                )
        );

        List<ServiceOrder> matches =
                repository.findAllByTenantIdAndStatus(
                        tenantId,
                        ServiceOrderStatus.OPEN
                );

        assertEquals(1, matches.size());
        assertEquals(
                "SO-STATUS-1",
                matches.getFirst().getOrderNumber()
        );
        assertEquals(
                tenantId,
                matches.getFirst().getTenantId()
        );
    }

    @Test
    void filtersServiceOrdersByTenantAndVehicle() {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        ServiceOrder tenantOrder = ServiceOrder.open(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                "SO-VEHICLE-1",
                vehicleId,
                null,
                OffsetDateTime.now()
        );

        ServiceOrder otherTenantOrder = ServiceOrder.open(
                UUID.randomUUID(),
                otherTenantId,
                null,
                null,
                "SO-VEHICLE-2",
                vehicleId,
                null,
                OffsetDateTime.now()
        );

        repository.saveAndFlush(tenantOrder);
        repository.saveAndFlush(otherTenantOrder);

        List<ServiceOrder> matches =
                repository.findAllByTenantIdAndVehicleId(
                        tenantId,
                        vehicleId
                );

        assertEquals(1, matches.size());
        assertEquals(
                "SO-VEHICLE-1",
                matches.getFirst().getOrderNumber()
        );
        assertEquals(vehicleId, matches.getFirst().getVehicleId());
    }

    private ServiceOrder newServiceOrder(
            UUID tenantId,
            String orderNumber
    ) {
        return ServiceOrder.open(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                orderNumber,
                UUID.randomUUID(),
                null,
                OffsetDateTime.now()
        );
    }
}