package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWorkflowVersionRepository
        extends JpaRepository<ServiceWorkflowVersion, UUID> {

    List<ServiceWorkflowVersion>
    findAllByWorkflowDefinitionIdOrderByVersionNumber(
            UUID workflowDefinitionId
    );

    Optional<ServiceWorkflowVersion>
    findByWorkflowDefinitionIdAndVersionNumber(
            UUID workflowDefinitionId,
            long versionNumber
    );

    Optional<ServiceWorkflowVersion>
    findFirstByWorkflowDefinitionIdAndStatusOrderByVersionNumberDesc(
            UUID workflowDefinitionId,
            ServiceWorkflowVersionStatus status
    );

    boolean existsByWorkflowDefinitionIdAndVersionNumber(
            UUID workflowDefinitionId,
            long versionNumber
    );
}