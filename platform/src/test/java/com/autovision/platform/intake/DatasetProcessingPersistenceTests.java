package com.autovision.platform.intake;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DatasetProcessingPersistenceTests {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-08-24T08:00:00Z");

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
    void persistsFullDatasetWithNullableProviderAndTenantScopedLookup() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = dataset(tenantId, "DATASET-1", "1");

        datasetRepository.saveAndFlush(processing);

        DatasetProcessing persisted = datasetRepository
                .findByIdAndTenantId(processing.getId(), tenantId)
                .orElseThrow();

        assertEquals(DatasetDeliveryType.FULL, persisted.getDeliveryType());
        assertEquals(DatasetProcessingStatus.RECEIVED, persisted.getStatus());
        assertEquals(tenantId, persisted.getTenantId());
        assertEquals(null, persisted.getSourceProvider());
        assertTrue(datasetRepository
                .findByIdAndTenantId(processing.getId(), UUID.randomUUID())
                .isEmpty());
    }

    @Test
    void stagesSourceLineageAndDoesNotExposePayloadInOrdinaryRepresentation() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = dataset(tenantId, "DATASET-2", "1");
        datasetRepository.saveAndFlush(processing);
        ObjectNode payload = new ObjectMapper().createObjectNode();
        payload.put("customerEmail", "sensitive@example.invalid");

        StagedSourceRecord record = service.stageRecord(
                tenantId,
                processing.getId(),
                StagedRecordType.SERVICE_ORDER,
                "ORDER-1",
                "CUSTOMER-1",
                "source-v1",
                "hash-1",
                payload,
                NOW
        );

        StagedSourceRecord persisted = recordRepository
                .findByIdAndTenantId(record.getId(), tenantId)
                .orElseThrow();

        assertEquals("ORDER-1", persisted.getSourceRecordId());
        assertEquals("CUSTOMER-1", persisted.getSourceParentId());
        assertEquals(payload, persisted.getRawPayload());
        assertEquals(StagedRecordState.STAGED, persisted.getState());
        assertFalse(persisted.isMaterializationEligible());
        assertFalse(persisted.toString().contains("sensitive@example.invalid"));
        assertEquals(
                DatasetProcessingStatus.STAGED,
                datasetRepository.findById(processing.getId()).orElseThrow().getStatus()
        );

        service.stageRecord(
                tenantId,
                processing.getId(),
                StagedRecordType.SERVICE_JOB,
                "JOB-1",
                null,
                "source-v1",
                "hash-2",
                new ObjectMapper().createObjectNode(),
                NOW
        );
        assertEquals(2, recordRepository.findByTenantIdAndDatasetProcessingIdOrderByCreatedAtAsc(
                tenantId, processing.getId()
        ).size());
    }

    @Test
    void sameDatasetIdentityCannotBeStoredTwiceForTenant() {
        UUID tenantId = UUID.randomUUID();
        datasetRepository.saveAndFlush(dataset(tenantId, "DATASET-3", "1"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> datasetRepository.saveAndFlush(
                        dataset(tenantId, "DATASET-3", "1")
                )
        );
    }

    @Test
    void sameSourceRecordIdentityCannotBeStoredTwiceWithinDataset() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = dataset(tenantId, "DATASET-4", "1");
        datasetRepository.saveAndFlush(processing);
        recordRepository.saveAndFlush(record(
                tenantId,
                processing.getId(),
                "RECORD-1"
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> recordRepository.saveAndFlush(record(
                        tenantId,
                        processing.getId(),
                        "RECORD-1"
                ))
        );
    }

    @Test
    void persistsEveryContractSeverityAndFindingEvidence() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = dataset(tenantId, "DATASET-5", "1");
        datasetRepository.saveAndFlush(processing);
        StagedSourceRecord record = recordRepository.saveAndFlush(record(
                tenantId,
                processing.getId(),
                "RECORD-2"
        ));

        for (ValidationSeverity severity : ValidationSeverity.values()) {
            service.attachFinding(
                    tenantId,
                    processing.getId(),
                    record.getId(),
                    ValidationStage.SCHEMA,
                    severity,
                    "FIELD_RULE",
                    "serviceOrder.serviceDate",
                    "The service date requires review.",
                    "OPPORTUNITY_DETECTION",
                    NOW
            );
        }

        List<ValidationFinding> findings = service.findingsFor(
                tenantId,
                processing.getId()
        );

        assertEquals(4, findings.size());
        assertTrue(findings.stream().allMatch(
                finding -> finding.getStagedSourceRecordId().equals(record.getId())
        ));
        assertEquals(ValidationSeverity.FATAL, findings.getFirst().getSeverity());
    }

    @Test
    void quarantineRetainsLineageAndBlocksMaterialization() {
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = dataset(tenantId, "DATASET-6", "1");
        datasetRepository.saveAndFlush(processing);
        StagedSourceRecord record = service.stageRecord(
                tenantId,
                processing.getId(),
                StagedRecordType.CUSTOMER,
                "CUSTOMER-AMBIGUOUS",
                null,
                null,
                null,
                null,
                NOW
        );
        service.attachFinding(
                tenantId,
                processing.getId(),
                record.getId(),
                ValidationStage.REFERENTIAL_INTEGRITY,
                ValidationSeverity.ERROR,
                "AMBIGUOUS_IDENTITY",
                "customer.customerId",
                "Customer identity requires review.",
                "IDENTITY_LINKAGE",
                NOW
        );

        service.quarantineRecord(
                tenantId,
                processing.getId(),
                record.getId(),
                NOW
        );

        StagedSourceRecord quarantined = recordRepository
                .findByIdAndTenantId(record.getId(), tenantId)
                .orElseThrow();
        assertEquals(StagedRecordState.QUARANTINED, quarantined.getState());
        assertEquals(StagedRecordValidationStatus.FAILED, quarantined.getValidationStatus());
        assertFalse(quarantined.isMaterializationEligible());
        assertEquals(
                DatasetProcessingStatus.QUARANTINED,
                datasetRepository.findById(processing.getId()).orElseThrow().getStatus()
        );
        assertEquals(1, service.findingsFor(tenantId, processing.getId()).size());
    }

    @Test
    void rejectsMissingSourceIdentityAndNonObjectPayload() {
        UUID tenantId = UUID.randomUUID();
        UUID datasetId = UUID.randomUUID();
        assertThrows(
                IllegalArgumentException.class,
                () -> StagedSourceRecord.stage(
                        UUID.randomUUID(),
                        tenantId,
                        datasetId,
                        StagedRecordType.VEHICLE,
                        " ",
                        null,
                        null,
                        null,
                        new ObjectMapper().createArrayNode(),
                        NOW
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> StagedSourceRecord.stage(
                        UUID.randomUUID(),
                        tenantId,
                        datasetId,
                        StagedRecordType.VEHICLE,
                        "VEHICLE-1",
                        null,
                        null,
                        null,
                        new ObjectMapper().createArrayNode(),
                        NOW
                )
        );
    }

    private static DatasetProcessing dataset(
            UUID tenantId,
            String datasetId,
            String datasetVersion
    ) {
        return DatasetProcessing.receive(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                datasetId,
                datasetVersion,
                "DMS-EXPORT",
                null,
                "source-1",
                NOW,
                null,
                null,
                "SHA-256",
                "checksum-" + datasetId,
                128L,
                UUID.randomUUID()
        );
    }

    private static StagedSourceRecord record(
            UUID tenantId,
            UUID datasetProcessingId,
            String sourceRecordId
    ) {
        return StagedSourceRecord.stage(
                UUID.randomUUID(),
                tenantId,
                datasetProcessingId,
                StagedRecordType.SERVICE_LINE,
                sourceRecordId,
                "JOB-1",
                "source-v1",
                "hash-" + sourceRecordId,
                new ObjectMapper().createObjectNode(),
                NOW
        );
    }
}