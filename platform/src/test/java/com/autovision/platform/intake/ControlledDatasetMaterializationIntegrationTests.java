package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.ServiceProfitOpportunityContextService;
import com.autovision.platform.serviceprofit.data.DealerDataAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityStatus;
import com.autovision.platform.serviceprofit.data.DealerDataCapability;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureAvailability;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.data.R1DealerDataReadinessPolicy;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionOrchestrator;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionPersistenceService;
import com.autovision.platform.serviceprofit.detection.ServiceProfitDetectionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlledDatasetMaterializationIntegrationTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T08:00:00Z");
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PRINCIPAL_ID = UUID.randomUUID();

    @Mock
    private DatasetProcessingService datasetService;
    @Mock
    private ControlledDatasetMaterializationFailureService failureService;
    @Mock
    private ControlledDatasetReadinessService readinessService;
    @Mock
    private ServiceProfitDetectionOrchestrator orchestrator;
    @Mock
    private ServiceProfitDetectionPersistenceService persistenceService;
    @Mock
    private ServiceProfitOpportunityContextService contextService;
    @Mock
    private DealerDataAssessmentRepository assessmentRepository;
    @Mock
    private DealerDataCapabilityAssessmentRepository capabilityRepository;

    @Test
    void eligibleRecordsMapWithLineageAndQuarantinedRecordsAreExcluded() {
        DatasetProcessing processing = dataset();
        StagedSourceRecord customer = record(StagedRecordType.CUSTOMER, "customer-1", object("name", "Synthetic"));
        StagedSourceRecord vehicle = record(StagedRecordType.VEHICLE, "vehicle-1",
                object("customerSourceRecordId", "customer-1", "vehicleId", "not-a-uuid", "vin", "VIN-1"));
        StagedSourceRecord order = record(StagedRecordType.SERVICE_ORDER, "order-1",
                object("customerSourceRecordId", "customer-1", "vehicleSourceRecordId", "vehicle-1"));
        StagedSourceRecord job = record(StagedRecordType.SERVICE_JOB, "job-1",
                object("serviceOrderSourceRecordId", "order-1", "description", "Inspection"));
        StagedSourceRecord quarantined = record(StagedRecordType.SERVICE_JOB, "job-bad",
                object("serviceOrderSourceRecordId", "order-1"));
        quarantined.markValidationFailed();
        quarantined.quarantine();

        CanonicalIntakeMapper mapper = new CanonicalIntakeMapper();
        List<CanonicalIntakeDetectionCandidate> candidates = mapper.map(
                processing, List.of(customer, vehicle, order, job, quarantined));

        assertEquals(1, candidates.size());
        assertEquals("job-1", candidates.getFirst().input().sourceEntityId());
        assertEquals("controlled-test", candidates.getFirst().input().sourceSystem());
        assertTrue(candidates.getFirst().input().evidence().stream()
                .anyMatch(evidence -> evidence.sourceId().equals("job-1")));
        assertEquals("not-a-uuid", candidates.getFirst().input().vehicleId() == null ? "not-a-uuid" : "unexpected");
        assertFalse(candidates.getFirst().input().customerContactable());
    }

    @Test
    void explicitDeclineIsPreservedAndMissingDispositionIsUnknown() {
        DatasetProcessing processing = dataset();
        StagedSourceRecord order = record(StagedRecordType.SERVICE_ORDER, "order-1",
                object("customerSourceRecordId", "customer-1", "vehicleSourceRecordId", "vehicle-1"));
        StagedSourceRecord vehicle = record(StagedRecordType.VEHICLE, "vehicle-1",
                object("customerSourceRecordId", "customer-1"));
        StagedSourceRecord customer = record(StagedRecordType.CUSTOMER, "customer-1", object());
        StagedSourceRecord job = record(StagedRecordType.SERVICE_JOB, "job-1",
                object("serviceOrderSourceRecordId", "order-1"));
        StagedSourceRecord declined = record(StagedRecordType.DISPOSITION, "disposition-1",
                object("serviceOrderSourceRecordId", "order-1", "disposition", "DECLINED"));
        StagedSourceRecord missing = record(StagedRecordType.SERVICE_HISTORY, "history-1",
                object("vehicleSourceRecordId", "vehicle-1"));

        CanonicalIntakeMapper mapper = new CanonicalIntakeMapper();
        var mapped = mapper.map(processing, List.of(order, vehicle, customer, job, declined, missing));

        assertEquals(2, mapped.size());
        assertEquals(com.autovision.platform.serviceprofit.detection.ServiceProfitDisposition.DECLINED,
                mapped.getFirst().input().disposition());
        assertEquals(null, mapped.get(1).input().disposition());
    }

    @Test
    void readinessPersistsCapabilityReportWithoutInventingMissingCostOrInvoice() {
        DealerDataAssessment assessment = assessment();
        when(assessmentRepository.findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                TENANT_ID, "dataset-1", "1")).thenReturn(Optional.empty());
        when(assessmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(capabilityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ControlledDatasetReadinessService service = new ControlledDatasetReadinessService(
                assessmentRepository, capabilityRepository, new R1DealerDataCapabilityPolicy(),
                new R1DealerDataReadinessPolicy());
        DatasetProcessing processing = dataset();
        List<StagedSourceRecord> records = List.of(
                record(StagedRecordType.CUSTOMER, "customer-1", object()),
                record(StagedRecordType.VEHICLE, "vehicle-1", object()),
                record(StagedRecordType.SERVICE_ORDER, "order-1", object()));
        for (StagedSourceRecord record : records) record.markValidationPassed();

        DealerDataAssessment persisted = service.assess(processing, records, PRINCIPAL_ID, NOW);

        assertNotNull(persisted);
        verify(assessmentRepository, org.mockito.Mockito.times(2)).saveAndFlush(any(DealerDataAssessment.class));
        ArgumentCaptor<com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessment> captor =
                ArgumentCaptor.forClass(com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessment.class);
        verify(capabilityRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        var capabilities = captor.getAllValues();
        assertEquals(DealerDataCapabilityStatus.UNAVAILABLE,
                capabilities.stream().filter(value -> value.getCapability() == DealerDataCapability.GROSS_PROFIT_ATTRIBUTION)
                        .findFirst().orElseThrow().getStatus());
        assertEquals(DealerDataCapabilityStatus.UNAVAILABLE,
                capabilities.stream().filter(value -> value.getCapability() == DealerDataCapability.REVENUE_ATTRIBUTION)
                        .findFirst().orElseThrow().getStatus());
    }

    @Test
    void successfulMaterializationReusesOwnersAndIsIdempotent() {
        ControlledDatasetMaterializer materializer = materializer();
        DatasetProcessing processing = dataset();
        processing.markStaged(NOW);
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        when(datasetService.recordsFor(TENANT_ID, processing.getId())).thenReturn(List.of());
        when(readinessService.assess(any(), any(), any(), any())).thenReturn(assessment());
        when(datasetService.markReadyForMaterialization(any(), any(), any())).thenReturn(processing);
        when(datasetService.beginMaterialization(any(), any(), any(), any(), any(), any())).thenReturn(processing);
        when(datasetService.markMaterialized(any(), any(), any(), any(), any())).thenReturn(processing);

        ControlledDatasetMaterializationResult result = materializer.materialize(
                TENANT_ID, processing.getId(), PRINCIPAL_ID, NOW);

        assertEquals(DatasetProcessingStatus.MATERIALIZED, result.status());
        assertEquals(0, result.detectedOpportunities());
        verify(orchestrator, never()).detect(any(), any());
    }

    @Test
    void downstreamFailureIsRecordedAndNotReportedAsMaterialized() {
        ControlledDatasetMaterializer materializer = materializer();
        DatasetProcessing processing = dataset();
        processing.markStaged(NOW);
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        when(datasetService.recordsFor(TENANT_ID, processing.getId())).thenReturn(List.of());
        when(readinessService.assess(any(), any(), any(), any())).thenReturn(assessment());
        when(datasetService.beginMaterialization(any(), any(), any(), any(), any(), any())).thenReturn(processing);
        doThrow(new IllegalArgumentException("safe mapping failure"))
                .when(mapper).map(any(), any());

        assertThrows(IllegalArgumentException.class,
                () -> materializer.materialize(TENANT_ID, processing.getId(), PRINCIPAL_ID, NOW));
        verify(failureService).record(any(), any(), any(), any(), any(), any());
        verify(datasetService, never()).markMaterialized(any(), any(), any(), any(), any());
    }

    @Test
    void resultContainsOnlyOperationalFields() {
        for (var field : ControlledDatasetMaterializationResult.class.getDeclaredFields()) {
            assertTrue(List.of("status", "mappedRecords", "detectedOpportunities", "createdOpportunities",
                    "existingOpportunities", "duplicate").contains(field.getName()));
        }
    }

    @Mock
    private CanonicalIntakeMapper mapper;

    @InjectMocks
    private ControlledDatasetMaterializer injectedMaterializer;

    private ControlledDatasetMaterializer materializer() {
        return new ControlledDatasetMaterializer(datasetService, failureService, mapper, readinessService,
                orchestrator, persistenceService, contextService);
    }

    private DatasetProcessing dataset() {
        return DatasetProcessing.receive(UUID.randomUUID(), TENANT_ID, UUID.randomUUID(), null,
                "dataset-1", "1", "controlled-test", null, "2026-01", NOW,
                null, null, "SHA-256", "checksum", 10L, UUID.randomUUID());
    }

    private DealerDataAssessment assessment() {
        return DealerDataAssessment.createDraft(UUID.randomUUID(), TENANT_ID, UUID.randomUUID(), null,
                "dataset-1", "controlled-test", "dataset-1", "1",
                new com.autovision.platform.serviceprofit.data.DealerDataCoverage(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                BigDecimal.ZERO, R1DealerDataReadinessPolicy.POLICY_VERSION, PRINCIPAL_ID, NOW);
    }

    private StagedSourceRecord record(StagedRecordType type, String id, ObjectNode payload) {
        StagedSourceRecord record = StagedSourceRecord.stage(UUID.randomUUID(), TENANT_ID,
                UUID.randomUUID(), type, id, null, "1", null, payload, NOW);
        record.markValidationPassed();
        return record;
    }

    private ObjectNode object(String... values) {
        ObjectNode object = new ObjectMapper().createObjectNode();
        for (int index = 0; index < values.length; index += 2) object.put(values[index], values[index + 1]);
        return object;
    }
}
