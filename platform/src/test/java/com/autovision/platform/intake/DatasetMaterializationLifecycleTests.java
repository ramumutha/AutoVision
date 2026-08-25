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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class DatasetMaterializationLifecycleTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T08:00:00Z");
    private static final UUID PRINCIPAL_ID = UUID.randomUUID();

    @Autowired
    private DatasetProcessingRepository datasetRepository;

    @Autowired
    private StagedSourceRecordRepository recordRepository;

    @Autowired
    private ValidationFindingRepository findingRepository;

    @Autowired
    private DatasetProcessingService service;

    @AfterEach
    void cleanup() {
        findingRepository.deleteAll();
        recordRepository.deleteAll();
        datasetRepository.deleteAll();
    }

    @Test
    void validStagedDatasetCanProgressAndSuccessIsIdempotent() {
        DatasetProcessing processing = persistedDataset("READY-1");
        StagedSourceRecord record = service.stageRecord(
                processing.getTenantId(), processing.getId(), StagedRecordType.CUSTOMER,
                "CUSTOMER-1", null, "1", "hash-1",
                new ObjectMapper().createObjectNode(), NOW
        );
        service.markRecordPassed(processing.getTenantId(), processing.getId(), record.getId());

        service.markReadyForMaterialization(processing.getTenantId(), processing.getId(), NOW.plusMinutes(1));
        service.beginMaterialization(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                UUID.randomUUID(), NOW.plusMinutes(2)
        );
        service.markMaterialized(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                NOW.plusMinutes(3)
        );
        DatasetProcessing repeated = service.markMaterialized(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                NOW.plusMinutes(4)
        );

        assertEquals(DatasetProcessingStatus.MATERIALIZED, repeated.getStatus());
        assertEquals(NOW.plusMinutes(3), repeated.getMaterializedAt());
        assertEquals(1, repeated.getMaterializationAttemptCount());
    }

    @Test
    void mixedDatasetCanBecomeReadyWhenQuarantinedRecordsAreExcluded() {
        DatasetProcessing processing = persistedDataset("READY-2");
        StagedSourceRecord safe = service.stageRecord(
                processing.getTenantId(), processing.getId(), StagedRecordType.CUSTOMER,
                "CUSTOMER-SAFE", null, "1", null,
                new ObjectMapper().createObjectNode(), NOW
        );
        service.markRecordPassed(processing.getTenantId(), processing.getId(), safe.getId());
        StagedSourceRecord record = service.stageRecord(
                processing.getTenantId(), processing.getId(), StagedRecordType.CUSTOMER,
                "CUSTOMER-2", null, "1", null,
                new ObjectMapper().createObjectNode(), NOW
        );
        service.quarantineRecord(processing.getTenantId(), processing.getId(), record.getId(), NOW.plusMinutes(1));

        service.markReadyForMaterialization(processing.getTenantId(), processing.getId(), NOW.plusMinutes(2));
        assertEquals(
                DatasetProcessingStatus.READY_FOR_MATERIALIZATION,
                service.requireDataset(processing.getTenantId(), processing.getId()).getStatus()
        );
    }

    @Test
    void failureIsDistinctAuditableAndRetryableWithSameIdentity() {
        DatasetProcessing processing = persistedDataset("READY-3");
        StagedSourceRecord record = service.stageRecord(
                processing.getTenantId(), processing.getId(), StagedRecordType.CUSTOMER,
                "CUSTOMER-3", null, "1", null,
                new ObjectMapper().createObjectNode(), NOW
        );
        service.markRecordPassed(processing.getTenantId(), processing.getId(), record.getId());
        service.markReadyForMaterialization(processing.getTenantId(), processing.getId(), NOW.plusMinutes(1));
        UUID correlationId = UUID.randomUUID();
        service.beginMaterialization(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                correlationId, NOW.plusMinutes(2)
        );
        service.markMaterializationFailed(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                "Opportunity persistence failed", NOW.plusMinutes(3)
        );

        DatasetProcessing failed = service.requireDataset(processing.getTenantId(), processing.getId());
        assertEquals(DatasetProcessingStatus.MATERIALIZATION_FAILED, failed.getStatus());
        assertEquals("Opportunity persistence failed", failed.getMaterializationFailureReason());
        assertEquals(NOW.plusMinutes(3), failed.getMaterializationFailedAt());
        assertEquals(1, failed.getMaterializationAttemptCount());

        service.beginMaterialization(
                processing.getTenantId(), processing.getId(), "dataset-materialization", "1",
                correlationId, NOW.plusMinutes(4)
        );
        assertEquals(
                2,
                service.requireDataset(processing.getTenantId(), processing.getId())
                        .getMaterializationAttemptCount()
        );
    }

    @Test
    void invalidTransitionsAndIdentityChangesAreRejected() {
        DatasetProcessing processing = persistedDataset("READY-4");

        assertThrows(
                IllegalStateException.class,
                () -> service.beginMaterialization(
                        processing.getTenantId(), processing.getId(), "key", "1",
                        UUID.randomUUID(), NOW
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> service.markMaterialized(
                        processing.getTenantId(), processing.getId(), "key", "1", NOW
                )
        );
    }

    private DatasetProcessing persistedDataset(String datasetId) {
        DatasetProcessing processing = DatasetProcessing.receive(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                datasetId, "1", "controlled-test", null, "2026-01",
                NOW, null, null, "SHA-256", "checksum", 10L,
                UUID.randomUUID()
        );
        return datasetRepository.saveAndFlush(processing);
    }
}
