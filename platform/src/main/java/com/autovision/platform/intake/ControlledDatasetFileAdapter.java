package com.autovision.platform.intake;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ControlledDatasetFileAdapter {

    private static final String MANIFEST = "manifest.json";
    private final ObjectMapper objectMapper;

    public ControlledDatasetFileAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedControlledDataset read(Path packagePath) {
        if (packagePath == null || !Files.isDirectory(packagePath)) {
            throw new IllegalArgumentException("Controlled dataset package is required");
        }
        Path manifestPath = packagePath.resolve(MANIFEST);
        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Controlled dataset manifest is required");
        }
        try {
            JsonNode manifest = objectMapper.readTree(Files.readAllBytes(manifestPath));
            ControlledDatasetEnvelope envelope = envelope(manifest);
            List<ParsedSourceRecord> records = new ArrayList<>();
            long contentByteSize = 0;
            java.security.MessageDigest checksum = checksum();
            byte[] manifestBytes = Files.readAllBytes(manifestPath);
            checksum.update(manifestBytes);
            contentByteSize += manifestBytes.length;
                for (Map.Entry<String, String> entry : envelope.recordFiles().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                StagedRecordType type = parseType(entry.getKey());
                Path recordFile = packagePath.resolve(entry.getValue()).normalize();
                if (!recordFile.startsWith(packagePath.normalize()) || !Files.isRegularFile(recordFile)) {
                    throw new IllegalArgumentException("Controlled dataset record file is invalid");
                }
                byte[] recordFileBytes = Files.readAllBytes(recordFile);
                checksum.update(recordFileBytes);
                contentByteSize += recordFileBytes.length;
                JsonNode collection = objectMapper.readTree(recordFileBytes);
                if (collection == null || !collection.isArray()) {
                    throw new IllegalArgumentException("Controlled dataset record file is invalid");
                }
                for (JsonNode payload : collection) {
                    if (payload == null || !payload.isObject()) {
                        throw new IllegalArgumentException("Controlled dataset record file is invalid");
                    }
                    records.add(new ParsedSourceRecord(
                            type,
                            optionalText(payload, "sourceRecordId"),
                            optionalText(payload, "sourceParentId"),
                            optionalText(payload, "sourceVersion"),
                            payload
                    ));
                }
            }
            return new ParsedControlledDataset(
                    envelope,
                    List.copyOf(records),
                    HexFormat.of().formatHex(checksum.digest()),
                    contentByteSize,
                    packagePath
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read controlled dataset package", exception);
        }
    }

    private ControlledDatasetEnvelope envelope(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Controlled dataset envelope is invalid");
        }
        return new ControlledDatasetEnvelope(
                text(node, "contractId"), text(node, "contractVersion"),
                text(node, "datasetId"), text(node, "datasetVersion"),
                uuid(node, "tenantId"), uuid(node, "dealerId"),
                optionalUuid(node, "locationId"), text(node, "sourceSystem"),
                optionalText(node, "sourceProvider"), text(node, "sourceSchemaVersion"),
                text(node, "deliveryType"), optionalTime(node, "effectiveFrom"),
                optionalTime(node, "effectiveTo"), text(node, "mappingVersion"),
                files(node.get("recordFiles"))
        );
    }

    private Map<String, String> files(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Controlled dataset record files are required");
        }
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        for (String fieldName : node.propertyNames()) {
            JsonNode field = node.get(fieldName);
            result.put(fieldName, field.asText());
        }
        return Map.copyOf(result);
    }

    private StagedRecordType parseType(String value) {
        try {
            return StagedRecordType.valueOf(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Controlled dataset record type is invalid");
        }
    }

    private String text(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Controlled dataset envelope is missing a required field");
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Controlled dataset envelope contains an invalid identity");
        }
    }

    private UUID optionalUuid(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Controlled dataset envelope contains an invalid identity");
        }
    }

    private java.time.OffsetDateTime optionalTime(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) return null;
        try { return java.time.OffsetDateTime.parse(value); }
        catch (Exception exception) { throw new IllegalArgumentException("Controlled dataset envelope contains an invalid date"); }
    }

    private MessageDigest checksum() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required checksum algorithm is unavailable", exception);
        }
    }
}