package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceJobRepositoryTests {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceJobRepository serviceJobRepository;

    @AfterEach
    void clearTables() {
        serviceJobRepository.deleteAll();
        serviceOrderRepository.deleteAll();
    }

    @Test
    void persistsAndReloadsOpenServiceJob() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-JOB-0001");

        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceJob serviceJob = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrder.getId(),
                "JOB-001",
                "Front brake repair",
                ServiceJobApprovalStatus.PENDING,
                principalId,
                now
        );

        serviceJobRepository.saveAndFlush(serviceJob);

        ServiceJob persisted =
                serviceJobRepository.findByIdAndServiceOrderId(
                        serviceJob.getId(),
                        serviceOrder.getId()
                ).orElseThrow();

        assertEquals(serviceJob.getId(), persisted.getId());
        assertEquals(
                serviceOrder.getId(),
                persisted.getServiceOrderId()
        );
        assertEquals("JOB-001", persisted.getJobNumber());
        assertEquals(
                "Front brake repair",
                persisted.getSummary()
        );
        assertEquals(
                ServiceJobStatus.OPEN,
                persisted.getStatus()
        );
        assertEquals(
                ServiceJobApprovalStatus.PENDING,
                persisted.getApprovalStatus()
        );

        assertNotNull(persisted.getOpenedAt());

        assertNull(persisted.getReadyAt());
        assertNull(persisted.getStartedAt());
        assertNull(persisted.getCompletedAt());
        assertNull(persisted.getCancelledAt());
        assertNull(persisted.getApprovedAt());
        assertNull(persisted.getApprovedByPrincipalId());

        assertEquals(
                principalId,
                persisted.getCreatedByPrincipalId()
        );
        assertEquals(
                principalId,
                persisted.getUpdatedByPrincipalId()
        );
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void supportsJobsWhereOperationalApprovalIsNotRequired() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-JOB-0002");

        ServiceJob serviceJob = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrder.getId(),
                "JOB-001",
                "Replace wiper mechanism",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        serviceJobRepository.saveAndFlush(serviceJob);

        ServiceJob persisted =
                serviceJobRepository.findByIdAndServiceOrderId(
                        serviceJob.getId(),
                        serviceOrder.getId()
                ).orElseThrow();

        assertEquals(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                persisted.getApprovalStatus()
        );
    }

    @Test
    void findsJobsOnlyWithinRequestedServiceOrder() {

        ServiceOrder firstOrder =
                persistServiceOrder("SO-JOB-0003");

        ServiceOrder secondOrder =
                persistServiceOrder("SO-JOB-0004");

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        firstOrder.getId(),
                        "JOB-001",
                        "First order job"
                )
        );

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        secondOrder.getId(),
                        "JOB-001",
                        "Second order job"
                )
        );

        List<ServiceJob> matches =
                serviceJobRepository
                        .findAllByServiceOrderIdOrderByJobNumber(
                                firstOrder.getId()
                        );

        assertEquals(1, matches.size());
        assertEquals(
                firstOrder.getId(),
                matches.getFirst().getServiceOrderId()
        );
        assertEquals(
                "First order job",
                matches.getFirst().getSummary()
        );
    }

    @Test
    void findsJobByServiceOrderAndJobNumber() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-JOB-0005");

        ServiceJob serviceJob =
                newServiceJob(
                        serviceOrder.getId(),
                        "JOB-010",
                        "Cooling system diagnosis"
                );

        serviceJobRepository.saveAndFlush(serviceJob);

        assertTrue(
                serviceJobRepository.existsByServiceOrderIdAndJobNumber(
                        serviceOrder.getId(),
                        "JOB-010"
                )
        );

        assertTrue(
                serviceJobRepository.findByServiceOrderIdAndJobNumber(
                        serviceOrder.getId(),
                        "JOB-010"
                ).isPresent()
        );

        assertFalse(
                serviceJobRepository.existsByServiceOrderIdAndJobNumber(
                        serviceOrder.getId(),
                        "JOB-020"
                )
        );
    }

    @Test
    void allowsSameJobNumberAcrossDifferentServiceOrders() {

        ServiceOrder firstOrder =
                persistServiceOrder("SO-JOB-0006");

        ServiceOrder secondOrder =
                persistServiceOrder("SO-JOB-0007");

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        firstOrder.getId(),
                        "JOB-001",
                        "First job"
                )
        );

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        secondOrder.getId(),
                        "JOB-001",
                        "Second job"
                )
        );

        assertTrue(
                serviceJobRepository.existsByServiceOrderIdAndJobNumber(
                        firstOrder.getId(),
                        "JOB-001"
                )
        );

        assertTrue(
                serviceJobRepository.existsByServiceOrderIdAndJobNumber(
                        secondOrder.getId(),
                        "JOB-001"
                )
        );
    }

    @Test
    void filtersJobsByExecutionStatusWithinServiceOrder() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-JOB-0008");

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        serviceOrder.getId(),
                        "JOB-001",
                        "Open workshop job"
                )
        );

        List<ServiceJob> matches =
                serviceJobRepository
                        .findAllByServiceOrderIdAndStatusOrderByJobNumber(
                                serviceOrder.getId(),
                                ServiceJobStatus.OPEN
                        );

        assertEquals(1, matches.size());
        assertEquals(
                ServiceJobStatus.OPEN,
                matches.getFirst().getStatus()
        );
    }

    @Test
    void filtersJobsByApprovalStatusWithinServiceOrder() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-JOB-0009");

        serviceJobRepository.saveAndFlush(
                newServiceJob(
                        serviceOrder.getId(),
                        "JOB-001",
                        "Approval pending job"
                )
        );

        ServiceJob noApprovalRequired = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrder.getId(),
                "JOB-002",
                "No approval required job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        serviceJobRepository.saveAndFlush(noApprovalRequired);

        List<ServiceJob> matches =
                serviceJobRepository
                        .findAllByServiceOrderIdAndApprovalStatusOrderByJobNumber(
                                serviceOrder.getId(),
                                ServiceJobApprovalStatus.PENDING
                        );

        assertEquals(1, matches.size());
        assertEquals(
                "JOB-001",
                matches.getFirst().getJobNumber()
        );
    }

    private ServiceOrder persistServiceOrder(
            String orderNumber
    ) {
        ServiceOrder serviceOrder = ServiceOrder.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                orderNumber,
                UUID.randomUUID(),
                null,
                OffsetDateTime.now()
        );

        return serviceOrderRepository.saveAndFlush(serviceOrder);
    }

    private ServiceJob newServiceJob(
            UUID serviceOrderId,
            String jobNumber,
            String summary
    ) {
        return ServiceJob.open(
                UUID.randomUUID(),
                serviceOrderId,
                jobNumber,
                summary,
                ServiceJobApprovalStatus.PENDING,
                null,
                OffsetDateTime.now()
        );
    }
}