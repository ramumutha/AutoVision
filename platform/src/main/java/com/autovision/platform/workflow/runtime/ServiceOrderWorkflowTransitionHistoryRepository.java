package com.autovision.platform.workflow.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderWorkflowTransitionHistoryRepository
        extends JpaRepository<ServiceOrderWorkflowTransitionHistory, UUID> {

    List<ServiceOrderWorkflowTransitionHistory>
    findByServiceOrderIdAndTenantIdOrderByExecutedAtAsc(
            UUID serviceOrderId,
            UUID tenantId
    );

    List<ServiceOrderWorkflowTransitionHistory>
    findByWorkflowExecutionIdAndTenantIdOrderByExecutedAtAsc(
            UUID workflowExecutionId,
            UUID tenantId
    );

    Optional<ServiceOrderWorkflowTransitionHistory> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );
}
