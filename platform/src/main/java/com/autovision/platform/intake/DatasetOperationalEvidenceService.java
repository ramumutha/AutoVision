package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DatasetOperationalEvidenceService {
    private final DatasetProcessingRepository processingRepository;
    private final DatasetReconciliationSummaryRepository summaryRepository;
    private final PreStagingRejectionRepository rejectionRepository;
    private final DatasetOperationalEventRepository eventRepository;

    public DatasetOperationalEvidenceService(
            DatasetProcessingRepository processingRepository,
            DatasetReconciliationSummaryRepository summaryRepository,
            PreStagingRejectionRepository rejectionRepository,
            DatasetOperationalEventRepository eventRepository) {
        this.processingRepository = processingRepository;
        this.summaryRepository = summaryRepository;
        this.rejectionRepository = rejectionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public DatasetReconciliationSummary recordSummary(UUID tenantId, DatasetReconciliationSummary summary) {
        requireTenant(tenantId, summary.getTenantId());
        requireProcessing(tenantId, summary.getDatasetProcessingId());
        return summaryRepository.save(summary);
    }

    @Transactional
    public DatasetReconciliationSummary updateMaterializationSummary(UUID tenantId, DatasetProcessing processing,
            int recordsMapped, int opportunitiesDetected, int opportunitiesPersisted, int duplicateNoOpCount,
            OffsetDateTime observedAt) {
        requireTenant(tenantId, processing.getTenantId());
        requireProcessing(tenantId, processing.getId());
        DatasetReconciliationSummary summary = summaryRepository
                .findByTenantIdAndDatasetProcessingId(tenantId, processing.getId()).orElse(null);
        if (summary == null) {
            summary = DatasetReconciliationSummary.record(UUID.randomUUID(), processing,
                    new ReconciliationCounts(0, 0, 0, 0, 0, 0, recordsMapped, opportunitiesDetected,
                            opportunitiesPersisted, duplicateNoOpCount), 0, 0, 0, 0, observedAt);
        } else {
            summary.updateMaterialization(recordsMapped, opportunitiesDetected, opportunitiesPersisted,
                    duplicateNoOpCount, processing, observedAt);
        }
        return summaryRepository.save(summary);
    }

    @Transactional
    public DatasetReconciliationSummary updateLifecycleSummary(UUID tenantId, DatasetProcessing processing,
            OffsetDateTime observedAt) {
        requireTenant(tenantId, processing.getTenantId());
        requireProcessing(tenantId, processing.getId());
        DatasetReconciliationSummary summary = summaryRepository
                .findByTenantIdAndDatasetProcessingId(tenantId, processing.getId()).orElse(null);
        if (summary != null) {
            summary.updateLifecycle(processing, observedAt);
            return summaryRepository.save(summary);
        }
        return null;
    }

    @Transactional
    public PreStagingRejection recordPreStagingRejection(UUID tenantId, PreStagingRejection rejection) {
        requireTenant(tenantId, rejection.getTenantId());
        requireProcessing(tenantId, rejection.getDatasetProcessingId());
        return rejectionRepository.save(rejection);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DatasetOperationalEvent recordEvent(UUID tenantId, DatasetOperationalEvent event) {
        requireTenant(tenantId, event.getTenantId());
        requireProcessing(tenantId, event.getDatasetProcessingId());
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public DatasetReconciliationSummary findSummary(UUID tenantId, UUID processingId) {
        requireProcessing(tenantId, processingId);
        return summaryRepository.findByTenantIdAndDatasetProcessingId(tenantId, processingId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PreStagingRejection> findRejections(UUID tenantId, UUID processingId) {
        requireProcessing(tenantId, processingId);
        return rejectionRepository.findByTenantIdAndDatasetProcessingIdOrderByOccurredAtAscId(tenantId, processingId);
    }

    @Transactional(readOnly = true)
    public List<DatasetOperationalEvent> findEvents(UUID tenantId, UUID processingId) {
        requireProcessing(tenantId, processingId);
        return eventRepository.findByTenantIdAndDatasetProcessingIdOrderByOccurredAtAscId(tenantId, processingId);
    }

    private void requireProcessing(UUID tenantId, UUID processingId) {
        requireTenant(tenantId, tenantId);
        if (processingId == null || processingRepository.findByIdAndTenantId(processingId, tenantId).isEmpty()) {
            throw new IllegalArgumentException("Dataset processing is not available for tenant");
        }
    }

    private static void requireTenant(UUID expected, UUID actual) {
        if (expected == null || actual == null || !expected.equals(actual)) {
            throw new IllegalArgumentException("Tenant containment check failed");
        }
    }
}