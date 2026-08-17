package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceOrderAggregateResponseTests {

    @Test
    void mapsCanonicalAggregateToApiResponse() {

        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime now =
                OffsetDateTime.parse(
                        "2026-08-17T12:00:00+00:00"
                );

        ServiceOrder order =
                ServiceOrder.open(
                        orderId,
                        tenantId,
                        dealerId,
                        branchId,
                        "SO-5001",
                        vehicleId,
                        principalId,
                        now
                );

        ServiceJob job =
                ServiceJob.open(
                        jobId,
                        orderId,
                        "JOB-001",
                        "Front brake inspection",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        principalId,
                        now
                );

        ServiceLine line =
                ServiceLine.create(
                        lineId,
                        orderId,
                        null,
                        10,
                        ServiceLineType.LABOR,
                        "Diagnostic labor",
                        new BigDecimal("1.5000"),
                        "HOUR",
                        principalId,
                        now
                );

        ServiceOrderAggregateResponse response =
                ServiceOrderAggregateResponse.from(
                        new ServiceOrderAggregateView(
                                order,
                                List.of(job),
                                List.of(line)
                        )
                );

        assertEquals(
                orderId,
                response.order().id()
        );

        assertEquals(
                tenantId,
                response.order().tenantId()
        );

        assertEquals(
                dealerId,
                response.order().dealerId()
        );

        assertEquals(
                branchId,
                response.order().branchId()
        );

        assertEquals(
                "SO-5001",
                response.order().orderNumber()
        );

        assertEquals(
                ServiceOrderStatus.OPEN,
                response.order().status()
        );

        assertEquals(
                1,
                response.jobs().size()
        );

        assertEquals(
                jobId,
                response.jobs().getFirst().id()
        );

        assertEquals(
                "JOB-001",
                response.jobs().getFirst().jobNumber()
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                response.jobs().getFirst().status()
        );

        assertEquals(
                1,
                response.lines().size()
        );

        assertEquals(
                lineId,
                response.lines().getFirst().id()
        );

        assertEquals(
                ServiceLineType.LABOR,
                response.lines().getFirst().lineType()
        );

        assertEquals(
                new BigDecimal("1.5000"),
                response.lines().getFirst().quantity()
        );

        assertNull(
                response.lines()
                        .getFirst()
                        .serviceJobId()
        );
    }
}