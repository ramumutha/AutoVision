package com.autovision.platform.intake;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class ControlledDatasetIntakeCommandService {

    private final ControlledDatasetIntakeService intakeService;
    private final ControlledDatasetFileAdapter fileAdapter;
    private final AuthorizationService authorizationService;
    private final DatasetProcessingService datasetService;
    private final DatasetOperationalEvidenceService evidenceService;
    private final Path packageRoot;
    private final Clock clock;

    @Autowired
    public ControlledDatasetIntakeCommandService(
            ControlledDatasetIntakeService intakeService,
            ControlledDatasetFileAdapter fileAdapter,
            AuthorizationService authorizationService,
            DatasetProcessingService datasetService,
            DatasetOperationalEvidenceService evidenceService,
            @Value("${AUTOVISION_CONTROLLED_INTAKE_PACKAGE_ROOT:/opt/autovision/controlled-intake}") String packageRoot
    ) {
        this(intakeService, fileAdapter, authorizationService, datasetService, evidenceService,
                Path.of(packageRoot), Clock.systemUTC());
    }

    ControlledDatasetIntakeCommandService(
            ControlledDatasetIntakeService intakeService,
            ControlledDatasetFileAdapter fileAdapter,
            AuthorizationService authorizationService,
            DatasetProcessingService datasetService,
            DatasetOperationalEvidenceService evidenceService,
            Path packageRoot,
            Clock clock
    ) {
        this.intakeService = intakeService;
        this.fileAdapter = fileAdapter;
        this.authorizationService = authorizationService;
        this.datasetService = datasetService;
        this.evidenceService = evidenceService;
        this.packageRoot = packageRoot.toAbsolutePath().normalize();
        this.clock = clock;
    }

    public ControlledDatasetProcessingResult process(
            AuthenticatedTenantContext context,
            String packageReference
    ) {
        if (context == null || packageReference == null || packageReference.isBlank()) {
            throw new IllegalArgumentException("Authenticated context and package reference are required");
        }
        Path packagePath = resolvePackage(packageReference);
        ParsedControlledDataset parsed = fileAdapter.read(packagePath);
        authorizationService.requirePermission(new AuthorizationRequest(
                context,
                ServiceProfitPermissions.CONTROLLED_DATA_INTAKE,
                AuthorizationResourceType.DEALER,
                parsed.envelope().dealerId()));
        if (parsed.envelope().locationId() != null) {
            authorizationService.requirePermission(new AuthorizationRequest(
                    context,
                    ServiceProfitPermissions.CONTROLLED_DATA_INTAKE,
                    AuthorizationResourceType.LOCATION,
                    parsed.envelope().locationId()));
        }
        ControlledDatasetProcessingResult result = intakeService.process(
            context, packagePath, OffsetDateTime.now(clock));
        DatasetProcessing processing = datasetService.requireDataset(context.tenantId(), result.datasetProcessingId());
        evidenceService.recordEvent(context.tenantId(), DatasetOperationalEvent.record(
            java.util.UUID.randomUUID(), processing, DatasetOperationalEventType.DATASET_RECEIVED,
            null, null, null, context.userRefId(), null, "CONTROLLED_DATASET_RECEIVED",
            "Controlled dataset receipt completed", OffsetDateTime.now(clock)));
        return result;
    }

    private Path resolvePackage(String packageReference) {
        Path reference = Path.of(packageReference);
        if (reference.isAbsolute() || packageReference.contains("..")) {
            throw new IllegalArgumentException("Controlled dataset package reference is invalid");
        }
        Path resolved = packageRoot.resolve(reference).normalize();
        if (!resolved.startsWith(packageRoot) || !Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("Controlled dataset package reference is invalid");
        }
        return resolved;
    }
}