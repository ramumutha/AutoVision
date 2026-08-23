package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.detection.ServiceProfitDisposition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProfitDemoDetectionInputAdapterTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private final ServiceProfitDemoDetectionInputAdapter adapter =
            new ServiceProfitDemoDetectionInputAdapter();

    @Test
    void loadsAllTenDemoDetectionScenarios() {

        List<ServiceProfitDemoDetectionScenario> scenarios =
                adapter.load(
                        datasetRoot(),
                        TENANT_ID
                );

        assertEquals(10, scenarios.size());

        assertEquals(
                10,
                scenarios.stream()
                        .map(ServiceProfitDemoDetectionScenario::scenarioId)
                        .distinct()
                        .count()
        );

        assertTrue(scenarios.stream().allMatch(
                scenario -> scenario.context() != null
        ));
    }

    @Test
    void mapsExplicitDeclinedScenario() {

        var scenario =
                scenarios().get("SP-DEMO-001");

        assertNotNull(scenario);

        assertEquals(
                ServiceProfitDisposition.DECLINED,
                scenario.input().disposition()
        );

        assertEquals(
                0,
                new BigDecimal("12400.00")
                        .compareTo(
                                scenario.input()
                                        .recommendedAmount()
                        )
        );

        assertEquals(
                "INR",
                scenario.input().currencyCode()
        );

        assertFalse(
                scenario.input().completedWorkEvidence()
        );

        assertEquals("Arjun Mehta", scenario.context().customerDisplayName());
        assertEquals("CUST-001", scenario.context().customerReference());
        assertTrue(scenario.context().customerContactable());
        assertEquals("KA01AV1001", scenario.context().vehicleRegistration());
        assertEquals("VINDEMO00000000001", scenario.context().vehicleVin());
        assertEquals("RO-1001", scenario.context().serviceOrderReference());
        assertEquals(
                "Front brake pad replacement",
                scenario.context().serviceDescription()
        );
    }

    @Test
    void mapsDeferredScenario() {

        var scenario =
                scenarios().get("SP-DEMO-002");

        assertEquals(
                ServiceProfitDisposition.DEFERRED,
                scenario.input().disposition()
        );

        assertEquals(
                0,
                new BigDecimal("28000.00")
                        .compareTo(
                                scenario.input()
                                        .recommendedAmount()
                        )
        );
    }

    @Test
    void mapsEvidenceDerivedScenarioWithoutExplicitDisposition() {

        var scenario =
                scenarios().get("SP-DEMO-003");

        assertNull(
                scenario.input().disposition()
        );

        assertEquals(
                "Front brake pads should be replaced.",
                scenario.input().recommendationText()
        );

        assertEquals(
                "Front pads low. Customer advised and will do next visit.",
                scenario.input().advisorNote()
        );
    }

    @Test
    void mapsLaterCompletionEvidenceForSuppressionScenario() {

        var scenario =
                scenarios().get("SP-DEMO-004");

        assertEquals(
                ServiceProfitDisposition.DECLINED,
                scenario.input().disposition()
        );

        assertTrue(
                scenario.input().completedWorkEvidence()
        );

        assertTrue(
                scenario.input().invoiceEvidence()
        );
    }

    @Test
    void mapsLifecycleDueScenario() {

        var scenario =
                scenarios().get("SP-DEMO-005");

        assertNotNull(
                scenario.input().nextServiceDueDate()
        );

        assertEquals(
                new BigDecimal("30000"),
                scenario.input().nextServiceDueMileage()
        );

        assertEquals("Vikram Shah",
                scenario.context().customerDisplayName());
        assertEquals("KA02AV2002", scenario.context().vehicleRegistration());
        assertEquals("2026-02-15", scenario.context().serviceDate().toString());
        assertNull(scenario.context().serviceOrderReference());
    }

    @Test
    void mapsLifecycleOverdueScenario() {

        var scenario =
                scenarios().get("SP-DEMO-006");

        assertEquals(
                "2026-04-10",
                scenario.input()
                        .nextServiceDueDate()
                        .toString()
        );
    }

    @Test
    void mapsInactiveCustomerActivity() {

        var scenario =
                scenarios().get("SP-DEMO-007");

        assertNotNull(
                scenario.input()
                        .lastCustomerActivityAt()
        );
    }

    @Test
    void invoiceDoesNotAutomaticallyMeanCompletedWork() {

        var scenario =
                scenarios().get("SP-DEMO-008");

        assertTrue(
                scenario.input().invoiceEvidence()
        );

        assertFalse(
                scenario.input().completedWorkEvidence()
        );
    }

    @Test
    void mapsAmbiguousIdentityScenarioWithoutCustomerId() {

        var scenario =
                scenarios().get("SP-DEMO-010");

        assertNull(
                scenario.input().customerId()
        );

        assertNotNull(
                scenario.input().vehicleId()
        );

        assertNull(scenario.context().customerDisplayName());
        assertNull(scenario.context().customerContactable());
        assertEquals("KA01AV9999", scenario.context().vehicleRegistration());
        assertEquals("RO-1099", scenario.context().serviceOrderReference());
    }

    private Map<String, ServiceProfitDemoDetectionScenario> scenarios() {

        return adapter.load(
                        datasetRoot(),
                        TENANT_ID
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                ServiceProfitDemoDetectionScenario::scenarioId,
                                Function.identity()
                        )
                );
    }

    private Path datasetRoot() {
        return Path.of(
                "..",
                "demo-data",
                "service-profit",
                "r1"
        ).toAbsolutePath().normalize();
    }
}