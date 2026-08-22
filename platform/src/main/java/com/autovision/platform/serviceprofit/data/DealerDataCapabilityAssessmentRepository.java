package com.autovision.platform.serviceprofit.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DealerDataCapabilityAssessmentRepository
        extends JpaRepository<DealerDataCapabilityAssessment, UUID> {

    List<DealerDataCapabilityAssessment>
    findByAssessmentIdOrderByCapabilityAsc(
            UUID assessmentId
    );

    void deleteByAssessmentId(UUID assessmentId);
}
