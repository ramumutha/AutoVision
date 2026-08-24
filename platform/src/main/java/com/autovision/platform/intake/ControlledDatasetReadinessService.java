package com.autovision.platform.intake;

import com.autovision.platform.serviceprofit.data.DealerDataAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityResult;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureAvailability;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureProfile;
import com.autovision.platform.serviceprofit.data.DealerDataCoverage;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.data.R1DealerDataReadinessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ControlledDatasetReadinessService {

    private final DealerDataAssessmentRepository assessmentRepository;
    private final DealerDataCapabilityAssessmentRepository capabilityRepository;
    private final R1DealerDataCapabilityPolicy capabilityPolicy;
    private final R1DealerDataReadinessPolicy readinessPolicy;

    public ControlledDatasetReadinessService(
            DealerDataAssessmentRepository assessmentRepository,
            DealerDataCapabilityAssessmentRepository capabilityRepository,
            R1DealerDataCapabilityPolicy capabilityPolicy,
            R1DealerDataReadinessPolicy readinessPolicy
    ) {
        this.assessmentRepository = assessmentRepository;
        this.capabilityRepository = capabilityRepository;
        this.capabilityPolicy = capabilityPolicy;
        this.readinessPolicy = readinessPolicy;
    }

    @Transactional
    public DealerDataAssessment assess(
            DatasetProcessing processing,
            List<StagedSourceRecord> records,
            UUID principalId,
            OffsetDateTime now
    ) {
        require(processing, records, principalId, now);
        var existing = assessmentRepository
                .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                        processing.getTenantId(), processing.getDatasetId(), processing.getDatasetVersion());
        if (existing.isPresent()) return existing.get();

        DealerDataCoverage coverage = coverage(records);
        DealerDataAssessment assessment = assessmentRepository.saveAndFlush(
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(), processing.getTenantId(), processing.getDealerId(), null,
                        processing.getDatasetId(), processing.getSourceSystem(), processing.getDatasetId(),
                        processing.getDatasetVersion(), coverage, readinessPolicy.calculateOverallScore(coverage),
                        readinessPolicy.policyVersion(), principalId, now));
        for (DealerDataCapabilityResult result : capabilityPolicy.evaluate(features(records))) {
            capabilityRepository.save(DealerDataCapabilityAssessment.create(
                    UUID.randomUUID(), assessment.getId(), result, principalId, now));
        }
        capabilityRepository.flush();
        assessment.complete(principalId, now);
        return assessmentRepository.saveAndFlush(assessment);
    }

    private DealerDataCoverage coverage(List<StagedSourceRecord> records) {
        BigDecimal total = BigDecimal.valueOf(records.size());
        if (records.isEmpty()) return new DealerDataCoverage(zero(), zero(), zero(), zero(), zero(), zero(), zero(), zero());
        return new DealerDataCoverage(
                percentage(records, StagedRecordType.CUSTOMER, total),
                percentage(records, StagedRecordType.VEHICLE, total),
                percentage(records, StagedRecordType.SERVICE_ORDER, total),
                percentage(records, StagedRecordType.RECOMMENDATION, total),
                percentage(records, StagedRecordType.DISPOSITION, total),
                percentage(records, StagedRecordType.SERVICE_HISTORY, total),
                percentage(records, StagedRecordType.INVOICE, total),
                percentage(records, StagedRecordType.COST, total));
    }

    private BigDecimal percentage(List<StagedSourceRecord> records, StagedRecordType type, BigDecimal total) {
        long count = records.stream().filter(record -> record.getRecordType() == type).count();
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100)).divide(total, 2, java.math.RoundingMode.HALF_UP);
    }

    private DealerDataFeatureProfile features(List<StagedSourceRecord> records) {
        return new DealerDataFeatureProfile(
                availability(records, StagedRecordType.DISPOSITION),
                availability(records, StagedRecordType.NOTE),
                availability(records, StagedRecordType.RECOMMENDATION),
                availability(records, StagedRecordType.SERVICE_HISTORY),
                availability(records, StagedRecordType.SERVICE_HISTORY),
                availability(records, StagedRecordType.CUSTOMER),
                availability(records, StagedRecordType.INVOICE),
                availability(records, StagedRecordType.COST));
    }

    private DealerDataFeatureAvailability availability(List<StagedSourceRecord> records, StagedRecordType type) {
        long count = records.stream().filter(record -> record.getRecordType() == type).count();
        if (count == 0) return DealerDataFeatureAvailability.UNAVAILABLE;
        return count == records.size() ? DealerDataFeatureAvailability.AVAILABLE : DealerDataFeatureAvailability.PARTIAL;
    }

    private BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }

    private void require(DatasetProcessing processing, List<StagedSourceRecord> records, UUID principalId, OffsetDateTime now) {
        if (processing == null || records == null || principalId == null || now == null) {
            throw new IllegalArgumentException("Dataset, records, principal ID, and assessment time are required");
        }
    }
}
