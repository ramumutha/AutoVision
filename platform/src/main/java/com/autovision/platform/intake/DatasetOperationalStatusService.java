package com.autovision.platform.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DatasetOperationalStatusService {
    private final DatasetReconciliationService reconciliationService;
    private final DatasetOperationalEvidenceService evidenceService;

    public DatasetOperationalStatusService(DatasetReconciliationService reconciliationService,
            DatasetOperationalEvidenceService evidenceService) {
        this.reconciliationService = reconciliationService;
        this.evidenceService = evidenceService;
    }

    @Transactional(readOnly = true)
    public DatasetOperationalStatus lookup(UUID tenantId, UUID processingId) {
        return new DatasetOperationalStatus(reconciliationService.reconstruct(tenantId, processingId),
                evidenceService.findEvents(tenantId, processingId));
    }
}