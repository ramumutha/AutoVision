package com.autovision.platform.workflow.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderWorkflowExecutionRepository
        extends JpaRepository<ServiceOrderWorkflowExecution, UUID> {

    Optional<ServiceOrderWorkflowExecution> findByServiceOrderId(
            UUID serviceOrderId
    );

    Optional<ServiceOrderWorkflowExecution> findByServiceOrderIdAndTenantId(
            UUID serviceOrderId,
            UUID tenantId
    );

    boolean existsByServiceOrderId(UUID serviceOrderId);

    boolean existsByServiceOrderIdAndTenantId(
            UUID serviceOrderId,
            UUID tenantId
    );

    Optional<ServiceOrderWorkflowExecution> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );
}