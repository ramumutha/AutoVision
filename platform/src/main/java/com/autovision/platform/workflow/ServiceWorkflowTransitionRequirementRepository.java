package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceWorkflowTransitionRequirementRepository
        extends JpaRepository<ServiceWorkflowTransitionRequirement, UUID> {

    List<ServiceWorkflowTransitionRequirement>
    findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
            UUID workflowVersionId,
            UUID workflowTransitionId
    );

    boolean existsByWorkflowTransitionIdAndRequirementKey(
            UUID workflowTransitionId,
            String requirementKey
    );
}