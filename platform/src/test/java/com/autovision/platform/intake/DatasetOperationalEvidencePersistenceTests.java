package com.autovision.platform.intake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class DatasetOperationalEvidencePersistenceTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T08:00:00Z");

    @Autowired DatasetProcessingRepository processingRepository;
    @Autowired DatasetReconciliationSummaryRepository summaryRepository;
    @Autowired PreStagingRejectionRepository rejectionRepository;
    @Autowired DatasetOperationalEventRepository eventRepository;
    @Autowired DatasetOperationalEvidenceService evidenceService;

    @AfterEach
    void cleanup() {
        eventRepository.deleteAll();
        rejectionRepository.deleteAll();
        summaryRepository.deleteAll();
        processingRepository.deleteAll();
    }

    @Test
    void persistsSummaryAndReloadsOnlyForOwningTenant() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = processing(tenantId);
        processingRepository.saveAndFlush(processing);

        DatasetReconciliationSummary summary = DatasetReconciliationSummary.record(
                UUID.randomUUID(), processing, new ReconciliationCounts(10, 9, 8, 1, 0, 8, 7, 4, 3, 1),
                1, 2, 3, 4, NOW);
        evidenceService.recordSummary(tenantId, summary);

        assertEquals(8, evidenceService.findSummary(tenantId, processing.getId()).getRecordsStaged());
        assertEquals(3, evidenceService.findSummary(tenantId, processing.getId()).getOpportunitiesPersisted());
        assertThrows(IllegalArgumentException.class,
                () -> evidenceService.findSummary(UUID.randomUUID(), processing.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> DatasetReconciliationSummary.record(UUID.randomUUID(), processing,
                        new ReconciliationCounts(1, 2, 0, 0, 0, 0, 0, 0, 0, 0), 0, 0, 0, 0, NOW));
    }

    @Test
    void recordsSafePreStagingRejectionAndOrderedOperationalEvents() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = processing(tenantId);
        processingRepository.saveAndFlush(processing);

        evidenceService.recordPreStagingRejection(tenantId, PreStagingRejection.record(
                UUID.randomUUID(), processing, StagedRecordType.CUSTOMER, "C-1", ValidationStage.SCHEMA,
                ValidationSeverity.ERROR, "REQUIRED_FIELD_MISSING", "customer.id", NOW));
        evidenceService.recordEvent(tenantId, DatasetOperationalEvent.record(
                UUID.randomUUID(), processing, DatasetOperationalEventType.REPLAY_REQUESTED,
                null, null, null, null, "worker-1", "REPLAY_REQUESTED", "manual request", NOW.plusSeconds(1)));

        PreStagingRejection rejection = evidenceService.findRejections(tenantId, processing.getId()).getFirst();
        DatasetOperationalEvent event = evidenceService.findEvents(tenantId, processing.getId()).getFirst();
        assertEquals("REQUIRED_FIELD_MISSING", rejection.getRejectionCode());
        assertEquals(ValidationSeverity.ERROR, rejection.getSeverity());
        assertEquals("worker-1", event.getApplicationId());
        assertFalse(event.toString().contains("manual request"));
    }

    @Test
    void storesStructuredFailureAttemptAndReplayability() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = processing(tenantId);
        processing.markStaged(NOW);
        processing.markReadyForMaterialization(NOW);
        processing.beginMaterialization("materialization-key", "v1", UUID.randomUUID(), NOW);
        processing.recordMaterializationFailure("materialization-key", "v1", "provider unavailable",
                MaterializationFailureStage.PERSISTENCE, MaterializationFailureCode.PROVIDER_UNAVAILABLE, true,
                NOW.plusSeconds(10));

        assertEquals(1, processing.getMaterializationFailureAttempt());
        assertEquals(MaterializationFailureCode.PROVIDER_UNAVAILABLE, processing.getMaterializationFailureCode());
        assertEquals(MaterializationFailureStage.PERSISTENCE, processing.getMaterializationFailureStage());
        assertEquals(Boolean.TRUE, processing.getMaterializationFailureReplayable());
    }

    private static DatasetProcessing processing(UUID tenantId) {
        return DatasetProcessing.receive(UUID.randomUUID(), tenantId, null, null, "DATASET-1", "1",
                "DMS-EXPORT", null, "source-v1", NOW, null, null, "SHA-256", "checksum", 10L,
                UUID.randomUUID());
    }
}