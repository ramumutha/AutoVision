package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWorkflowStageRepository
        extends JpaRepository<ServiceWorkflowStage, UUID> {

    List<ServiceWorkflowStage> findAllByWorkflowVersionIdOrderBySequence(
            UUID workflowVersionId
    );

    Optional<ServiceWorkflowStage> findByIdAndWorkflowVersionId(
            UUID id,
            UUID workflowVersionId
    );

    Optional<ServiceWorkflowStage> findByWorkflowVersionIdAndCode(
            UUID workflowVersionId,
            String code
    );

    boolean existsByWorkflowVersionIdAndCode(
            UUID workflowVersionId,
            String code
    );
}