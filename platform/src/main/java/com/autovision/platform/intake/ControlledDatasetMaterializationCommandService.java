package com.autovision.platform.intake;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ControlledDatasetMaterializationCommandService {

    private final DatasetProcessingService datasetService;
    private final ControlledDatasetMaterializer materializer;
    private final AuthorizationService authorizationService;
    private final DatasetOperationalEvidenceService evidenceService;
    private final Clock clock;

        @Autowired
        public ControlledDatasetMaterializationCommandService(
            DatasetProcessingService datasetService,
            ControlledDatasetMaterializer materializer,
            AuthorizationService authorizationService,
            DatasetOperationalEvidenceService evidenceService
    ) {
        this(datasetService, materializer, authorizationService, evidenceService, Clock.systemUTC());
    }

    ControlledDatasetMaterializationCommandService(
            DatasetProcessingService datasetService,
            ControlledDatasetMaterializer materializer,
            AuthorizationService authorizationService,
            DatasetOperationalEvidenceService evidenceService,
            Clock clock
    ) {
        this.datasetService = datasetService;
        this.materializer = materializer;
        this.authorizationService = authorizationService;
        this.evidenceService = evidenceService;
        this.clock = clock;
    }

    public ControlledDatasetMaterializationResult materialize(
            AuthenticatedTenantContext context,
            UUID datasetProcessingId
    ) {
        if (context == null || context.tenantId() == null || context.userRefId() == null
                || datasetProcessingId == null) {
            throw new IllegalArgumentException("Authenticated context and dataset are required");
        }
        DatasetProcessing processing = authorizedProcessing(context, datasetProcessingId);
        if (processing.getStatus() != DatasetProcessingStatus.READY_FOR_MATERIALIZATION
                && processing.getStatus() != DatasetProcessingStatus.MATERIALIZATION_FAILED
                && processing.getStatus() != DatasetProcessingStatus.MATERIALIZED) {
            throw new IllegalStateException("Dataset has not passed the materialization approval checkpoint");
        }
        evidenceService.recordEvent(context.tenantId(), DatasetOperationalEvent.record(
                UUID.randomUUID(), processing, DatasetOperationalEventType.MATERIALIZATION_AUTHORIZED,
                processing.getMaterializationKey(), processing.getMaterializationVersion(),
                Math.max(1, processing.getMaterializationAttemptCount() + 1), context.userRefId(), null,
                "CONTROLLED_DATASET_MATERIALIZATION_AUTHORIZED",
                "Controlled dataset materialization authorized", OffsetDateTime.now(clock)));
        return materializer.materialize(
            context.tenantId(), datasetProcessingId, context.userRefId(), OffsetDateTime.now(clock));
    }

    public DatasetProcessing approve(
            AuthenticatedTenantContext context,
            UUID datasetProcessingId
    ) {
        DatasetProcessing processing = authorizedProcessing(context, datasetProcessingId);
        if (processing.getStatus() != DatasetProcessingStatus.STAGED) {
            throw new IllegalStateException("Only staged datasets can be approved for materialization");
        }
        DatasetProcessing approved = datasetService.markReadyForMaterialization(
                context.tenantId(), datasetProcessingId, OffsetDateTime.now(clock));
        evidenceService.recordEvent(context.tenantId(), DatasetOperationalEvent.record(
                UUID.randomUUID(), approved, DatasetOperationalEventType.MATERIALIZATION_AUTHORIZED,
                null, null, null, context.userRefId(), null,
                "CONTROLLED_DATASET_MATERIALIZATION_APPROVED",
                "Controlled dataset passed the materialization approval checkpoint",
                OffsetDateTime.now(clock)));
        return approved;
    }

    private DatasetProcessing authorizedProcessing(
            AuthenticatedTenantContext context,
            UUID datasetProcessingId
    ) {
        if (context == null || context.tenantId() == null || context.userRefId() == null
                || datasetProcessingId == null) {
            throw new IllegalArgumentException("Authenticated context and dataset are required");
        }
        DatasetProcessing processing = datasetService.requireDataset(context.tenantId(), datasetProcessingId);
        if (processing.getDealerId() == null) {
            throw new IllegalStateException("Dataset dealer is required for authorization");
        }
        authorizationService.requirePermission(new AuthorizationRequest(
                context, ServiceProfitPermissions.CONTROLLED_DATA_INTAKE,
                AuthorizationResourceType.DEALER, processing.getDealerId()));
        if (processing.getLocationId() != null) {
            authorizationService.requirePermission(new AuthorizationRequest(
                    context, ServiceProfitPermissions.CONTROLLED_DATA_INTAKE,
                    AuthorizationResourceType.LOCATION, processing.getLocationId()));
        }
        return processing;
    }
}