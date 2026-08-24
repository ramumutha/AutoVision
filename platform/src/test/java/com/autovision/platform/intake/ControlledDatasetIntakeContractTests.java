package com.autovision.platform.intake;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledDatasetIntakeContractTests {

    private static final UUID TENANT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID DEALER_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private final ControlledDatasetFileAdapter adapter = new ControlledDatasetFileAdapter(new ObjectMapper());
    private final DatasetEnvelopeValidator envelopeValidator = new DatasetEnvelopeValidator();
    private final StagedRecordValidator recordValidator = new StagedRecordValidator();

    @Test
    void readsFullPackageWithTypedRecordsAndChecksum() {
        ParsedControlledDataset parsed = adapter.read(resource("intake/g6-3-valid"));

        assertEquals("AV-R1-CDF-001", parsed.envelope().contractId());
        assertEquals("FULL", parsed.envelope().deliveryType());
        assertEquals(3, parsed.records().size());
        assertEquals(StagedRecordType.CUSTOMER, parsed.records().getFirst().recordType());
        assertEquals(64, parsed.contentChecksum().length());
        assertTrue(parsed.contentByteSize() > 0);
    }

    @Test
    void rejectsMissingPackageAndMalformedRecordFile() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> adapter.read(Path.of("missing-controlled-package")));

        Path packagePath = Files.createTempDirectory("g63-malformed-");
        Files.writeString(packagePath.resolve("manifest.json"), "{}");
        assertThrows(IllegalArgumentException.class, () -> adapter.read(packagePath));
    }

    @Test
    void validatesUnsupportedDeliveryAndContractVersions() {
        ControlledDatasetEnvelope envelope = envelope("DELTA", "9.9");

        List<IntakeFinding> findings = envelopeValidator.validate(envelope);

        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_UNSUPPORTED_DELIVERY_TYPE));
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_UNSUPPORTED_CONTRACT_VERSION));
    }

    @Test
    void validatesDuplicateReferencesContainmentDatesAndAmounts() {
        ParsedControlledDataset parsed = adapter.read(resource("intake/g6-3-valid"));
        ParsedSourceRecord duplicate = new ParsedSourceRecord(
                StagedRecordType.CUSTOMER, "customer-1", null, "2",
                new ObjectMapper().readTree("{\"tenantId\":\"30000000-0000-4000-8000-000000000001\",\"amount\":-1,\"serviceDate\":\"bad\"}")
        );
        List<IntakeFinding> findings = recordValidator.validate(parsed.envelope(), List.of(parsed.records().getFirst(), duplicate));

        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_DUPLICATE_SOURCE_RECORD));
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_TENANT_CONTAINMENT_MISMATCH));
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_INVALID_AMOUNT));
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_INVALID_DATE));
    }

    @Test
    void reportsMissingRequiredRelationship() {
        ParsedSourceRecord vehicle = new ParsedSourceRecord(
                StagedRecordType.VEHICLE, "vehicle-2", null, "1",
                new ObjectMapper().readTree("{\"tenantId\":\"10000000-0000-4000-8000-000000000001\",\"dealerId\":\"20000000-0000-4000-8000-000000000001\"}")
        );

        List<IntakeFinding> findings = recordValidator.validate(envelope("FULL", "1.0"), List.of(vehicle));

        assertFalse(findings.isEmpty());
        assertTrue(findings.stream().anyMatch(f -> f.code() == IntakeValidationCode.INTAKE_REQUIRED_REFERENCE_MISSING));
    }

    private Path resource(String name) {
        try {
            return Path.of(getClass().getClassLoader().getResource(name).toURI());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ControlledDatasetEnvelope envelope(String deliveryType, String contractVersion) {
        return new ControlledDatasetEnvelope(
                "AV-R1-CDF-001", contractVersion, "dataset-test", "1", TENANT_ID, DEALER_ID,
                null, "controlled-test-export", null, "2026-01", deliveryType,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"), OffsetDateTime.parse("2026-01-31T00:00:00Z"),
                "not-materialized", Map.of("CUSTOMER", "customers.json")
        );
    }
}
