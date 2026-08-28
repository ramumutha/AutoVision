package com.autovision.platform.intake;

import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ControlledDatasetIntakeCommandServiceTests {

    @Test
    void rejectsAbsoluteAndTraversalPackageReferencesBeforeAuthorization() throws Exception {
        Path root = Files.createTempDirectory("controlled-intake-test");
        ControlledDatasetIntakeService intake = mock(ControlledDatasetIntakeService.class);
        ControlledDatasetFileAdapter adapter = mock(ControlledDatasetFileAdapter.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        DatasetProcessingService datasetService = mock(DatasetProcessingService.class);
        DatasetOperationalEvidenceService evidenceService = mock(DatasetOperationalEvidenceService.class);
        ControlledDatasetIntakeCommandService service = new ControlledDatasetIntakeCommandService(
                intake, adapter, authorization, datasetService, evidenceService, root,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
        AuthenticatedTenantContext context = new AuthenticatedTenantContext(
                UUID.randomUUID(), UUID.randomUUID(), "operator");

        assertThrows(IllegalArgumentException.class, () -> service.process(context, "../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.process(context, root.toString()));

        verify(adapter, never()).read(any());
        verify(authorization, never()).requirePermission(any(com.autovision.platform.authorization.AuthorizationRequest.class));
    }

    @Test
    void materializationCommandCannotBypassApprovalCheckpoint() {
        DatasetProcessingService datasetService = mock(DatasetProcessingService.class);
        ControlledDatasetMaterializer materializer = mock(ControlledDatasetMaterializer.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        DatasetOperationalEvidenceService evidenceService = mock(DatasetOperationalEvidenceService.class);
        ControlledDatasetMaterializationCommandService service =
            new ControlledDatasetMaterializationCommandService(
                datasetService, materializer, authorization, evidenceService,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = DatasetProcessing.receive(
            UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
            "dataset", "1", "source", null, "schema",
            Instant.parse("2026-08-28T00:00:00Z").atOffset(ZoneOffset.UTC),
            null, null, "SHA-256", "checksum", 1L, UUID.randomUUID());
        when(datasetService.requireDataset(tenantId, processing.getId())).thenReturn(processing);

        AuthenticatedTenantContext context = new AuthenticatedTenantContext(
            UUID.randomUUID(), tenantId, "operator");

        assertThrows(IllegalStateException.class,
            () -> service.materialize(context, processing.getId()));
        verify(materializer, never()).materialize(any(), any(), any(), any());
        }

        @Test
        void authorizationEvidenceIsRecordedBeforeSuccessfulMaterialization() {
        DatasetProcessingService datasetService = mock(DatasetProcessingService.class);
        ControlledDatasetMaterializer materializer = mock(ControlledDatasetMaterializer.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        DatasetOperationalEvidenceService evidenceService = mock(DatasetOperationalEvidenceService.class);
        ControlledDatasetMaterializationCommandService service =
            new ControlledDatasetMaterializationCommandService(
                datasetService, materializer, authorization, evidenceService,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = readyDataset(tenantId);
        AuthenticatedTenantContext context = new AuthenticatedTenantContext(
            UUID.randomUUID(), tenantId, "operator");
        when(datasetService.requireDataset(tenantId, processing.getId())).thenReturn(processing);
        when(materializer.materialize(tenantId, processing.getId(), context.userRefId(),
            Instant.parse("2026-08-28T00:00:00Z").atOffset(ZoneOffset.UTC)))
            .thenReturn(new ControlledDatasetMaterializationResult(
                DatasetProcessingStatus.MATERIALIZED, 1, 1, 1, 0, false));

        service.materialize(context, processing.getId());

        var order = inOrder(evidenceService, materializer);
        order.verify(evidenceService).recordEvent(eq(tenantId), any());
        order.verify(materializer).materialize(eq(tenantId), eq(processing.getId()),
            eq(context.userRefId()), any());
        }

        @Test
        void authorizationEvidencePrecedesFailedMaterializationAttempt() {
        DatasetProcessingService datasetService = mock(DatasetProcessingService.class);
        ControlledDatasetMaterializer materializer = mock(ControlledDatasetMaterializer.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        DatasetOperationalEvidenceService evidenceService = mock(DatasetOperationalEvidenceService.class);
        ControlledDatasetMaterializationCommandService service =
            new ControlledDatasetMaterializationCommandService(
                datasetService, materializer, authorization, evidenceService,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
        UUID tenantId = UUID.randomUUID();
        DatasetProcessing processing = readyDataset(tenantId);
        AuthenticatedTenantContext context = new AuthenticatedTenantContext(
            UUID.randomUUID(), tenantId, "operator");
        when(datasetService.requireDataset(tenantId, processing.getId())).thenReturn(processing);
        when(materializer.materialize(eq(tenantId), eq(processing.getId()), eq(context.userRefId()), any()))
            .thenThrow(new IllegalStateException("materialization failed"));

        assertThrows(IllegalStateException.class, () -> service.materialize(context, processing.getId()));

        var order = inOrder(evidenceService, materializer);
        order.verify(evidenceService).recordEvent(eq(tenantId), any());
        order.verify(materializer).materialize(eq(tenantId), eq(processing.getId()),
            eq(context.userRefId()), any());
        }

        private DatasetProcessing readyDataset(UUID tenantId) {
        DatasetProcessing processing = DatasetProcessing.receive(
            UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
            "dataset", "1", "source", null, "schema",
            Instant.parse("2026-08-28T00:00:00Z").atOffset(ZoneOffset.UTC),
            null, null, "SHA-256", "checksum", 1L, UUID.randomUUID());
        processing.markStaged(Instant.parse("2026-08-28T00:00:00Z").atOffset(ZoneOffset.UTC));
        processing.markReadyForMaterialization(Instant.parse("2026-08-28T00:00:00Z").atOffset(ZoneOffset.UTC));
        return processing;
        }
}