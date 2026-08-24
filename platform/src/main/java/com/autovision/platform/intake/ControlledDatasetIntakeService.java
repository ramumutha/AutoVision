package com.autovision.platform.intake;

import com.autovision.platform.organization.DealerRepository;
import com.autovision.platform.organization.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ControlledDatasetIntakeService {

    private final ControlledDatasetFileAdapter adapter;
    private final DatasetEnvelopeValidator envelopeValidator;
    private final StagedRecordValidator recordValidator;
    private final DatasetProcessingRepository datasetRepository;
    private final DatasetProcessingService persistenceService;
    private final DealerRepository dealerRepository;
    private final LocationRepository locationRepository;

    public ControlledDatasetIntakeService(
            ControlledDatasetFileAdapter adapter,
            DatasetEnvelopeValidator envelopeValidator,
            StagedRecordValidator recordValidator,
            DatasetProcessingRepository datasetRepository,
            DatasetProcessingService persistenceService,
            DealerRepository dealerRepository,
            LocationRepository locationRepository
    ) {
        this.adapter = adapter;
        this.envelopeValidator = envelopeValidator;
        this.recordValidator = recordValidator;
        this.datasetRepository = datasetRepository;
        this.persistenceService = persistenceService;
        this.dealerRepository = dealerRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public ControlledDatasetProcessingResult process(Path packagePath, OffsetDateTime receivedAt) {
        if (receivedAt == null) throw new IllegalArgumentException("Receipt time is required");
        ParsedControlledDataset parsed = adapter.read(packagePath);
        ControlledDatasetEnvelope envelope = parsed.envelope();
        List<IntakeFinding> envelopeFindings = new ArrayList<>(envelopeValidator.validate(envelope));
        validateContainment(envelope, envelopeFindings);

        DatasetProcessing existing = datasetRepository
                .findByTenantIdAndDatasetIdAndDatasetVersion(
                        envelope.tenantId(), envelope.datasetId(), envelope.datasetVersion())
                .orElse(null);
        if (existing != null) {
            if (parsed.contentChecksum().equals(existing.getContentChecksum())
                    && parsed.contentByteSize() == existing.getContentByteSize()) {
                return result(existing, 0, 0, 0, 0, 0, 0, true);
            }
            throw new IllegalStateException("Dataset identity already exists with different content");
        }

        DatasetProcessing processing = DatasetProcessing.receive(
                UUID.randomUUID(), envelope.tenantId(), envelope.dealerId(), envelope.locationId(),
                envelope.datasetId(), envelope.datasetVersion(), envelope.sourceSystem(),
                envelope.sourceProvider(), envelope.sourceSchemaVersion(), receivedAt,
                envelope.effectiveFrom(), envelope.effectiveTo(), "SHA-256",
                parsed.contentChecksum(), parsed.contentByteSize(), UUID.randomUUID());
        persistenceService.receiveDataset(processing);

        int fatal = count(envelopeFindings, ValidationSeverity.FATAL);
        int errors = count(envelopeFindings, ValidationSeverity.ERROR);
        int warnings = count(envelopeFindings, ValidationSeverity.WARNING);
        int info = count(envelopeFindings, ValidationSeverity.INFO);
        for (IntakeFinding finding : envelopeFindings) attach(processing, finding, null, receivedAt);
        if (fatal > 0 || errors > 0) {
            persistenceService.markValidationFailed(envelope.tenantId(), processing.getId(), receivedAt);
            return result(processing, 0, 0, fatal, errors, warnings, info, false);
        }

        List<IntakeFinding> recordFindings = recordValidator.validate(envelope, parsed.records());
        int quarantined = 0;
        for (ParsedSourceRecord sourceRecord : parsed.records()) {
            List<IntakeFinding> findings = recordFindings.stream()
                    .filter(finding -> sourceRecord.sourceRecordId() == null
                            || sourceRecord.sourceRecordId().equals(finding.sourceRecordId()))
                    .toList();
            if (sourceRecord.sourceRecordId() == null || sourceRecord.sourceRecordId().isBlank()) {
                for (IntakeFinding finding : findings) {
                    attach(processing, finding, null, receivedAt);
                    if (finding.severity() == ValidationSeverity.FATAL) fatal++;
                    if (finding.severity() == ValidationSeverity.ERROR) errors++;
                    if (finding.severity() == ValidationSeverity.WARNING) warnings++;
                    if (finding.severity() == ValidationSeverity.INFO) info++;
                }
                quarantined++;
                continue;
            }
            StagedSourceRecord staged = persistenceService.stageRecord(
                    envelope.tenantId(), processing.getId(), sourceRecord.recordType(),
                    sourceRecord.sourceRecordId(), sourceRecord.sourceParentId(),
                    sourceRecord.sourceVersion(), null, sourceRecord.payload(), receivedAt);
            boolean unsafe = findings.stream().anyMatch(finding ->
                    finding.severity() == ValidationSeverity.FATAL
                            || finding.severity() == ValidationSeverity.ERROR);
            for (IntakeFinding finding : findings) {
                attach(processing, finding, staged.getId(), receivedAt);
                if (finding.severity() == ValidationSeverity.FATAL) fatal++;
                if (finding.severity() == ValidationSeverity.ERROR) errors++;
                if (finding.severity() == ValidationSeverity.WARNING) warnings++;
                if (finding.severity() == ValidationSeverity.INFO) info++;
            }
            if (unsafe) {
                persistenceService.quarantineRecord(envelope.tenantId(), processing.getId(), staged.getId(), receivedAt);
            } else {
                persistenceService.markRecordPassed(envelope.tenantId(), processing.getId(), staged.getId());
            }
        }
        quarantined = (int) persistenceService.recordsFor(envelope.tenantId(), processing.getId()).stream()
                .filter(record -> record.getState() == StagedRecordState.QUARANTINED).count();
        int staged = parsed.records().size() - quarantined;
        if (errors > 0 && processing.getStatus() == DatasetProcessingStatus.STAGED) {
            persistenceService.markValidationFailed(envelope.tenantId(), processing.getId(), receivedAt);
        }
        return result(processing, parsed.records().size(), staged, quarantined,
                fatal, errors, warnings, info, false);
    }

    private void validateContainment(ControlledDatasetEnvelope envelope, List<IntakeFinding> findings) {
        if (envelope.tenantId() == null || envelope.dealerId() == null
                || !dealerRepository.findByIdAndTenantId(envelope.dealerId(), envelope.tenantId()).isPresent()) {
            findings.add(new IntakeFinding(ValidationStage.CONTAINMENT, ValidationSeverity.FATAL,
                    IntakeValidationCode.INTAKE_DEALER_CONTAINMENT_MISMATCH, "dealerId",
                    "The dealer is not contained by the tenant", null));
        }
        if (envelope.locationId() != null
                && locationRepository.findByIdAndTenantId(envelope.locationId(), envelope.tenantId()).isEmpty()) {
            findings.add(new IntakeFinding(ValidationStage.CONTAINMENT, ValidationSeverity.FATAL,
                    IntakeValidationCode.INTAKE_LOCATION_CONTAINMENT_MISMATCH, "locationId",
                    "The location is not contained by the tenant", null));
        }
    }

    private void attach(DatasetProcessing processing, IntakeFinding finding, UUID recordId, OffsetDateTime now) {
        persistenceService.attachFinding(processing.getTenantId(), processing.getId(), recordId,
                finding.stage(), finding.severity(), finding.code().name(), finding.fieldPath(),
                finding.safeMessage(), null, now);
    }

    private int count(List<IntakeFinding> findings, ValidationSeverity severity) {
        return (int) findings.stream().filter(finding -> finding.severity() == severity).count();
    }

    private ControlledDatasetProcessingResult result(DatasetProcessing processing, int received,
                                                     int staged, int quarantined, int fatal,
                                                     int errors, int warnings, int info,
                                                     boolean duplicate) {
        return new ControlledDatasetProcessingResult(processing.getId(), processing.getDatasetId(),
                processing.getDatasetVersion(), processing.getStatus(), received, staged,
                quarantined, fatal, errors, warnings, info, duplicate);
    }

    private ControlledDatasetProcessingResult result(DatasetProcessing processing, int received,
                                                     int staged, int fatal, int errors,
                                                     int warnings, int info, boolean duplicate) {
        return result(processing, received, staged, 0, fatal, errors, warnings, info, duplicate);
    }
}