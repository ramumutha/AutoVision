package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceLineLifecycleTests {

    @Test
    void updatesMutableServiceLineDetails() {

        UUID createPrincipal = UUID.randomUUID();
        UUID updatePrincipal = UUID.randomUUID();

        OffsetDateTime createdAt =
                OffsetDateTime.now().minusMinutes(5);

        OffsetDateTime updatedAt =
                OffsetDateTime.now();

        ServiceLine line =
                newLine(
                        null,
                        createPrincipal,
                        createdAt
                );

        line.updateDetails(
                ServiceLineType.PART,
                "Replacement brake pad",
                new BigDecimal("2.0000"),
                "EA",
                updatePrincipal,
                updatedAt
        );

        assertEquals(
                ServiceLineType.PART,
                line.getLineType()
        );

        assertEquals(
                "Replacement brake pad",
                line.getDescription()
        );

        assertEquals(
                0,
                new BigDecimal("2.0000")
                        .compareTo(line.getQuantity())
        );

        assertEquals(
                "EA",
                line.getUnitOfMeasure()
        );

        assertEquals(
                updatePrincipal,
                line.getUpdatedByPrincipalId()
        );

        assertEquals(
                updatedAt,
                line.getUpdatedAt()
        );

        assertEquals(
                createPrincipal,
                line.getCreatedByPrincipalId()
        );

        assertEquals(
                createdAt,
                line.getCreatedAt()
        );
    }

    @Test
    void assignsDirectServiceLineToJob() {

        UUID jobId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceLine line =
                newLine(
                        null,
                        UUID.randomUUID(),
                        now.minusMinutes(1)
                );

        line.assignToJob(
                jobId,
                principalId,
                now
        );

        assertEquals(
                jobId,
                line.getServiceJobId()
        );

        assertEquals(
                principalId,
                line.getUpdatedByPrincipalId()
        );

        assertEquals(
                now,
                line.getUpdatedAt()
        );
    }

    @Test
    void unassignsServiceLineFromJob() {

        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ServiceLine line =
                newLine(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        now.minusMinutes(1)
                );

        line.unassignFromJob(
                principalId,
                now
        );

        assertNull(
                line.getServiceJobId()
        );

        assertEquals(
                principalId,
                line.getUpdatedByPrincipalId()
        );
    }

    @Test
    void rejectsNullJobAssignment() {

        ServiceLine line =
                newLine(
                        null,
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> line.assignToJob(
                        null,
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsNullLineTypeDuringUpdate() {

        ServiceLine line = newLine();

        assertThrows(
                IllegalArgumentException.class,
                () -> line.updateDetails(
                        null,
                        "Valid description",
                        BigDecimal.ONE,
                        "EA",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsBlankDescriptionDuringUpdate() {

        ServiceLine line = newLine();

        assertThrows(
                IllegalArgumentException.class,
                () -> line.updateDetails(
                        ServiceLineType.LABOR,
                        " ",
                        BigDecimal.ONE,
                        "HOUR",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsNonPositiveQuantityDuringUpdate() {

        ServiceLine line = newLine();

        assertThrows(
                IllegalArgumentException.class,
                () -> line.updateDetails(
                        ServiceLineType.LABOR,
                        "Workshop labor",
                        BigDecimal.ZERO,
                        "HOUR",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> line.updateDetails(
                        ServiceLineType.LABOR,
                        "Workshop labor",
                        new BigDecimal("-1"),
                        "HOUR",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsBlankUnitOfMeasureDuringUpdate() {

        ServiceLine line = newLine();

        assertThrows(
                IllegalArgumentException.class,
                () -> line.updateDetails(
                        ServiceLineType.LABOR,
                        "Workshop labor",
                        BigDecimal.ONE,
                        " ",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsInvalidCreationArguments() {

        UUID orderId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceLine.create(
                        null,
                        orderId,
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Labor",
                        BigDecimal.ONE,
                        "HOUR",
                        principalId,
                        now
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceLine.create(
                        UUID.randomUUID(),
                        null,
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Labor",
                        BigDecimal.ONE,
                        "HOUR",
                        principalId,
                        now
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceLine.create(
                        UUID.randomUUID(),
                        orderId,
                        null,
                        0,
                        ServiceLineType.LABOR,
                        "Labor",
                        BigDecimal.ONE,
                        "HOUR",
                        principalId,
                        now
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceLine.create(
                        UUID.randomUUID(),
                        orderId,
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Labor",
                        BigDecimal.ZERO,
                        "HOUR",
                        principalId,
                        now
                )
        );
    }

    @Test
    void preservesOrderAndLineNumberAcrossMutations() {

        UUID orderId = UUID.randomUUID();

        ServiceLine line =
                ServiceLine.create(
                        UUID.randomUUID(),
                        orderId,
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Initial labor",
                        BigDecimal.ONE,
                        "HOUR",
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                );

        line.updateDetails(
                ServiceLineType.PART,
                "Updated line",
                new BigDecimal("2"),
                "EA",
                UUID.randomUUID(),
                OffsetDateTime.now()
        );

        line.assignToJob(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now()
        );

        assertEquals(
                orderId,
                line.getServiceOrderId()
        );

        assertEquals(
                10,
                line.getLineNumber()
        );
    }

    private ServiceLine newLine() {
        return newLine(
                null,
                UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }

    private ServiceLine newLine(
            UUID serviceJobId,
            UUID principalId,
            OffsetDateTime now
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                serviceJobId,
                10,
                ServiceLineType.LABOR,
                "Workshop labor",
                BigDecimal.ONE,
                "HOUR",
                principalId,
                now
        );
    }
}