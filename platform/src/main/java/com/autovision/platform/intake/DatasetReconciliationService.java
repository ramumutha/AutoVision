package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DatasetReconciliationService {
    private final DatasetProcessingService datasetService;
    private final DatasetOperationalEvidenceService evidenceService;

    public DatasetReconciliationService(DatasetProcessingService datasetService,
            DatasetOperationalEvidenceService evidenceService) {
        this.datasetService = datasetService;
        this.evidenceService = evidenceService;
    }

    @Transactional(readOnly = true)
    public DatasetReconciliationResult reconstruct(UUID tenantId, UUID processingId) {
        DatasetProcessing processing = datasetService.requireDataset(tenantId, processingId);
        List<StagedSourceRecord> records = datasetService.recordsFor(tenantId, processingId);
        List<ValidationFinding> findings = datasetService.findingsFor(tenantId, processingId);
        List<PreStagingRejection> rejections = evidenceService.findRejections(tenantId, processingId);
        DatasetReconciliationSummary stored = evidenceService.findSummary(tenantId, processingId);
        int staged = count(records, record -> record.getState() == StagedRecordState.STAGED);
        int quarantined = count(records, record -> record.getState() == StagedRecordState.QUARANTINED);
        int eligible = count(records, StagedSourceRecord::isMaterializationEligible);
        int excluded = count(records, record -> !record.isMaterializationEligible());
        int received = stored == null ? records.size() + rejections.size() : stored.getRecordsReceived();
        int parsed = stored == null ? records.size() : stored.getRecordsParsed();
        int mapped = stored == null ? 0 : stored.getRecordsMapped();
        int detected = stored == null ? 0 : stored.getOpportunitiesDetected();
        int persisted = stored == null ? 0 : stored.getOpportunitiesPersisted();
        int duplicate = stored == null ? 0 : stored.getDuplicateNoOpCount();
        FindingSummary findingSummary = findingSummary(findings, rejections);
        boolean consistent = consistent(processing, staged, quarantined, eligible, detected, persisted);
        return new DatasetReconciliationResult(processing.getId(), processing.getTenantId(), processing.getDatasetId(),
                processing.getDatasetVersion(), processing.getStatus(), received, parsed, staged, quarantined, excluded,
                eligible, mapped, detected, persisted, duplicate, findingSummary,
                processing.getMaterializationAttemptCount(), processing.getMaterializationKey(),
                processing.getMaterializationVersion(), processing.getMaterializationStartedAt(),
                processing.getMaterializedAt(), processing.getMaterializationFailedAt(),
                processing.getMaterializationFailureStage(), processing.getMaterializationFailureCode(),
                processing.getMaterializationFailureReplayable(), consistent);
    }

    private FindingSummary findingSummary(List<ValidationFinding> findings, List<PreStagingRejection> rejections) {
        EnumMap<ValidationSeverity, Integer> severity = new EnumMap<>(ValidationSeverity.class);
        EnumMap<ValidationStage, Integer> stage = new EnumMap<>(ValidationStage.class);
        HashMap<String, Integer> code = new HashMap<>();
        HashMap<String, Integer> capability = new HashMap<>();
        findings.forEach(finding -> {
            severity.merge(finding.getSeverity(), 1, Integer::sum);
            stage.merge(finding.getValidationStage(), 1, Integer::sum);
            code.merge(finding.getFindingCode(), 1, Integer::sum);
            if (finding.getCapabilityAffected() != null) capability.merge(finding.getCapabilityAffected(), 1, Integer::sum);
        });
        rejections.forEach(rejection -> {
            severity.merge(rejection.getSeverity(), 1, Integer::sum);
            stage.merge(rejection.getValidationStage(), 1, Integer::sum);
            code.merge(rejection.getRejectionCode(), 1, Integer::sum);
        });
        return new FindingSummary(Map.copyOf(severity), Map.copyOf(stage), Map.copyOf(code),
                Map.copyOf(capability), rejections.size());
    }

    private boolean consistent(DatasetProcessing processing, int staged, int quarantined, int eligible,
            int detected, int persisted) {
        return staged >= 0 && quarantined >= 0 && eligible >= 0 && detected >= 0 && persisted >= 0
                && eligible <= staged && persisted <= detected
                && (processing.getStatus() != DatasetProcessingStatus.MATERIALIZED
                || processing.getMaterializationKey() != null && processing.getMaterializedAt() != null)
                && (processing.getStatus() != DatasetProcessingStatus.MATERIALIZATION_FAILED
                || processing.getMaterializedAt() == null);
    }

    private <T> int count(List<T> values, Function<T, Boolean> predicate) {
        return (int) values.stream().filter(predicate::apply).count();
    }
}