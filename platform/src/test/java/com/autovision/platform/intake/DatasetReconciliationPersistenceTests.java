package com.autovision.platform.intake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class DatasetReconciliationPersistenceTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T08:00:00Z");

    @Autowired DatasetProcessingRepository processingRepository;
    @Autowired StagedSourceRecordRepository recordRepository;
    @Autowired ValidationFindingRepository findingRepository;
    @Autowired DatasetReconciliationSummaryRepository summaryRepository;
    @Autowired PreStagingRejectionRepository rejectionRepository;
    @Autowired DatasetReconciliationService reconciliationService;
    @Autowired DatasetOperationalEvidenceService evidenceService;
    @Autowired DatasetProcessingService processingService;

    @AfterEach
    void cleanup() {
        rejectionRepository.deleteAll();
        summaryRepository.deleteAll();
        findingRepository.deleteAll();
        recordRepository.deleteAll();
        processingRepository.deleteAll();
    }

    @Test
    void reconstructsOperationalStateFromDurablePersistenceWithoutPayload() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = DatasetProcessing.receive(UUID.randomUUID(), tenantId, null, null,
                "dataset-reconcile", "1", "controlled", null, "schema", NOW, null, null,
                "SHA-256", "checksum", 12L, UUID.randomUUID());
        processingRepository.saveAndFlush(processing);
        StagedSourceRecord eligible = processingService.stageRecord(tenantId, processing.getId(),
                StagedRecordType.SERVICE_ORDER, "order-1", null, "1", null,
                new ObjectMapper().createObjectNode().put("email", "hidden@example.invalid"), NOW);
        processingService.markRecordPassed(tenantId, processing.getId(), eligible.getId());
        StagedSourceRecord quarantined = processingService.stageRecord(tenantId, processing.getId(),
                StagedRecordType.CUSTOMER, "customer-1", null, "1", null,
                new ObjectMapper().createObjectNode(), NOW);
        processingService.quarantineRecord(tenantId, processing.getId(), quarantined.getId(), NOW);
        processingService.attachFinding(tenantId, processing.getId(), eligible.getId(), ValidationStage.SCHEMA,
                ValidationSeverity.WARNING, "WARN_CODE", "order.id", "safe warning", "DETECTION", NOW);
        evidenceService.recordPreStagingRejection(tenantId, PreStagingRejection.record(UUID.randomUUID(), processing,
                StagedRecordType.VEHICLE, null, ValidationStage.ENVELOPE, ValidationSeverity.ERROR,
                "MISSING_SOURCE_ID", "vehicle.sourceRecordId", NOW));
        evidenceService.recordSummary(tenantId, DatasetReconciliationSummary.record(UUID.randomUUID(), processing,
                new ReconciliationCounts(3, 3, 2, 1, 0, 1, 1, 1, 1, 0), 0, 1, 1, 0, NOW));

        DatasetReconciliationResult result = reconciliationService.reconstruct(tenantId, processing.getId());

        assertEquals("dataset-reconcile", result.datasetId());
        assertEquals(3, result.recordsReceived());
        assertEquals(3, result.recordsParsed());
        assertEquals(1, result.recordsStaged());
        assertEquals(1, result.recordsQuarantined());
        assertEquals(1, result.recordsEligible());
        assertEquals(1, result.findings().preStagingRejectionCount());
        assertEquals(1, result.findings().bySeverity().get(ValidationSeverity.WARNING));
        assertEquals(1, result.findings().bySeverity().get(ValidationSeverity.ERROR));
        assertEquals(1, result.findings().byCode().get("MISSING_SOURCE_ID"));
        assertFalse(result.toString().contains("hidden@example.invalid"));
    }
}
