package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextSnapshot;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionInput;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDisposition;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceRef;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceSourceType;
import com.autovision.platform.serviceprofit.detection.ServiceProfitRecommendationStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CanonicalIntakeMapper {

    public List<CanonicalIntakeDetectionCandidate> map(
            DatasetProcessing processing,
            List<StagedSourceRecord> records
    ) {
        if (processing == null || records == null) {
            throw new IllegalArgumentException("Dataset and staged records are required");
        }
        Map<StagedRecordType, Map<String, StagedSourceRecord>> byType = index(records);
        List<CanonicalIntakeDetectionCandidate> candidates = new ArrayList<>();
        for (StagedSourceRecord record : records) {
            if (!eligible(record)) continue;
            if (record.getRecordType() == StagedRecordType.SERVICE_JOB) {
                candidates.add(mapJob(processing, record, byType));
            } else if (record.getRecordType() == StagedRecordType.SERVICE_HISTORY) {
                candidates.add(mapHistory(processing, record, byType));
            }
        }
        return List.copyOf(candidates);
    }

    private CanonicalIntakeDetectionCandidate mapJob(
            DatasetProcessing processing,
            StagedSourceRecord job,
            Map<StagedRecordType, Map<String, StagedSourceRecord>> byType
    ) {
        JsonNode jobPayload = payload(job);
        String orderId = text(jobPayload, "serviceOrderSourceRecordId");
        if (orderId == null) orderId = job.getSourceParentId();
        StagedSourceRecord order = find(byType, StagedRecordType.SERVICE_ORDER, orderId);
        JsonNode orderPayload = payload(order);
        String vehicleId = text(orderPayload, "vehicleSourceRecordId");
        StagedSourceRecord vehicle = find(byType, StagedRecordType.VEHICLE, vehicleId);
        JsonNode vehiclePayload = payload(vehicle);
        String customerId = text(vehiclePayload, "customerSourceRecordId");
        StagedSourceRecord customer = find(byType, StagedRecordType.CUSTOMER, customerId);
        JsonNode customerPayload = payload(customer);
        StagedSourceRecord recommendation = findByReference(
                byType.get(StagedRecordType.RECOMMENDATION), "serviceOrderSourceRecordId", orderId);
        StagedSourceRecord disposition = findByReference(
                byType.get(StagedRecordType.DISPOSITION), "serviceOrderSourceRecordId", orderId);
        StagedSourceRecord note = findByReference(
                byType.get(StagedRecordType.NOTE), "serviceJobSourceRecordId", job.getSourceRecordId());
        StagedSourceRecord invoice = findByReference(
                byType.get(StagedRecordType.INVOICE), "serviceOrderSourceRecordId", orderId);
        JsonNode recommendationPayload = payload(recommendation);
        JsonNode dispositionPayload = payload(disposition);
        JsonNode notePayload = payload(note);
        JsonNode invoicePayload = payload(invoice);
        LocalDate serviceDate = date(orderPayload, "serviceDate");

        ServiceProfitDetectionInput input = new ServiceProfitDetectionInput(
                processing.getTenantId(), processing.getDealerId(), uuid(orderPayload, "branchId"),
                processing.getLocationId(), uuid(customerPayload, "customerId"), uuid(vehiclePayload, "vehicleId"),
                processing.getSourceSystem(), "SERVICE_JOB", job.getSourceRecordId(),
                uuid(order, "sourceRecordId"), uuid(job, "sourceRecordId"), null, null,
                disposition(dispositionPayload, "disposition"), recommendationStatus(recommendationPayload),
                text(recommendationPayload, "description"), text(notePayload, "note"),
                date(recommendationPayload, "recommendedAt"), date(recommendationPayload, "deferredUntilDate"),
                null, null, decimal(orderPayload, "mileage"), decimal(recommendationPayload, "deferredUntilMileage"),
                null, decimal(recommendationPayload, "amount"), decimal(invoicePayload, "totalAmount"), null,
                firstText(recommendationPayload, "currency", invoicePayload, "currency"),
                hasContact(customerPayload), invoice != null, invoice != null, evidence(processing, job, order,
                        recommendation, disposition, note, invoice)
        );
        return new CanonicalIntakeDetectionCandidate(job, input, context(customerPayload, vehiclePayload, orderPayload,
                jobPayload, recommendationPayload, notePayload, serviceDate, customerId, vehicleId));
    }

    private CanonicalIntakeDetectionCandidate mapHistory(
            DatasetProcessing processing,
            StagedSourceRecord history,
            Map<StagedRecordType, Map<String, StagedSourceRecord>> byType
    ) {
        JsonNode historyPayload = payload(history);
        String vehicleId = text(historyPayload, "vehicleSourceRecordId");
        StagedSourceRecord vehicle = find(byType, StagedRecordType.VEHICLE, vehicleId);
        JsonNode vehiclePayload = payload(vehicle);
        String customerId = text(vehiclePayload, "customerSourceRecordId");
        StagedSourceRecord customer = find(byType, StagedRecordType.CUSTOMER, customerId);
        JsonNode customerPayload = payload(customer);
        LocalDate lastServiceDate = date(historyPayload, "lastServiceDate");
        ServiceProfitDetectionInput input = new ServiceProfitDetectionInput(
                processing.getTenantId(), processing.getDealerId(), uuid(historyPayload, "branchId"),
                processing.getLocationId(), uuid(customerPayload, "customerId"), uuid(vehiclePayload, "vehicleId"),
                processing.getSourceSystem(), "SERVICE_HISTORY", history.getSourceRecordId(),
                null, null, null, null, null, null, null, text(historyPayload, "summary"),
                null, date(historyPayload, "nextServiceDueDate"), date(historyPayload, "nextServiceDueDate"),
                lastServiceDate, decimal(historyPayload, "currentMileage"),
                decimal(historyPayload, "nextServiceDueMileage"), time(historyPayload, "lastCustomerActivityAt"),
                null, null, null, null, hasContact(customerPayload), false, false,
                evidence(processing, history, vehicle, customer)
        );
        return new CanonicalIntakeDetectionCandidate(history, input, context(customerPayload, vehiclePayload,
                null, historyPayload, null, null, lastServiceDate, customerId, vehicleId));
    }

    private Map<StagedRecordType, Map<String, StagedSourceRecord>> index(List<StagedSourceRecord> records) {
        Map<StagedRecordType, Map<String, StagedSourceRecord>> result = new EnumMap<>(StagedRecordType.class);
        for (StagedSourceRecord record : records) {
            result.computeIfAbsent(record.getRecordType(), ignored -> new java.util.HashMap<>())
                    .put(record.getSourceRecordId(), record);
        }
        return result;
    }

    private boolean eligible(StagedSourceRecord record) {
        return record.getState() == StagedRecordState.STAGED
                && record.getValidationStatus() == StagedRecordValidationStatus.PASSED
                && record.isMaterializationEligible();
    }

    private StagedSourceRecord find(Map<StagedRecordType, Map<String, StagedSourceRecord>> byType,
                                    StagedRecordType type, String id) {
        return id == null ? null : byType.getOrDefault(type, Map.of()).get(id);
    }

    private StagedSourceRecord findByReference(Map<String, StagedSourceRecord> records, String field, String id) {
        if (records == null || id == null) return null;
        return records.values().stream()
            .filter(this::eligible)
            .filter(record -> id.equals(text(payload(record), field)))
            .findFirst()
            .orElse(null);
    }

    private JsonNode payload(StagedSourceRecord record) {
        return record == null || record.getRawPayload() == null ? null : record.getRawPayload();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }

    private String firstText(JsonNode first, String firstField, JsonNode second, String secondField) {
        String value = text(first, firstField);
        return value == null ? text(second, secondField) : value;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return new BigDecimal(value); } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Canonical numeric field is invalid");
        }
    }

    private LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return LocalDate.parse(value.substring(0, 10)); } catch (Exception exception) {
            throw new IllegalArgumentException("Canonical date field is invalid");
        }
    }

    private OffsetDateTime time(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return OffsetDateTime.parse(value); } catch (Exception exception) {
            throw new IllegalArgumentException("Canonical timestamp field is invalid");
        }
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException exception) { return null; }
    }

    private UUID uuid(StagedSourceRecord record, String field) {
        String value = record == null ? null : record.getSourceRecordId();
        return uuid(value);
    }

    private UUID uuid(String value) {
        if (value == null) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException exception) { return null; }
    }

    private ServiceProfitDisposition disposition(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return ServiceProfitDisposition.UNKNOWN;
        try { return ServiceProfitDisposition.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return ServiceProfitDisposition.UNKNOWN; }
    }

    private ServiceProfitRecommendationStatus recommendationStatus(JsonNode node) {
        String value = text(node, "status");
        if (value == null) return null;
        try { return ServiceProfitRecommendationStatus.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException exception) { return ServiceProfitRecommendationStatus.UNKNOWN; }
    }

    private boolean hasContact(JsonNode customer) {
        return text(customer, "phone") != null || text(customer, "email") != null;
    }

    private List<ServiceProfitEvidenceRef> evidence(DatasetProcessing processing, StagedSourceRecord... records) {
        List<ServiceProfitEvidenceRef> result = new ArrayList<>();
        for (StagedSourceRecord record : records) {
            if (record == null) continue;
            ServiceProfitEvidenceSourceType sourceType;
            try { sourceType = ServiceProfitEvidenceSourceType.valueOf(record.getRecordType().name()); }
            catch (IllegalArgumentException exception) { continue; }
                result.add(new ServiceProfitEvidenceRef(
                    sourceType,
                    record.getSourceRecordId(),
                    record.getSourceParentId(),
                    record.getCreatedAt()
                ));
        }
        return List.copyOf(result);
    }

    private ServiceProfitOpportunityContextSnapshot context(JsonNode customer, JsonNode vehicle, JsonNode order,
            JsonNode source, JsonNode recommendation, JsonNode note, LocalDate serviceDate,
            String customerId, String vehicleId) {
        return new ServiceProfitOpportunityContextSnapshot(
            firstText(customer, "displayName", customer, "name"), customerId, text(customer, "phone"), text(customer, "email"),
                hasContact(customer), text(vehicle, "registration"), text(vehicle, "vin"), text(vehicle, "make"),
                text(vehicle, "model"), integer(vehicle, "modelYear"), text(vehicle, "powertrain"),
                text(order, "sourceRecordId"), serviceDate, firstText(recommendation, "description", source, "description"),
                text(note, "note"));
    }

    private Integer integer(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException exception) { return null; }
    }
}
