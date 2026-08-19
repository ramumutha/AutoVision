package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWorkflowTransitionRepository
        extends JpaRepository<ServiceWorkflowTransition, UUID> {

    List<ServiceWorkflowTransition> findByWorkflowVersionIdOrderBySequenceAsc(
            UUID workflowVersionId
    );

    Optional<ServiceWorkflowTransition> findByIdAndWorkflowVersionId(
            UUID id,
            UUID workflowVersionId
    );

    boolean existsByWorkflowVersionIdAndCode(
            UUID workflowVersionId,
            String code
    );

    boolean existsByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndToStageIdAndToStatusId(
            UUID workflowVersionId,
            UUID fromStageId,
            UUID fromStatusId,
            UUID toStageId,
            UUID toStatusId
    );

    List<ServiceWorkflowTransition>
    findByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndActiveTrueOrderBySequenceAsc(
            UUID workflowVersionId,
            UUID fromStageId,
            UUID fromStatusId
    );
}