package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionInput;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDisposition;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceRef;
import com.autovision.platform.serviceprofit.detection.ServiceProfitEvidenceSourceType;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ServiceProfitDemoDetectionInputAdapter {

    public List<ServiceProfitDemoDetectionScenario> load(
            Path datasetRoot,
            UUID tenantId
    ) {
        requirePath(datasetRoot);

        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "Demo detection tenant ID is required"
            );
        }

        try {
            Map<String, RepairOrderRow> repairOrders =
                    readRepairOrders(datasetRoot);

            Map<String, List<ServiceJobRow>> jobs =
                    readServiceJobs(datasetRoot);

            Map<String, RecommendationRow> recommendations =
                    readRecommendations(datasetRoot);

            Map<String, AdvisorNoteRow> notes =
                    readAdvisorNotes(datasetRoot);

            Map<String, ServiceHistoryRow> histories =
                    readServiceHistory(datasetRoot);

            Map<String, InvoiceRow> invoices =
                    readInvoices(datasetRoot);

            List<ServiceProfitDemoDetectionScenario> scenarios =
                    new ArrayList<>();

            /*
             * Transaction-backed scenarios are driven by service jobs.
             * For scenarios with multiple jobs, the earliest unresolved
             * recommendation is the detection source.
             */
            for (Map.Entry<String, List<ServiceJobRow>> entry
                    : jobs.entrySet()) {

                String scenarioId = entry.getKey();

                ServiceJobRow sourceJob =
                        selectSourceJob(entry.getValue());

                if (sourceJob == null) {
                    continue;
                }

                RepairOrderRow repairOrder =
                        repairOrders.get(
                                sourceJob.repairOrderId()
                        );

                if (repairOrder == null) {
                    throw new IllegalStateException(
                            "Repair order not found for service job "
                                    + sourceJob.serviceJobId()
                    );
                }

                RecommendationRow recommendation =
                        recommendations.get(
                                sourceJob.serviceJobId()
                        );

                AdvisorNoteRow note =
                        notes.get(
                                sourceJob.serviceJobId()
                        );

                boolean completedWorkEvidence =
                        hasCompletedWorkEvidence(
                                scenarioId,
                                entry.getValue(),
                                invoices
                        );

                ServiceProfitDetectionInput input =
                        new ServiceProfitDetectionInput(
                                tenantId,
                                repairOrder.dealerId(),
                                repairOrder.branchId(),
                                repairOrder.locationId(),

                                repairOrder.customerId(),
                                repairOrder.vehicleId(),

                                "AUTOVISION_SERVICE_PROFIT_DEMO",
                                "SERVICE_JOB",
                                sourceJob.serviceJobId(),

                                uuid(repairOrder.repairOrderId()),
                                uuid(sourceJob.serviceJobId()),
                                null,
                                null,

                                disposition(
                                        sourceJob.disposition()
                                ),
                                null,

                                recommendation == null
                                        ? null
                                        : recommendation.recommendationText(),

                                note == null
                                        ? null
                                        : note.noteText(),

                                recommendation == null
                                        ? null
                                        : date(
                                                recommendation.recommendedAt()
                                        ),

                                recommendation == null
                                        ? null
                                        : localDate(
                                                recommendation.deferUntilDate()
                                        ),

                                null,
                                null,

                                decimal(
                                        repairOrder.odometerKm()
                                ),

                                recommendation == null
                                        ? null
                                        : decimal(
                                                recommendation.deferUntilOdometerKm()
                                        ),

                                null,

                                decimal(
                                        sourceJob.estimatedAmount()
                                ),
                                null,
                                null,
                                sourceJob.currencyCode(),

                                repairOrder.customerId() != null,
                                completedWorkEvidence,
                                invoices.containsKey(scenarioId),

                                evidenceFor(
                                        sourceJob,
                                        recommendation,
                                        note
                                )
                        );

                scenarios.add(
                        new ServiceProfitDemoDetectionScenario(
                                scenarioId,
                                input
                        )
                );
            }

            /*
             * Lifecycle scenarios have no repair-order/service-job
             * transaction and are constructed from service history.
             */
            for (ServiceHistoryRow history
                    : histories.values()) {

                ServiceProfitDetectionInput input =
                        new ServiceProfitDetectionInput(
                                tenantId,
                                history.dealerId(),
                                history.branchId(),
                                history.locationId(),

                                history.customerId(),
                                history.vehicleId(),

                                "AUTOVISION_SERVICE_PROFIT_DEMO",
                                "SERVICE_HISTORY",
                                history.serviceHistoryId(),

                                null,
                                null,
                                null,
                                null,

                                null,
                                null,

                                null,
                                null,

                                null,
                                null,

                                localDate(
                                        history.nextServiceDueDate()
                                ),

                                localDate(
                                        history.lastServiceDate()
                                ),

                                null,

                                decimal(
                                        history.nextServiceDueOdometerKm()
                                ),

                                dateTime(
                                        history.lastCustomerActivityDate()
                                ),

                                null,
                                null,
                                null,
                                null,

                                true,
                                false,
                                false,

                                List.of(
                                        new ServiceProfitEvidenceRef(
                                                ServiceProfitEvidenceSourceType
                                                        .SERVICE_HISTORY,
                                                history.serviceHistoryId(),
                                                dateTime(
                                                        history.lastCustomerActivityDate()
                                                )
                                        )
                                )
                        );

                scenarios.add(
                        new ServiceProfitDemoDetectionScenario(
                                history.scenarioId(),
                                input
                        )
                );
            }

            return List.copyOf(scenarios);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read Service Profit demo detection dataset",
                    exception
            );
        }
    }

    private Map<String, RepairOrderRow> readRepairOrders(
            Path root
    ) throws IOException {
        Map<String, RepairOrderRow> result =
                new HashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("repair_orders.csv")
                )) {

            RepairOrderRow value =
                    new RepairOrderRow(
                            row.get("repair_order_id"),
                            row.get("scenario_id"),
                            uuid(row.get("dealer_id")),
                            uuid(row.get("branch_id")),
                            uuid(row.get("location_id")),
                            uuid(row.get("customer_id")),
                            uuid(row.get("vehicle_id")),
                            row.get("odometer_km")
                    );

            result.put(
                    value.repairOrderId(),
                    value
            );
        }

        return result;
    }

    private Map<String, List<ServiceJobRow>> readServiceJobs(
            Path root
    ) throws IOException {
        Map<String, List<ServiceJobRow>> result =
                new LinkedHashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("service_jobs.csv")
                )) {

            ServiceJobRow value =
                    new ServiceJobRow(
                            row.get("service_job_id"),
                            row.get("repair_order_id"),
                            row.get("scenario_id"),
                            row.get("disposition"),
                            row.get("estimated_amount"),
                            row.get("currency_code"),
                            Boolean.parseBoolean(
                                    row.get("completed")
                            )
                    );

            result.computeIfAbsent(
                    value.scenarioId(),
                    ignored -> new ArrayList<>()
            ).add(value);
        }

        return result;
    }

    private Map<String, RecommendationRow> readRecommendations(
            Path root
    ) throws IOException {
        Map<String, RecommendationRow> result =
                new HashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("recommendations.csv")
                )) {

            RecommendationRow value =
                    new RecommendationRow(
                            row.get("service_job_id"),
                            row.get("recommendation_text"),
                            row.get("recommended_at"),
                            row.get("defer_until_date"),
                            row.get("defer_until_odometer_km")
                    );

            result.put(
                    value.serviceJobId(),
                    value
            );
        }

        return result;
    }

    private Map<String, AdvisorNoteRow> readAdvisorNotes(
            Path root
    ) throws IOException {
        Map<String, AdvisorNoteRow> result =
                new HashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("advisor_notes.csv")
                )) {

            AdvisorNoteRow value =
                    new AdvisorNoteRow(
                            row.get("service_job_id"),
                            row.get("note_text"),
                            row.get("created_at")
                    );

            result.put(
                    value.serviceJobId(),
                    value
            );
        }

        return result;
    }

    private Map<String, ServiceHistoryRow> readServiceHistory(
            Path root
    ) throws IOException {
        Map<String, ServiceHistoryRow> result =
                new LinkedHashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("service_history.csv")
                )) {

            ServiceHistoryRow value =
                    new ServiceHistoryRow(
                            row.get("service_history_id"),
                            row.get("scenario_id"),
                            uuid(row.get("customer_id")),
                            uuid(row.get("vehicle_id")),
                            uuid(row.get("dealer_id")),
                            uuid(row.get("branch_id")),
                            uuid(row.get("location_id")),
                            row.get("last_service_date"),
                            row.get("next_service_due_date"),
                            row.get("next_service_due_odometer_km"),
                            row.get("last_customer_activity_date")
                    );

            result.put(
                    value.scenarioId(),
                    value
            );
        }

        return result;
    }

    private Map<String, InvoiceRow> readInvoices(
            Path root
    ) throws IOException {
        Map<String, InvoiceRow> result =
                new HashMap<>();

        for (Map<String, String> row
                : readCsv(
                        root.resolve("source")
                                .resolve("invoices.csv")
                )) {

            InvoiceRow value =
                    new InvoiceRow(
                            row.get("scenario_id"),
                            row.get("invoice_id")
                    );

            result.put(
                    value.scenarioId(),
                    value
            );
        }

        return result;
    }

    private ServiceJobRow selectSourceJob(
            List<ServiceJobRow> jobs
    ) {
        for (ServiceJobRow job : jobs) {
            if (!job.completed()) {
                return job;
            }
        }

        return jobs.isEmpty()
                ? null
                : jobs.getFirst();
    }

    private boolean hasCompletedWorkEvidence(
            String scenarioId,
            List<ServiceJobRow> jobs,
            Map<String, InvoiceRow> invoices
    ) {
        for (ServiceJobRow job : jobs) {
            if (job.completed()) {
                return true;
            }
        }

        /*
         * An invoice alone is not treated as completed-work evidence
         * here because SP-DEMO-008 and SP-DEMO-009 deliberately test
         * later conversion/revenue attribution rather than suppression.
         */
        return false;
    }

    private List<ServiceProfitEvidenceRef> evidenceFor(
            ServiceJobRow job,
            RecommendationRow recommendation,
            AdvisorNoteRow note
    ) {
        List<ServiceProfitEvidenceRef> evidence =
                new ArrayList<>();

        evidence.add(
                new ServiceProfitEvidenceRef(
                        ServiceProfitEvidenceSourceType.SERVICE_JOB,
                        job.serviceJobId(),
                        null
                )
        );

        if (recommendation != null) {
            evidence.add(
                    new ServiceProfitEvidenceRef(
                            ServiceProfitEvidenceSourceType.RECOMMENDATION,
                            recommendation.serviceJobId(),
                            offsetDateTime(
                                    recommendation.recommendedAt()
                            )
                    )
            );
        }

        if (note != null) {
            evidence.add(
                    new ServiceProfitEvidenceRef(
                            ServiceProfitEvidenceSourceType.ADVISOR_NOTE,
                            note.serviceJobId(),
                            offsetDateTime(
                                    note.createdAt()
                            )
                    )
            );
        }

        return List.copyOf(evidence);
    }

    private List<Map<String, String>> readCsv(
            Path path
    ) throws IOException {
        List<String> lines =
                Files.readAllLines(path);

        if (lines.isEmpty()) {
            return List.of();
        }

        String[] headers =
                lines.getFirst().split(",", -1);

        List<Map<String, String>> rows =
                new ArrayList<>();

        for (int index = 1;
             index < lines.size();
             index++) {

            String line = lines.get(index);

            if (line.isBlank()) {
                continue;
            }

            String[] values =
                    line.split(",", -1);

            if (values.length != headers.length) {
                throw new IllegalStateException(
                        "Invalid demo CSV column count in "
                                + path
                                + " at line "
                                + (index + 1)
                );
            }

            Map<String, String> row =
                    new HashMap<>();

            for (int column = 0;
                 column < headers.length;
                 column++) {

                row.put(
                        headers[column].trim(),
                        normalize(values[column])
                );
            }

            rows.add(row);
        }

        return rows;
    }

    private ServiceProfitDisposition disposition(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case "DECLINED" ->
                    ServiceProfitDisposition.DECLINED;

            case "DEFERRED" ->
                    ServiceProfitDisposition.DEFERRED;

            default ->
                    null;
        };
    }

    private static UUID uuid(
            String value
    ) {
        return value == null
                ? null
                : UUID.fromString(value);
    }

    private static BigDecimal decimal(
            String value
    ) {
        return value == null
                ? null
                : new BigDecimal(value);
    }

    private static LocalDate localDate(
            String value
    ) {
        return value == null
                ? null
                : LocalDate.parse(value);
    }

    private static LocalDate date(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return OffsetDateTime.parse(value)
                .toLocalDate();
    }

    private static OffsetDateTime dateTime(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return LocalDate.parse(value)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime offsetDateTime(
            String value
    ) {
        return value == null
                ? null
                : OffsetDateTime.parse(value);
    }

    private static String normalize(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private static void requirePath(
            Path datasetRoot
    ) {
        if (datasetRoot == null) {
            throw new IllegalArgumentException(
                    "Demo dataset root is required"
            );
        }
    }

    private record RepairOrderRow(
            String repairOrderId,
            String scenarioId,
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            UUID customerId,
            UUID vehicleId,
            String odometerKm
    ) {
    }

    private record ServiceJobRow(
            String serviceJobId,
            String repairOrderId,
            String scenarioId,
            String disposition,
            String estimatedAmount,
            String currencyCode,
            boolean completed
    ) {
    }

    private record RecommendationRow(
            String serviceJobId,
            String recommendationText,
            String recommendedAt,
            String deferUntilDate,
            String deferUntilOdometerKm
    ) {
    }

    private record AdvisorNoteRow(
            String serviceJobId,
            String noteText,
            String createdAt
    ) {
    }

    private record ServiceHistoryRow(
            String serviceHistoryId,
            String scenarioId,
            UUID customerId,
            UUID vehicleId,
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            String lastServiceDate,
            String nextServiceDueDate,
            String nextServiceDueOdometerKm,
            String lastCustomerActivityDate
    ) {
    }

    private record InvoiceRow(
            String scenarioId,
            String invoiceId
    ) {
    }
}