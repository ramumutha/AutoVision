package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWorkflowStatusRepository
        extends JpaRepository<ServiceWorkflowStatus, UUID> {

    List<ServiceWorkflowStatus> findAllByWorkflowStageIdOrderBySequence(
            UUID workflowStageId
    );

    Optional<ServiceWorkflowStatus> findByWorkflowStageIdAndCode(
            UUID workflowStageId,
            String code
    );

    boolean existsByWorkflowStageIdAndCode(
            UUID workflowStageId,
            String code
    );
}