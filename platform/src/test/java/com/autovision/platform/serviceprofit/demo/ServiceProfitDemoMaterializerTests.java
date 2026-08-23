package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunity;
import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContext;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextRepository;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextService;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextSnapshot;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityRepository;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityStatus;
import com.autovision.platform.serviceprofit.detection.EvidenceDerivedServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.ExplicitServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.LifecycleServiceProfitDetectionPolicy;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionOrchestrator;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionPersistenceService;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionResult;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidencePhraseClassifier;
import com.autovision.platform.serviceprofit.detection.ServiceProfitOpportunityKeyFactory;
import com.autovision.platform.serviceprofit.detection.ServiceProfitPersistenceOutcome;
import com.autovision.platform.serviceprofit.detection.ServiceProfitPersistenceResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitDemoMaterializerTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private static final UUID PRINCIPAL_ID =
            UUID.fromString(
                    "20000000-0000-4000-8000-000000000001"
            );

    private static final OffsetDateTime EVALUATED_AT =
            OffsetDateTime.parse(
                    "2026-08-22T10:00:00+05:30"
            );

    @Test
    void delegatesEveryScenarioAndSummarizesPersistenceOutcomes() {
        ServiceProfitDemoDetectionInputAdapter adapter =
                mock(ServiceProfitDemoDetectionInputAdapter.class);

        ServiceProfitDetectionOrchestrator orchestrator =
                mock(ServiceProfitDetectionOrchestrator.class);

        ServiceProfitDetectionPersistenceService persistenceService =
                mock(ServiceProfitDetectionPersistenceService.class);

        ServiceProfitDetectionResult detectionResult =
                mock(ServiceProfitDetectionResult.class);

        ServiceProfitOpportunityContextService contextService =
                mock(ServiceProfitOpportunityContextService.class);

        List<ServiceProfitDemoDetectionScenario> scenarios =
                java.util.stream.IntStream.rangeClosed(1, 10)
                        .mapToObj(index ->
                                new ServiceProfitDemoDetectionScenario(
                                        "SP-DEMO-%03d".formatted(index),
                                        mock(
                                                com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionInput.class
                                        ),
                                        mock(ServiceProfitOpportunityContextSnapshot.class)
                                )
                        )
                        .toList();

        when(adapter.load(datasetRoot(), TENANT_ID))
                .thenReturn(scenarios);

        when(orchestrator.detect(any(), any()))
                .thenReturn(detectionResult);

        when(persistenceService.persist(any(), any(), any()))
                .thenReturn(
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(
                                ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED
                        ),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED),
                        mockResult(ServiceProfitPersistenceOutcome.EXISTING),
                        mockResult(ServiceProfitPersistenceOutcome.NO_MATCH),
                        mockResult(ServiceProfitPersistenceOutcome.CREATED)
                );

        ServiceProfitDemoMaterializer materializer =
                new ServiceProfitDemoMaterializer(
                        new ObjectMapper(),
                        adapter,
                        orchestrator,
                        persistenceService,
                        contextService
                );

        ServiceProfitDemoMaterializationResult result =
                materializer.materialize(
                        datasetRoot(),
                        PRINCIPAL_ID,
                        EVALUATED_AT
                );

        assertEquals(10, result.totalScenariosEvaluated());
        assertEquals(7, result.createdOpportunities());
        assertEquals(1, result.createdSuppressedOpportunities());
        assertEquals(1, result.existingOpportunities());
        assertEquals(1, result.noMatchScenarios());
        assertEquals(
                ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED,
                result.scenarioOutcomes().get(3).outcome()
        );

        verify(orchestrator, times(10))
                .detect(any(), any());

        verify(persistenceService, times(10))
                .persist(
                        any(),
                        org.mockito.ArgumentMatchers.eq(PRINCIPAL_ID),
                        org.mockito.ArgumentMatchers.eq(EVALUATED_AT)
                );

        verify(contextService, times(9))
                .capture(any(), any(), any());
    }

    @Test
    void materializesFrozenDatasetIdempotentlyThroughProductionPipeline() {
        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        Map<String, ServiceProfitOpportunity> persisted =
                new LinkedHashMap<>();

        Map<UUID, ServiceProfitOpportunityContext> contexts =
                new LinkedHashMap<>();

        when(repository.findByTenantIdAndOpportunityKey(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        persisted.get(
                                storageKey(
                                        invocation.getArgument(0),
                                        invocation.getArgument(1)
                                )
                        )
                ));

        when(repository.save(any()))
                .thenAnswer(invocation -> {
                    ServiceProfitOpportunity opportunity =
                            invocation.getArgument(0);

                    persisted.put(
                            storageKey(
                                    opportunity.getTenantId(),
                                    opportunity.getOpportunityKey()
                            ),
                            opportunity
                    );

                    return opportunity;
                });

        ServiceProfitOpportunityKeyFactory keyFactory =
                new ServiceProfitOpportunityKeyFactory();

        ServiceProfitDemoDetectionInputAdapter adapter =
                new ServiceProfitDemoDetectionInputAdapter();

        ServiceProfitOpportunityContextRepository contextRepository =
                mock(ServiceProfitOpportunityContextRepository.class);

        when(contextRepository.findByOpportunityIdAndTenantId(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        contexts.get(invocation.getArgument(0))
                ));

        when(contextRepository.save(any()))
                .thenAnswer(invocation -> {
                    ServiceProfitOpportunityContext context =
                            invocation.getArgument(0);
                    contexts.put(context.getOpportunityId(), context);
                    return context;
                });

        ServiceProfitDemoMaterializer materializer =
                new ServiceProfitDemoMaterializer(
                        new ObjectMapper(),
                        adapter,
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
                        ),
                        new ServiceProfitDetectionPersistenceService(
                                repository
                        ),
                        new ServiceProfitOpportunityContextService(
                                contextRepository
                        )
                );

        ServiceProfitDemoMaterializationResult first =
                materializer.materialize(
                        datasetRoot(),
                        PRINCIPAL_ID,
                        EVALUATED_AT
                );

        assertEquals(10, first.totalScenariosEvaluated());
        assertEquals(9, first.createdOpportunities());
        assertEquals(1, first.createdSuppressedOpportunities());
        assertEquals(0, first.existingOpportunities());
        assertEquals(0, first.noMatchScenarios());
        assertEquals(10, persisted.size());
        assertEquals(10, contexts.size());
        assertEquals(
                ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED,
                outcomeFor(first, "SP-DEMO-004")
        );

        Map<String, ServiceProfitDemoDetectionScenario> scenarios =
                adapter.load(datasetRoot(), TENANT_ID).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ServiceProfitDemoDetectionScenario::scenarioId,
                                scenario -> scenario
                        ));

        ServiceProfitOpportunity suppressedOpportunity =
                opportunityFor(
                        scenarios.get("SP-DEMO-004"),
                        persisted
                );

        ServiceProfitOpportunity reviewOpportunity =
                opportunityFor(
                        scenarios.get("SP-DEMO-010"),
                        persisted
                );

        assertEquals(
                ServiceProfitOpportunityStatus.SUPPRESSED,
                suppressedOpportunity.getStatus()
        );

        assertEquals(
                ServiceProfitActionability.REVIEW_REQUIRED,
                reviewOpportunity.getActionability()
        );

        ServiceProfitOpportunityContext suppressedContext =
                contexts.get(suppressedOpportunity.getId());
        ServiceProfitOpportunityContext reviewContext =
                contexts.get(reviewOpportunity.getId());

        assertNotNull(suppressedContext);
        assertNotNull(reviewContext);
        assertEquals(
                "RO-1004",
                suppressedContext.getServiceOrderReference()
        );
        assertEquals(
                "KA01AV9999",
                reviewContext.getVehicleRegistration()
        );

        ServiceProfitDemoMaterializationResult second =
                materializer.materialize(
                        datasetRoot(),
                        PRINCIPAL_ID,
                        EVALUATED_AT
                );

        assertEquals(10, second.totalScenariosEvaluated());
        assertEquals(0, second.createdOpportunities());
        assertEquals(0, second.createdSuppressedOpportunities());
        assertEquals(10, second.existingOpportunities());
        assertEquals(0, second.noMatchScenarios());
        assertEquals(10, persisted.size());
        assertEquals(10, contexts.size());
        assertEquals(
                suppressedContext.getId(),
                contexts.get(suppressedOpportunity.getId()).getId()
        );
        assertEquals(
                reviewContext.getId(),
                contexts.get(reviewOpportunity.getId()).getId()
        );
        assertEquals(
                ServiceProfitOpportunityStatus.SUPPRESSED,
                suppressedOpportunity.getStatus()
        );
        assertEquals(
                ServiceProfitActionability.REVIEW_REQUIRED,
                reviewOpportunity.getActionability()
        );
        assertNotNull(reviewOpportunity.getUpdatedAt());

        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAllInBatch();
    }

    private ServiceProfitPersistenceResult mockResult(
            ServiceProfitPersistenceOutcome outcome
    ) {
        return new ServiceProfitPersistenceResult(
                outcome,
                outcome == ServiceProfitPersistenceOutcome.NO_MATCH
                        ? null
                        : mock(ServiceProfitOpportunity.class)
        );
    }

        private ServiceProfitPersistenceOutcome outcomeFor(
                        ServiceProfitDemoMaterializationResult result,
                        String scenarioId
        ) {
                return result.scenarioOutcomes().stream()
                                .filter(outcome -> scenarioId.equals(outcome.scenarioId()))
                                .findFirst()
                                .orElseThrow()
                                .outcome();
        }

        private String storageKey(
                        UUID tenantId,
                        String opportunityKey
        ) {
                return tenantId + ":" + opportunityKey;
        }

            private ServiceProfitOpportunity opportunityFor(
                    ServiceProfitDemoDetectionScenario scenario,
                    Map<String, ServiceProfitOpportunity> persisted
            ) {
                return persisted.values().stream()
                        .filter(opportunity ->
                                scenario.input().sourceEntityId()
                                        .equals(opportunity.getSourceEntityId())
                        )
                        .findFirst()
                        .orElseThrow();
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