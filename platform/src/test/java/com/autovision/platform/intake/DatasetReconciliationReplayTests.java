package com.autovision.platform.intake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetReconciliationReplayTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T08:00:00Z");
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PRINCIPAL_ID = UUID.randomUUID();

    @Mock DatasetProcessingService datasetService;
    @Mock DatasetOperationalEvidenceService evidenceService;
    @Mock ControlledDatasetMaterializer materializer;

    @Test
    void materializedSameIdentityIsDeterministicNoOp() {
        DatasetProcessing processing = materialized();
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        DatasetReplayService service = new DatasetReplayService(datasetService, materializer, evidenceService);

        DatasetReplayResult result = service.replay(TENANT_ID, processing.getId(), PRINCIPAL_ID, "operator-app",
            "materialization-key", "1", NOW);

        assertTrue(result.noOp());
        assertEquals(2, result.attempt());
        verify(materializer, never()).materialize(any(), any(), any(), any());
        verify(evidenceService, never()).recordEvent(any(), any());
    }

    @Test
    void failedReplayRecordsRequestStartAndSuccessEvents() {
        DatasetProcessing processing = failedReplayable();
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        when(materializer.materialize(TENANT_ID, processing.getId(), PRINCIPAL_ID, NOW))
                .thenReturn(new ControlledDatasetMaterializationResult(DatasetProcessingStatus.MATERIALIZED, 2, 1, 1, 0, false));
        DatasetProcessing completed = materialized();
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing, processing, completed);
        DatasetReplayService service = new DatasetReplayService(datasetService, materializer, evidenceService);

        DatasetReplayResult result = service.replay(TENANT_ID, processing.getId(), PRINCIPAL_ID, "operator-app", NOW);

        assertEquals(DatasetProcessingStatus.MATERIALIZED, result.status());
        assertFalse(result.noOp());
        ArgumentCaptor<DatasetOperationalEvent> events = ArgumentCaptor.forClass(DatasetOperationalEvent.class);
        verify(evidenceService, org.mockito.Mockito.times(3)).recordEvent(org.mockito.ArgumentMatchers.eq(TENANT_ID), events.capture());
        assertEquals(List.of(DatasetOperationalEventType.REPLAY_REQUESTED,
                DatasetOperationalEventType.REPLAY_STARTED, DatasetOperationalEventType.REPLAY_SUCCEEDED),
                events.getAllValues().stream().map(DatasetOperationalEvent::getEventType).toList());
    }

    @Test
    void replayRejectsUnsafeStateAndMissingExecutionPrincipal() {
        DatasetProcessing processing = DatasetProcessing.receive(UUID.randomUUID(), TENANT_ID, null, null,
                "dataset", "1", "controlled", null, "schema", NOW, null, null, null, null, null,
                UUID.randomUUID());
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        DatasetReplayService service = new DatasetReplayService(datasetService, materializer, evidenceService);

        assertThrows(IllegalStateException.class,
                () -> service.replay(TENANT_ID, processing.getId(), PRINCIPAL_ID, "app", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> service.replay(TENANT_ID, processing.getId(), null, "app", NOW));
    }

    @Test
    void recoveryPreservesIdentityAndAttemptAndRecordsSafeEvent() {
        DatasetProcessing processing = failedMaterializing();
        when(datasetService.requireDataset(TENANT_ID, processing.getId())).thenReturn(processing);
        when(datasetService.receiveDataset(processing)).thenReturn(processing);
        DatasetMaterializationRecoveryService service = new DatasetMaterializationRecoveryService(datasetService, evidenceService);

        DatasetProcessing recovered = service.markAbandonedMaterializationFailedForRecovery(TENANT_ID, processing.getId(),
                null, "recovery-app", "ABANDONED_TIMEOUT_REVIEW", "External outcome could not be verified", true, NOW);

        assertEquals(DatasetProcessingStatus.MATERIALIZATION_FAILED, recovered.getStatus());
        assertEquals(1, recovered.getMaterializationAttemptCount());
        assertEquals("materialization-key", recovered.getMaterializationKey());
        assertEquals(Boolean.TRUE, recovered.getMaterializationFailureReplayable());
        ArgumentCaptor<DatasetOperationalEvent> event = ArgumentCaptor.forClass(DatasetOperationalEvent.class);
        verify(evidenceService).recordEvent(org.mockito.ArgumentMatchers.eq(TENANT_ID), event.capture());
        assertEquals(DatasetOperationalEventType.ABANDONED_MATERIALIZATION_MARKED_FAILED, event.getValue().getEventType());
        assertEquals("recovery-app", event.getValue().getApplicationId());
    }

    private DatasetProcessing failedReplayable() {
        DatasetProcessing processing = failedMaterializing();
        processing.recordMaterializationFailure("materialization-key", "1", "safe failure",
                MaterializationFailureStage.PERSISTENCE, MaterializationFailureCode.PERSISTENCE_FAILED, true, NOW.plusSeconds(1));
        return processing;
    }

    private DatasetProcessing failedMaterializing() {
        DatasetProcessing processing = DatasetProcessing.receive(UUID.randomUUID(), TENANT_ID, null, null,
                "dataset", "1", "controlled", null, "schema", NOW, null, null, null, null, null,
                UUID.randomUUID());
        processing.markStaged(NOW);
        processing.markReadyForMaterialization(NOW);
        processing.beginMaterialization("materialization-key", "1", UUID.randomUUID(), NOW);
        return processing;
    }

    private DatasetProcessing materialized() {
        DatasetProcessing processing = failedMaterializing();
        processing.markMaterializationFailed("materialization-key", "1", "safe failure", NOW.plusSeconds(1));
        processing.beginMaterialization("materialization-key", "1", UUID.randomUUID(), NOW.plusSeconds(2));
        processing.markMaterialized("materialization-key", "1", NOW.plusSeconds(3));
        return processing;
    }
}
