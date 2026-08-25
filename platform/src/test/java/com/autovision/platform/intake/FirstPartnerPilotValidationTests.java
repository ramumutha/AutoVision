package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.data.DealerDataCapability;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityStatus;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureAvailability;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureProfile;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDisposition;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstPartnerPilotValidationTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-25T00:00:00Z");
    private final ControlledDatasetFileAdapter adapter = new ControlledDatasetFileAdapter(new ObjectMapper());
    private final StagedRecordValidator validator = new StagedRecordValidator();

    @Test
    void partnerLikeFullPackageQuarantinesOnlyUnsafeRecordAndMapsSafeLineage() {
        ParsedControlledDataset parsed = adapter.read(resource("intake/g6-6-first-partner"));

        List<IntakeFinding> findings = validator.validate(parsed.envelope(), parsed.records());

        assertEquals(1, findings.size());
        assertEquals(IntakeValidationCode.INTAKE_REFERENCE_NOT_FOUND, findings.getFirst().code());
        assertEquals("cost-invalid", findings.getFirst().sourceRecordId());

        DatasetProcessing processing = DatasetProcessing.receive(UUID.randomUUID(), parsed.envelope().tenantId(),
                parsed.envelope().dealerId(), parsed.envelope().locationId(), parsed.envelope().datasetId(),
                parsed.envelope().datasetVersion(), parsed.envelope().sourceSystem(), null,
                parsed.envelope().sourceSchemaVersion(), NOW, parsed.envelope().effectiveFrom(),
                parsed.envelope().effectiveTo(), "SHA-256", parsed.contentChecksum(), parsed.contentByteSize(),
                UUID.randomUUID());
        List<StagedSourceRecord> records = parsed.records().stream().map(source -> {
            StagedSourceRecord record = StagedSourceRecord.stage(UUID.randomUUID(), processing.getTenantId(),
                    processing.getId(), source.recordType(), source.sourceRecordId(), source.sourceParentId(),
                    source.sourceVersion(), null, source.payload(), NOW);
            if ("cost-invalid".equals(source.sourceRecordId())) {
                record.markValidationFailed();
                record.quarantine();
            } else {
                record.markValidationPassed();
            }
            return record;
        }).toList();

        List<CanonicalIntakeDetectionCandidate> candidates = new CanonicalIntakeMapper().map(processing, records);

        assertEquals(4, candidates.size());
        assertTrue(candidates.stream().noneMatch(candidate -> "cost-invalid".equals(candidate.sourceRecord().getSourceRecordId())));
        assertEquals(ServiceProfitDisposition.DECLINED, candidate(candidates, "job-declined").input().disposition());
        assertEquals(ServiceProfitDisposition.UNKNOWN, candidate(candidates, "job-missing-disposition").input().disposition());
        assertFalse(candidate(candidates, "job-declined").input().customerContactable());
        assertFalse(candidate(candidates, "job-declined").input().invoiceEvidence());
        assertTrue(candidate(candidates, "job-declined").input().evidence().stream()
            .anyMatch(evidence -> "job-declined".equals(evidence.sourceId())));
        assertTrue(candidate(candidates, "job-declined").input().evidence().stream()
            .anyMatch(evidence -> evidence.sourceType().name().equals("DISPOSITION")
                    && "disposition-declined".equals(evidence.sourceId())));
        assertTrue(candidate(candidates, "job-deferred").input().evidence().stream()
            .anyMatch(evidence -> evidence.sourceType().name().equals("DISPOSITION")
                && "disposition-deferred".equals(evidence.sourceId())
                && "order-deferred".equals(evidence.sourceParentId())));
        assertFalse(candidate(candidates, "job-missing-disposition").input().evidence().stream()
            .anyMatch(evidence -> evidence.sourceType().name().equals("DISPOSITION")));
    }

    @Test
    void partialInvoiceAndCostEvidenceDoesNotClaimCompleteAttribution() {
        var capabilities = new R1DealerDataCapabilityPolicy().evaluate(new DealerDataFeatureProfile(
                DealerDataFeatureAvailability.PARTIAL, DealerDataFeatureAvailability.PARTIAL,
                DealerDataFeatureAvailability.PARTIAL, DealerDataFeatureAvailability.PARTIAL,
                DealerDataFeatureAvailability.PARTIAL, DealerDataFeatureAvailability.PARTIAL,
                DealerDataFeatureAvailability.PARTIAL, DealerDataFeatureAvailability.PARTIAL));

        assertEquals(DealerDataCapabilityStatus.PARTIAL, capability(capabilities,
                DealerDataCapability.REVENUE_ATTRIBUTION));
        assertEquals(DealerDataCapabilityStatus.PARTIAL, capability(capabilities,
                DealerDataCapability.GROSS_PROFIT_ATTRIBUTION));
    }

            @Test
            void quarantinedDispositionCannotInfluenceOrBecomeEvidence() {
            DatasetProcessing processing = DatasetProcessing.receive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "dataset-quarantined-disposition", "1", "controlled-test", null,
                "2026-01", NOW, null, null, "SHA-256", "checksum", 1L,
                UUID.randomUUID());
            StagedSourceRecord order = record(processing, StagedRecordType.SERVICE_ORDER,
                "order-1", object("customerSourceRecordId", "customer-1", "vehicleSourceRecordId", "vehicle-1"));
            StagedSourceRecord vehicle = record(processing, StagedRecordType.VEHICLE,
                "vehicle-1", object("customerSourceRecordId", "customer-1"));
            StagedSourceRecord customer = record(processing, StagedRecordType.CUSTOMER,
                "customer-1", object());
            StagedSourceRecord job = record(processing, StagedRecordType.SERVICE_JOB,
                "job-1", object("serviceOrderSourceRecordId", "order-1"));
            StagedSourceRecord disposition = record(processing, StagedRecordType.DISPOSITION,
                "disposition-1", object("serviceOrderSourceRecordId", "order-1", "disposition", "DECLINED"));
            disposition.markValidationFailed();
            disposition.quarantine();

            var candidate = new CanonicalIntakeMapper().map(
                processing, List.of(order, vehicle, customer, job, disposition)).getFirst();

            assertEquals(ServiceProfitDisposition.UNKNOWN, candidate.input().disposition());
            assertFalse(candidate.input().evidence().stream()
                .anyMatch(evidence -> evidence.sourceType().name().equals("DISPOSITION")));
            }

    private Path resource(String name) {
        try {
            return Path.of(getClass().getClassLoader().getResource(name).toURI());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private CanonicalIntakeDetectionCandidate candidate(List<CanonicalIntakeDetectionCandidate> candidates, String sourceId) {
        return candidates.stream().filter(candidate -> sourceId.equals(candidate.input().sourceEntityId())).findFirst().orElseThrow();
    }

    private StagedSourceRecord record(DatasetProcessing processing, com.autovision.platform.intake.StagedRecordType type,
                                      String id, tools.jackson.databind.node.ObjectNode payload) {
        StagedSourceRecord record = StagedSourceRecord.stage(UUID.randomUUID(), processing.getTenantId(),
                processing.getId(), type, id, null, "1", null, payload, NOW);
        record.markValidationPassed();
        return record;
    }

    private tools.jackson.databind.node.ObjectNode object(String... values) {
        tools.jackson.databind.node.ObjectNode object = new ObjectMapper().createObjectNode();
        for (int index = 0; index < values.length; index += 2) {
            object.put(values[index], values[index + 1]);
        }
        return object;
    }

    private DealerDataCapabilityStatus capability(List<com.autovision.platform.serviceprofit.data.DealerDataCapabilityResult> capabilities,
                                                   DealerDataCapability capability) {
        return capabilities.stream().filter(result -> result.capability() == capability).findFirst().orElseThrow().status();
    }
}