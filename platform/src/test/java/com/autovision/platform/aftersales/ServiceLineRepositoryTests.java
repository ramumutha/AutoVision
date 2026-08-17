package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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
class ServiceLineRepositoryTests {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceJobRepository serviceJobRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @AfterEach
    void clearTables() {
        serviceLineRepository.deleteAll();
        serviceJobRepository.deleteAll();
        serviceOrderRepository.deleteAll();
    }

    @Test
    void persistsDirectServiceLineWithoutServiceJob() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0001");

        UUID principalId = UUID.randomUUID();

        ServiceLine serviceLine = ServiceLine.create(
                UUID.randomUUID(),
                serviceOrder.getId(),
                null,
                10,
                ServiceLineType.PART,
                "Replace wiper blade",
                BigDecimal.ONE,
                "EA",
                principalId,
                OffsetDateTime.now()
        );

        serviceLineRepository.saveAndFlush(serviceLine);

        ServiceLine persisted =
                serviceLineRepository.findByIdAndServiceOrderId(
                        serviceLine.getId(),
                        serviceOrder.getId()
                ).orElseThrow();

        assertEquals(
                serviceOrder.getId(),
                persisted.getServiceOrderId()
        );
        assertNull(persisted.getServiceJobId());
        assertEquals(10, persisted.getLineNumber());
        assertEquals(
                ServiceLineType.PART,
                persisted.getLineType()
        );
        assertEquals(
                "Replace wiper blade",
                persisted.getDescription()
        );
        assertEquals(
                0,
                BigDecimal.ONE.compareTo(persisted.getQuantity())
        );
        assertEquals("EA", persisted.getUnitOfMeasure());
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
    void persistsServiceLineAssignedToServiceJob() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0002");

        ServiceJob serviceJob =
                persistServiceJob(
                        serviceOrder,
                        "JOB-001",
                        "Front brake service"
                );

        ServiceLine serviceLine = ServiceLine.create(
                UUID.randomUUID(),
                serviceOrder.getId(),
                serviceJob.getId(),
                10,
                ServiceLineType.LABOR,
                "Replace front brake pads",
                new BigDecimal("1.5000"),
                "HOUR",
                null,
                OffsetDateTime.now()
        );

        serviceLineRepository.saveAndFlush(serviceLine);

        ServiceLine persisted =
                serviceLineRepository.findByIdAndServiceOrderId(
                        serviceLine.getId(),
                        serviceOrder.getId()
                ).orElseThrow();

        assertEquals(
                serviceOrder.getId(),
                persisted.getServiceOrderId()
        );
        assertEquals(
                serviceJob.getId(),
                persisted.getServiceJobId()
        );
    }

    @Test
    void findsDirectLinesWithinServiceOrder() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0003");

        ServiceJob serviceJob =
                persistServiceJob(
                        serviceOrder,
                        "JOB-001",
                        "Workshop job"
                );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        null,
                        10,
                        "Direct line"
                )
        );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        serviceJob.getId(),
                        20,
                        "Job line"
                )
        );

        List<ServiceLine> directLines =
                serviceLineRepository
                        .findAllByServiceOrderIdAndServiceJobIdIsNullOrderByLineNumber(
                                serviceOrder.getId()
                        );

        assertEquals(1, directLines.size());
        assertEquals(
                "Direct line",
                directLines.getFirst().getDescription()
        );
        assertNull(directLines.getFirst().getServiceJobId());
    }

    @Test
    void findsLinesAssignedToSpecificServiceJob() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0004");

        ServiceJob firstJob =
                persistServiceJob(
                        serviceOrder,
                        "JOB-001",
                        "First job"
                );

        ServiceJob secondJob =
                persistServiceJob(
                        serviceOrder,
                        "JOB-002",
                        "Second job"
                );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        firstJob.getId(),
                        10,
                        "First job line"
                )
        );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        secondJob.getId(),
                        20,
                        "Second job line"
                )
        );

        List<ServiceLine> matches =
                serviceLineRepository
                        .findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
                                serviceOrder.getId(),
                                firstJob.getId()
                        );

        assertEquals(1, matches.size());
        assertEquals(
                firstJob.getId(),
                matches.getFirst().getServiceJobId()
        );
        assertEquals(
                "First job line",
                matches.getFirst().getDescription()
        );
    }

    @Test
    void ordersAllServiceLinesByLineNumberWithinOrder() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0005");

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        null,
                        30,
                        "Third line"
                )
        );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        null,
                        10,
                        "First line"
                )
        );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        serviceOrder.getId(),
                        null,
                        20,
                        "Second line"
                )
        );

        List<ServiceLine> lines =
                serviceLineRepository
                        .findAllByServiceOrderIdOrderByLineNumber(
                                serviceOrder.getId()
                        );

        assertEquals(3, lines.size());
        assertEquals(10, lines.get(0).getLineNumber());
        assertEquals(20, lines.get(1).getLineNumber());
        assertEquals(30, lines.get(2).getLineNumber());
    }

    @Test
    void allowsSameLineNumberAcrossDifferentServiceOrders() {

        ServiceOrder firstOrder =
                persistServiceOrder("SO-LINE-0006");

        ServiceOrder secondOrder =
                persistServiceOrder("SO-LINE-0007");

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        firstOrder.getId(),
                        null,
                        10,
                        "First order line"
                )
        );

        serviceLineRepository.saveAndFlush(
                newServiceLine(
                        secondOrder.getId(),
                        null,
                        10,
                        "Second order line"
                )
        );

        assertTrue(
                serviceLineRepository
                        .existsByServiceOrderIdAndLineNumber(
                                firstOrder.getId(),
                                10
                        )
        );

        assertTrue(
                serviceLineRepository
                        .existsByServiceOrderIdAndLineNumber(
                                secondOrder.getId(),
                                10
                        )
        );
    }

    @Test
    void supportsFractionalServiceLineQuantity() {

        ServiceOrder serviceOrder =
                persistServiceOrder("SO-LINE-0008");

        ServiceLine serviceLine = ServiceLine.create(
                UUID.randomUUID(),
                serviceOrder.getId(),
                null,
                10,
                ServiceLineType.OTHER,
                "Measured workshop material",
                new BigDecimal("0.7500"),
                "LITER",
                null,
                OffsetDateTime.now()
        );

        serviceLineRepository.saveAndFlush(serviceLine);

        ServiceLine persisted =
                serviceLineRepository.findByIdAndServiceOrderId(
                        serviceLine.getId(),
                        serviceOrder.getId()
                ).orElseThrow();

        assertEquals(
                0,
                new BigDecimal("0.7500")
                        .compareTo(persisted.getQuantity())
        );
    }

    @Test
    void doesNotFindServiceLineThroughAnotherServiceOrder() {

        ServiceOrder firstOrder =
                persistServiceOrder("SO-LINE-0009");

        ServiceOrder secondOrder =
                persistServiceOrder("SO-LINE-0010");

        ServiceLine serviceLine =
                newServiceLine(
                        firstOrder.getId(),
                        null,
                        10,
                        "Scoped service line"
                );

        serviceLineRepository.saveAndFlush(serviceLine);

        assertTrue(
                serviceLineRepository.findByIdAndServiceOrderId(
                        serviceLine.getId(),
                        firstOrder.getId()
                ).isPresent()
        );

        assertFalse(
                serviceLineRepository.findByIdAndServiceOrderId(
                        serviceLine.getId(),
                        secondOrder.getId()
                ).isPresent()
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

    private ServiceJob persistServiceJob(
            ServiceOrder serviceOrder,
            String jobNumber,
            String summary
    ) {
        ServiceJob serviceJob = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrder.getId(),
                jobNumber,
                summary,
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        return serviceJobRepository.saveAndFlush(serviceJob);
    }

    private ServiceLine newServiceLine(
            UUID serviceOrderId,
            UUID serviceJobId,
            int lineNumber,
            String description
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                serviceOrderId,
                serviceJobId,
                lineNumber,
                ServiceLineType.LABOR,
                description,
                BigDecimal.ONE,
                "EA",
                null,
                OffsetDateTime.now()
        );
    }
}