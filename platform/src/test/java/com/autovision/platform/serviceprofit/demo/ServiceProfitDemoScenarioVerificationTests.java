package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.detection.EvidenceDerivedServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.ExplicitServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.LifecycleServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionOrchestrator;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionResult;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidencePhraseClassifier;
import com.autovision.platform.serviceprofit.detection.ServiceProfitOpportunityKeyFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProfitDemoScenarioVerificationTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private static final OffsetDateTime EVALUATED_AT =
            OffsetDateTime.parse(
                    "2026-08-22T10:00:00+05:30"
            );

    private final ServiceProfitDemoDetectionInputAdapter adapter =
            new ServiceProfitDemoDetectionInputAdapter();

    private final ServiceProfitOpportunityKeyFactory keyFactory =
            new ServiceProfitOpportunityKeyFactory();

    private final ServiceProfitDetectionOrchestrator orchestrator =
            new ServiceProfitDetectionOrchestrator(
                    new ExplicitServiceProfitDetectionPolicy(
                            keyFactory
                    ),
                    new EvidenceDerivedServiceProfitDetectionPolicy(
                            new ServiceProfitEvidencePhraseClassifier(),
                            keyFactory
                    ),
                    new LifecycleServiceProfitDetectionPolicy(
                            keyFactory
                    )
            );

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void allTenDemoScenariosProduceExpectedDetectionOutcomes()
            throws Exception {

        Map<String, ServiceProfitDemoDetectionScenario> scenarios =
                loadScenarios();

        JsonNode manifest =
                objectMapper.readTree(
                        Files.readString(
                                datasetRoot()
                                        .resolve("manifest.json")
                        )
                );

        JsonNode expectedScenarios =
                manifest.get("scenarios");

        assertNotNull(expectedScenarios);
        assertEquals(10, expectedScenarios.size());

        for (JsonNode expected : expectedScenarios) {

            String scenarioId =
                    expected.get("id").asText();

            ServiceProfitDemoDetectionScenario scenario =
                    scenarios.get(scenarioId);

            assertNotNull(
                    scenario,
                    "Missing adapter scenario " + scenarioId
            );

            ServiceProfitDetectionResult result =
                    orchestrator.detect(
                            scenario.input(),
                            EVALUATED_AT
                    );

            assertTrue(
                    result.detected(),
                    "Expected detection for " + scenarioId
            );

            assertEquals(
                    ServiceProfitOpportunityType.valueOf(
                            expected.get(
                                    "expectedOpportunityType"
                            ).asText()
                    ),
                    result.opportunityType(),
                    scenarioId + " opportunity type"
            );

            assertEquals(
                    ServiceProfitEvidenceClass.valueOf(
                            expected.get(
                                    "expectedEvidenceClass"
                            ).asText()
                    ),
                    result.evidenceClass(),
                    scenarioId + " evidence class"
            );

            assertEquals(
                    ServiceProfitEvidenceStrength.valueOf(
                            expected.get(
                                    "expectedEvidenceStrength"
                            ).asText()
                    ),
                    result.evidenceStrength(),
                    scenarioId + " evidence strength"
            );

            if (expected.has(
                    "expectedActionability"
            )) {
                assertEquals(
                        ServiceProfitActionability.valueOf(
                                expected.get(
                                        "expectedActionability"
                                ).asText()
                        ),
                        result.actionability(),
                        scenarioId + " actionability"
                );
            }

            if (expected.has(
                    "expectedPotentialAmount"
            )) {
                BigDecimal expectedAmount =
                        expected.get(
                                "expectedPotentialAmount"
                        ).decimalValue();

                assertNotNull(
                        result.potentialAmount(),
                        scenarioId + " potential amount"
                );

                assertEquals(
                        0,
                        expectedAmount.compareTo(
                                result.potentialAmount()
                        ),
                        scenarioId + " potential amount"
                );
            }

            if (expected.has(
                    "currencyCode"
            )) {
                assertEquals(
                        expected.get(
                                "currencyCode"
                        ).asText(),
                        result.currencyCode(),
                        scenarioId + " currency"
                );
            }

            if ("SUPPRESS_ALREADY_COMPLETED".equals(
                    expected.get(
                            "expectedOutcome"
                    ).asText()
            )) {
                assertTrue(
                        result.suppressed(),
                        scenarioId
                                + " should be suppressed"
                );
            }
        }
    }

    private Map<String, ServiceProfitDemoDetectionScenario>
    loadScenarios() {

        List<ServiceProfitDemoDetectionScenario> loaded =
                adapter.load(
                        datasetRoot(),
                        TENANT_ID
                );

        Map<String, ServiceProfitDemoDetectionScenario> result =
                new HashMap<>();

        for (ServiceProfitDemoDetectionScenario scenario
                : loaded) {
            result.put(
                    scenario.scenarioId(),
                    scenario
            );
        }

        return result;
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