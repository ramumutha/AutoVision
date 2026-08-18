package com.autovision.platform.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWorkflowDefinitionRepository
        extends JpaRepository<ServiceWorkflowDefinition, UUID> {

    List<ServiceWorkflowDefinition> findAllByTenantId(
            UUID tenantId
    );

    List<ServiceWorkflowDefinition> findAllByTenantIdAndActiveTrue(
            UUID tenantId
    );

    Optional<ServiceWorkflowDefinition> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Optional<ServiceWorkflowDefinition>
    findByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
            UUID tenantId,
            String code
    );

    boolean existsByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
            UUID tenantId,
            String code
    );
}