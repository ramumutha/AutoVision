package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextService;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionOrchestrator;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionPersistenceService;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionResult;
import com.autovision.platform.serviceprofit.detection.ServiceProfitPersistenceResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceProfitDemoMaterializer {

    private final ObjectMapper objectMapper;
    private final ServiceProfitDemoDetectionInputAdapter inputAdapter;
    private final ServiceProfitDetectionOrchestrator orchestrator;
    private final ServiceProfitDetectionPersistenceService persistenceService;
        private final ServiceProfitOpportunityContextService contextService;

    public ServiceProfitDemoMaterializer(
            ObjectMapper objectMapper,
            ServiceProfitDemoDetectionInputAdapter inputAdapter,
            ServiceProfitDetectionOrchestrator orchestrator,
            ServiceProfitDetectionPersistenceService persistenceService,
            ServiceProfitOpportunityContextService contextService
    ) {
        this.objectMapper = objectMapper;
        this.inputAdapter = inputAdapter;
        this.orchestrator = orchestrator;
        this.persistenceService = persistenceService;
        this.contextService = contextService;
    }

    public ServiceProfitDemoMaterializationResult materialize(
            Path datasetRoot,
            UUID principalId,
            OffsetDateTime evaluatedAt
    ) {
        requirePath(datasetRoot);
        requireId(principalId);
        requireTime(evaluatedAt);

        UUID tenantId = readTenantId(datasetRoot);

        List<ServiceProfitDemoDetectionScenario> scenarios =
                inputAdapter.load(datasetRoot, tenantId);

        List<ServiceProfitDemoMaterializationResult.ScenarioOutcome> outcomes =
                new ArrayList<>();

        for (ServiceProfitDemoDetectionScenario scenario : scenarios) {
            ServiceProfitDetectionResult detectionResult =
                    orchestrator.detect(
                            scenario.input(),
                            evaluatedAt
                    );

            ServiceProfitPersistenceResult persistenceResult =
                    persistenceService.persist(
                            detectionResult,
                            principalId,
                            evaluatedAt
                    );

            if (persistenceResult.opportunity() != null
                    && scenario.context() != null) {
                contextService.capture(
                        persistenceResult.opportunity(),
                        scenario.context(),
                        evaluatedAt
                );
            }

            outcomes.add(
                    new ServiceProfitDemoMaterializationResult.ScenarioOutcome(
                            scenario.scenarioId(),
                            persistenceResult.outcome()
                    )
            );
        }

        return ServiceProfitDemoMaterializationResult.from(outcomes);
    }

    private UUID readTenantId(
            Path datasetRoot
    ) {
        Path manifestPath = datasetRoot.resolve("manifest.json");

        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException(
                    "Service Profit demo manifest does not exist: "
                            + manifestPath
            );
        }

        try {
            ServiceProfitDemoManifest manifest =
                    objectMapper.readValue(
                            manifestPath.toFile(),
                            ServiceProfitDemoManifest.class
                    );

            if (manifest.tenantId() == null) {
                throw new IllegalArgumentException(
                        "Demo tenant ID is required"
                );
            }

            return manifest.tenantId();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to read Service Profit demo manifest",
                    exception
            );
        }
    }

    private void requirePath(
            Path datasetRoot
    ) {
        if (datasetRoot == null) {
            throw new IllegalArgumentException(
                    "Demo dataset root is required"
            );
        }
    }

    private void requireId(
            UUID principalId
    ) {
        if (principalId == null) {
            throw new IllegalArgumentException(
                    "Principal ID is required"
            );
        }
    }

    private void requireTime(
            OffsetDateTime evaluatedAt
    ) {
        if (evaluatedAt == null) {
            throw new IllegalArgumentException(
                    "Demo materialization time is required"
            );
        }
    }
}