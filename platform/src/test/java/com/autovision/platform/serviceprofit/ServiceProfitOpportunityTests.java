package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceProfitOpportunityTests {

    @Test
    void detectsOpportunityWithCanonicalAndSourceProvenance() {

        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceJobId = UUID.randomUUID();
        UUID serviceLineId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime detectedAt = OffsetDateTime.now();

        ServiceProfitOpportunity opportunity =
                ServiceProfitOpportunity.detect(
                        id,
                        tenantId,
                        dealerId,
                        branchId,
                        locationId,
                        customerId,
                        vehicleId,
                        "DECLINED:RO-1001:JOB-10",
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitActionability.READY,
                        "Declined front brake work",
                        "Customer previously declined front brake repair.",
                        new BigDecimal("425.5000"),
                        "usd",
                        "AUTOVISION",
                        "SERVICE_JOB",
                        "JOB-10",
                        serviceOrderId,
                        serviceJobId,
                        serviceLineId,
                        quoteId,
                        "R1-POLICY-1",
                        principalId,
                        detectedAt
                );

        assertEquals(id, opportunity.getId());
        assertEquals(tenantId, opportunity.getTenantId());
        assertEquals(dealerId, opportunity.getDealerId());
        assertEquals(branchId, opportunity.getBranchId());
        assertEquals(locationId, opportunity.getLocationId());
        assertEquals(customerId, opportunity.getCustomerId());
        assertEquals(vehicleId, opportunity.getVehicleId());

        assertEquals(
                ServiceProfitOpportunityStatus.DETECTED,
                opportunity.getStatus()
        );

        assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                opportunity.getOpportunityType()
        );

        assertEquals(
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                opportunity.getEvidenceClass()
        );

        assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                opportunity.getEvidenceStrength()
        );

        assertEquals(
                ServiceProfitPriority.HIGH,
                opportunity.getPriority()
        );

        assertEquals(
                ServiceProfitActionability.READY,
                opportunity.getActionability()
        );

        assertEquals(
                0,
                new BigDecimal("425.5000")
                        .compareTo(opportunity.getPotentialAmount())
        );

        assertEquals("USD", opportunity.getCurrencyCode());
        assertEquals("AUTOVISION", opportunity.getSourceSystem());
        assertEquals("SERVICE_JOB", opportunity.getSourceEntityType());
        assertEquals("JOB-10", opportunity.getSourceEntityId());

        assertEquals(
                serviceOrderId,
                opportunity.getSourceServiceOrderId()
        );
        assertEquals(serviceJobId, opportunity.getSourceServiceJobId());
        assertEquals(serviceLineId, opportunity.getSourceServiceLineId());
        assertEquals(quoteId, opportunity.getSourceQuoteId());

        assertEquals("R1-POLICY-1", opportunity.getPolicyVersion());
        assertEquals(detectedAt, opportunity.getDetectedAt());
        assertEquals(detectedAt, opportunity.getCreatedAt());
        assertEquals(detectedAt, opportunity.getUpdatedAt());
        assertEquals(principalId, opportunity.getCreatedByPrincipalId());
        assertEquals(principalId, opportunity.getUpdatedByPrincipalId());
    }

    @Test
    void allowsOpportunityWithoutResolvedCustomerVehicleOrCanonicalServiceRecords() {

        ServiceProfitOpportunity opportunity =
                minimalOpportunity(
                        null,
                        null,
                        null,
                        null
                );

        assertNull(opportunity.getCustomerId());
        assertNull(opportunity.getVehicleId());
        assertNull(opportunity.getSourceServiceOrderId());
        assertNull(opportunity.getSourceServiceJobId());
        assertNull(opportunity.getSourceServiceLineId());
        assertNull(opportunity.getSourceQuoteId());
    }

    @Test
    void allowsOpportunityWithoutPotentialValue() {

        ServiceProfitOpportunity opportunity =
                ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DUE:SRC-1",
                        ServiceProfitOpportunityType.DUE_SERVICE,
                        ServiceProfitEvidenceClass.POLICY_DERIVED,
                        ServiceProfitEvidenceStrength.MODERATE,
                        ServiceProfitPriority.MEDIUM,
                        ServiceProfitActionability.REVIEW_REQUIRED,
                        "Service due",
                        null,
                        null,
                        null,
                        "DEALER_IMPORT",
                        "SERVICE_HISTORY",
                        "SRC-1",
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                );

        assertNull(opportunity.getPotentialAmount());
        assertNull(opportunity.getCurrencyCode());
    }

    @Test
    void rejectsPotentialAmountWithoutCurrency() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DECLINED:SRC-2",
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitActionability.READY,
                        "Declined work",
                        null,
                        new BigDecimal("100.0000"),
                        null,
                        "DEALER_IMPORT",
                        "RO_LINE",
                        "SRC-2",
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsCurrencyWithoutPotentialAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DECLINED:SRC-3",
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitActionability.READY,
                        "Declined work",
                        null,
                        null,
                        "USD",
                        "DEALER_IMPORT",
                        "RO_LINE",
                        "SRC-3",
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsNegativePotentialAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DECLINED:SRC-4",
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitActionability.READY,
                        "Declined work",
                        null,
                        new BigDecimal("-0.0001"),
                        "USD",
                        "DEALER_IMPORT",
                        "RO_LINE",
                        "SRC-4",
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsMissingTenant() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceProfitOpportunity.detect(
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "DUE:SRC-5",
                        ServiceProfitOpportunityType.DUE_SERVICE,
                        ServiceProfitEvidenceClass.POLICY_DERIVED,
                        ServiceProfitEvidenceStrength.MODERATE,
                        ServiceProfitPriority.MEDIUM,
                        ServiceProfitActionability.REVIEW_REQUIRED,
                        "Service due",
                        null,
                        null,
                        null,
                        "DEALER_IMPORT",
                        "SERVICE_HISTORY",
                        "SRC-5",
                        null,
                        null,
                        null,
                        null,
                        "R1-POLICY-1",
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    private ServiceProfitOpportunity minimalOpportunity(
            UUID customerId,
            UUID vehicleId,
            UUID serviceOrderId,
            UUID serviceJobId
    ) {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                customerId,
                vehicleId,
                "DECLINED:MINIMAL-1",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.EVIDENCE_DERIVED,
                ServiceProfitEvidenceStrength.MODERATE,
                ServiceProfitPriority.MEDIUM,
                ServiceProfitActionability.REVIEW_REQUIRED,
                "Potential declined work",
                null,
                null,
                null,
                "DEALER_IMPORT",
                "RO_LINE",
                "MINIMAL-1",
                serviceOrderId,
                serviceJobId,
                null,
                null,
                "R1-POLICY-1",
                null,
                OffsetDateTime.now()
        );
    }
}
