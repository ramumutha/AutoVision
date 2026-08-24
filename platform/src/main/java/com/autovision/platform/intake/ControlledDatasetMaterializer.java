package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextService;
import com.autovision.platform.serviceprofit.data.DealerDataAssessment;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionOrchestrator;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionPersistenceService;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionResult;
import com.autovision.platform.serviceprofit.detection.ServiceProfitPersistenceOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ControlledDatasetMaterializer {

    public static final String MATERIALIZATION_KEY_PREFIX = "SERVICE_PROFIT";
    public static final String MATERIALIZATION_VERSION = "R1-CANONICAL-MATERIALIZATION-1";

    private final DatasetProcessingService datasetService;
    private final ControlledDatasetMaterializationFailureService failureService;
    private final CanonicalIntakeMapper mapper;
    private final ControlledDatasetReadinessService readinessService;
    private final ServiceProfitDetectionOrchestrator orchestrator;
    private final ServiceProfitDetectionPersistenceService persistenceService;
    private final ServiceProfitOpportunityContextService contextService;

    public ControlledDatasetMaterializer(
            DatasetProcessingService datasetService,
            ControlledDatasetMaterializationFailureService failureService,
            CanonicalIntakeMapper mapper,
            ControlledDatasetReadinessService readinessService,
            ServiceProfitDetectionOrchestrator orchestrator,
            ServiceProfitDetectionPersistenceService persistenceService,
            ServiceProfitOpportunityContextService contextService
    ) {
        this.datasetService = datasetService;
        this.failureService = failureService;
        this.mapper = mapper;
        this.readinessService = readinessService;
        this.orchestrator = orchestrator;
        this.persistenceService = persistenceService;
        this.contextService = contextService;
    }

    @Transactional
    public ControlledDatasetMaterializationResult materialize(
            UUID tenantId,
            UUID datasetProcessingId,
            UUID principalId,
            OffsetDateTime evaluatedAt
    ) {
        require(tenantId, datasetProcessingId, principalId, evaluatedAt);
        DatasetProcessing processing = datasetService.requireDataset(tenantId, datasetProcessingId);
        List<StagedSourceRecord> records = datasetService.recordsFor(tenantId, datasetProcessingId);
        DealerDataAssessment assessment = readinessService.assess(processing, records, principalId, evaluatedAt);
        if (assessment == null) throw new IllegalStateException("Dataset readiness assessment is required");
        String key = MATERIALIZATION_KEY_PREFIX + ":" + processing.getDatasetId() + ":" + processing.getDatasetVersion();
        if (processing.getStatus() == DatasetProcessingStatus.MATERIALIZED) {
            return new ControlledDatasetMaterializationResult(processing.getStatus(), 0, 0, 0, 0, true);
        }
        if (processing.getStatus() == DatasetProcessingStatus.STAGED) {
            datasetService.markReadyForMaterialization(tenantId, datasetProcessingId, evaluatedAt);
        }
        datasetService.beginMaterialization(tenantId, datasetProcessingId, key, MATERIALIZATION_VERSION,
                UUID.randomUUID(), evaluatedAt);
        try {
            List<CanonicalIntakeDetectionCandidate> candidates = mapper.map(processing, records);
            int detected = 0;
            int created = 0;
            int existing = 0;
            for (CanonicalIntakeDetectionCandidate candidate : candidates) {
                ServiceProfitDetectionResult result = orchestrator.detect(candidate.input(), evaluatedAt);
                if (!result.detected()) continue;
                detected++;
                var persisted = persistenceService.persist(result, principalId, evaluatedAt);
                if (persisted.opportunity() != null) {
                    contextService.capture(persisted.opportunity(), candidate.context(), evaluatedAt);
                }
                if (persisted.outcome() == ServiceProfitPersistenceOutcome.EXISTING) existing++;
                if (persisted.outcome() == ServiceProfitPersistenceOutcome.CREATED
                        || persisted.outcome() == ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED) created++;
            }
            datasetService.markMaterialized(tenantId, datasetProcessingId, key, MATERIALIZATION_VERSION, evaluatedAt);
            return new ControlledDatasetMaterializationResult(DatasetProcessingStatus.MATERIALIZED,
                    candidates.size(), detected, created, existing, false);
        } catch (RuntimeException exception) {
                failureService.record(tenantId, datasetProcessingId, key, MATERIALIZATION_VERSION,
                    safeFailureReason(exception), evaluatedAt);
            throw exception;
        }
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Controlled materialization failed" : message;
    }

    private void require(UUID tenantId, UUID datasetProcessingId, UUID principalId, OffsetDateTime evaluatedAt) {
        if (tenantId == null || datasetProcessingId == null || principalId == null || evaluatedAt == null) {
            throw new IllegalArgumentException("Tenant, dataset, principal ID, and materialization time are required");
        }
    }
}
