package com.autovision.platform.intake;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class StagedRecordValidator {

    public List<IntakeFinding> validate(
            ControlledDatasetEnvelope envelope,
            List<ParsedSourceRecord> records
    ) {
        Set<String> identities = new HashSet<>();
        java.util.ArrayList<IntakeFinding> findings = new java.util.ArrayList<>();
        for (ParsedSourceRecord record : records) {
            String identity = record.recordType() + ":" + record.sourceRecordId();
            if (record.sourceRecordId() == null || record.sourceRecordId().isBlank()) {
                findings.add(error(record, IntakeValidationCode.INTAKE_REQUIRED_REFERENCE_MISSING,
                        "sourceRecordId", "A source record identity is required"));
                continue;
            }
            if (!identities.add(identity)) {
                findings.add(error(record, IntakeValidationCode.INTAKE_DUPLICATE_SOURCE_RECORD,
                        "sourceRecordId", "The source record identity is duplicated"));
            }
            checkContainment(envelope, record, findings);
            checkRelationships(record, records, findings);
            checkBusinessFields(record, findings);
        }
        return List.copyOf(findings);
    }

    private void checkContainment(ControlledDatasetEnvelope envelope, ParsedSourceRecord record,
                                  List<IntakeFinding> findings) {
        checkEquals(envelope.tenantId().toString(), record, "tenantId",
                IntakeValidationCode.INTAKE_TENANT_CONTAINMENT_MISMATCH, findings);
        checkEquals(envelope.dealerId().toString(), record, "dealerId",
                IntakeValidationCode.INTAKE_DEALER_CONTAINMENT_MISMATCH, findings);
        JsonNode location = record.payload().get("locationId");
        if (location != null && !location.isNull() && envelope.locationId() != null) {
            checkEquals(envelope.locationId().toString(), record, "locationId",
                    IntakeValidationCode.INTAKE_LOCATION_CONTAINMENT_MISMATCH, findings);
        }
    }

    private void checkEquals(String expected, ParsedSourceRecord record, String field,
                             IntakeValidationCode code, List<IntakeFinding> findings) {
        JsonNode value = record.payload().get(field);
        if (value != null && !value.isNull() && !expected.equals(value.asText())) {
            findings.add(error(record, code, field, "The record is outside the dataset containment boundary"));
        }
    }

    private void checkRelationships(ParsedSourceRecord record, List<ParsedSourceRecord> records,
                                    List<IntakeFinding> findings) {
        switch (record.recordType()) {
            case VEHICLE -> required(record, "customerSourceRecordId", StagedRecordType.CUSTOMER, records, findings);
            case SERVICE_ORDER -> {
                required(record, "customerSourceRecordId", StagedRecordType.CUSTOMER, records, findings);
                required(record, "vehicleSourceRecordId", StagedRecordType.VEHICLE, records, findings);
            }
            case SERVICE_JOB -> required(record, "serviceOrderSourceRecordId", StagedRecordType.SERVICE_ORDER, records, findings);
            case SERVICE_LINE -> required(record, "serviceJobSourceRecordId", StagedRecordType.SERVICE_JOB, records, findings);
            case RECOMMENDATION -> required(record, "serviceOrderSourceRecordId", StagedRecordType.SERVICE_ORDER, records, findings);
            case SERVICE_HISTORY -> required(record, "vehicleSourceRecordId", StagedRecordType.VEHICLE, records, findings);
            case INVOICE -> required(record, "serviceOrderSourceRecordId", StagedRecordType.SERVICE_ORDER, records, findings);
            case INVOICE_LINE -> required(record, "invoiceSourceRecordId", StagedRecordType.INVOICE, records, findings);
            case COST -> optionalReference(record, "serviceLineSourceRecordId", StagedRecordType.SERVICE_LINE, records, findings);
            default -> { }
        }
    }

    private void required(ParsedSourceRecord record, String field, StagedRecordType type,
                          List<ParsedSourceRecord> records, List<IntakeFinding> findings) {
        String value = text(record.payload(), field);
        if (value == null) {
            findings.add(error(record, IntakeValidationCode.INTAKE_REQUIRED_REFERENCE_MISSING,
                    field, "A required source reference is missing"));
        } else if (!exists(type, value, records)) {
            findings.add(error(record, IntakeValidationCode.INTAKE_REFERENCE_NOT_FOUND,
                    field, "A required source reference was not found"));
        }
    }

    private void optionalReference(ParsedSourceRecord record, String field, StagedRecordType type,
                                   List<ParsedSourceRecord> records, List<IntakeFinding> findings) {
        String value = text(record.payload(), field);
        if (value != null && !exists(type, value, records)) {
            findings.add(error(record, IntakeValidationCode.INTAKE_REFERENCE_NOT_FOUND,
                    field, "The supplied source reference was not found"));
        }
    }

    private boolean exists(StagedRecordType type, String sourceRecordId, List<ParsedSourceRecord> records) {
        return records.stream().anyMatch(candidate -> candidate.recordType() == type
                && sourceRecordId.equals(candidate.sourceRecordId()));
    }

    private void checkBusinessFields(ParsedSourceRecord record, List<IntakeFinding> findings) {
        if (record.recordType() == StagedRecordType.SERVICE_LINE
                || record.recordType() == StagedRecordType.RECOMMENDATION) {
            String field = record.recordType() == StagedRecordType.SERVICE_LINE ? "description" : "description";
            if (text(record.payload(), field) == null) {
                findings.add(error(record, IntakeValidationCode.INTAKE_REQUIRED_REFERENCE_MISSING,
                        field, "A required description is missing"));
            }
        }
        for (String field : List.of("serviceDate", "recommendedAt", "invoiceDate")) {
            JsonNode value = record.payload().get(field);
            if (value != null && !value.isNull()) {
                try { java.time.OffsetDateTime.parse(value.asText()); }
                catch (Exception ignored) {
                    try { java.time.LocalDate.parse(value.asText()); }
                    catch (Exception ignoredAgain) {
                        findings.add(error(record, IntakeValidationCode.INTAKE_INVALID_DATE,
                                field, "The supplied date is invalid"));
                    }
                }
            }
        }
        for (String field : List.of("amount", "totalAmount", "quantity")) {
            JsonNode value = record.payload().get(field);
            if (value != null && !value.isNull()) {
                try { if (value.asDouble() < 0) throw new NumberFormatException(); }
                catch (Exception exception) {
                    findings.add(error(record, IntakeValidationCode.INTAKE_INVALID_AMOUNT,
                            field, "The supplied amount must be non-negative"));
                }
            }
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private IntakeFinding error(ParsedSourceRecord record, IntakeValidationCode code,
                                String path, String message) {
        return new IntakeFinding(ValidationStage.REFERENTIAL_INTEGRITY,
                ValidationSeverity.ERROR, code, path, message, record.sourceRecordId());
    }
}