package com.autovision.platform.workflow;

import com.autovision.platform.organization.Branch;
import com.autovision.platform.organization.BranchRepository;
import com.autovision.platform.organization.DealerRepository;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceWorkflowConfigurationCommandService {

    private final ServiceWorkflowDefinitionRepository definitionRepository;
    private final ServiceWorkflowVersionRepository versionRepository;
    private final ServiceWorkflowStageRepository stageRepository;
    private final ServiceWorkflowStatusRepository statusRepository;
    private final DealerRepository dealerRepository;
    private final BranchRepository branchRepository;

    public ServiceWorkflowConfigurationCommandService(
            ServiceWorkflowDefinitionRepository definitionRepository,
            ServiceWorkflowVersionRepository versionRepository,
            ServiceWorkflowStageRepository stageRepository,
            ServiceWorkflowStatusRepository statusRepository,
            DealerRepository dealerRepository,
            BranchRepository branchRepository
    ) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.dealerRepository = dealerRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public ServiceWorkflowDefinition createDefinition(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId,
            UUID branchId,
            String code,
            String displayName
    ) {
        requireTenantContext(tenantContext);
        validateScope(tenantContext, dealerId, branchId);

        boolean duplicate = dealerId == null
                ? definitionRepository
                        .existsByTenantIdAndDealerIdIsNullAndBranchIdIsNullAndCode(
                                tenantContext.tenantId(), code)
                : branchId == null
                        ? definitionRepository
                                .existsByTenantIdAndDealerIdAndBranchIdIsNullAndCode(
                                        tenantContext.tenantId(), dealerId, code)
                        : definitionRepository
                                .existsByTenantIdAndDealerIdAndBranchIdAndCode(
                                        tenantContext.tenantId(), dealerId, branchId, code);
        if (duplicate) {
            throw conflict("Workflow definition code already exists in scope");
        }

        try {
            return definitionRepository.save(ServiceWorkflowDefinition.create(
                    UUID.randomUUID(),
                    tenantContext.tenantId(),
                    dealerId,
                    branchId,
                    code,
                    displayName,
                    tenantContext.userRefId(),
                    OffsetDateTime.now()
            ));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    @Transactional
    public ServiceWorkflowVersion createDraftVersion(
            AuthenticatedTenantContext tenantContext,
            UUID definitionId,
            long versionNumber
    ) {
        requireTenantContext(tenantContext);
        ServiceWorkflowDefinition definition = requireDefinition(
                tenantContext, definitionId);
        if (versionNumber <= 0) {
            throw badRequest("Workflow version number must be greater than zero");
        }
        if (versionRepository.existsByWorkflowDefinitionIdAndVersionNumber(
                definition.getId(), versionNumber)) {
            throw conflict("Workflow version number already exists");
        }

        try {
            return versionRepository.save(ServiceWorkflowVersion.draft(
                    UUID.randomUUID(),
                    definition.getId(),
                    versionNumber,
                    tenantContext.userRefId(),
                    OffsetDateTime.now()
            ));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    @Transactional
    public ServiceWorkflowStage addStage(
            AuthenticatedTenantContext tenantContext,
            UUID versionId,
            String code,
            String displayName,
            int sequence
    ) {
        ServiceWorkflowVersion version = requireDraftVersion(
                tenantContext, versionId);
        if (stageRepository.existsByWorkflowVersionIdAndCode(version.getId(), code)) {
            throw conflict("Workflow stage code already exists");
        }

        try {
            return stageRepository.save(ServiceWorkflowStage.create(
                    UUID.randomUUID(), version.getId(), code, displayName, sequence));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    @Transactional
    public ServiceWorkflowStatus addStatus(
            AuthenticatedTenantContext tenantContext,
            UUID stageId,
            String code,
            String displayName,
            int sequence
    ) {
        ServiceWorkflowStage stage = requireStageInTenant(tenantContext, stageId);
        ServiceWorkflowVersion version = requireDraftVersion(
                tenantContext, stage.getWorkflowVersionId());
        if (statusRepository.existsByWorkflowStageIdAndCode(stage.getId(), code)) {
            throw conflict("Workflow status code already exists");
        }

        try {
            return statusRepository.save(ServiceWorkflowStatus.create(
                    UUID.randomUUID(), stage.getId(), code, displayName, sequence));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    @Transactional
    public ServiceWorkflowVersion publish(
            AuthenticatedTenantContext tenantContext,
            UUID versionId
    ) {
        ServiceWorkflowVersion version = requireDraftVersion(
                tenantContext, versionId);
        List<ServiceWorkflowStage> stages = stageRepository
                .findAllByWorkflowVersionIdOrderBySequence(version.getId());
        if (stages.isEmpty()) {
            throw badRequest("Workflow version must contain at least one stage");
        }
        for (ServiceWorkflowStage stage : stages) {
            if (!statusRepository.existsByWorkflowStageId(stage.getId())) {
                throw badRequest("Every workflow stage must contain at least one status");
            }
        }

        try {
            version.publish(tenantContext.userRefId(), OffsetDateTime.now());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw badRequest(exception.getMessage());
        }
        return versionRepository.save(version);
    }

    @Transactional
    public ServiceWorkflowVersion retire(
            AuthenticatedTenantContext tenantContext,
            UUID versionId
    ) {
        ServiceWorkflowVersion version = requireVersionInTenant(
                tenantContext, versionId);
        try {
            version.retire();
        } catch (IllegalStateException exception) {
            throw badRequest(exception.getMessage());
        }
        return versionRepository.save(version);
    }

    private ServiceWorkflowVersion requireDraftVersion(
            AuthenticatedTenantContext tenantContext,
            UUID versionId
    ) {
        ServiceWorkflowVersion version = requireVersionInTenant(tenantContext, versionId);
        if (version.getStatus() != ServiceWorkflowVersionStatus.DRAFT) {
            throw badRequest("Only draft workflow versions can be structurally changed");
        }
        return version;
    }

    private ServiceWorkflowVersion requireVersionInTenant(
            AuthenticatedTenantContext tenantContext,
            UUID versionId
    ) {
        requireTenantContext(tenantContext);
        ServiceWorkflowVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> notFound("Workflow version not found"));
        requireDefinition(tenantContext, version.getWorkflowDefinitionId());
        return version;
    }

    private ServiceWorkflowStage requireStageInTenant(
            AuthenticatedTenantContext tenantContext,
            UUID stageId
    ) {
        requireTenantContext(tenantContext);
        ServiceWorkflowStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> notFound("Workflow stage not found"));
        requireVersionInTenant(tenantContext, stage.getWorkflowVersionId());
        return stage;
    }

    private ServiceWorkflowDefinition requireDefinition(
            AuthenticatedTenantContext tenantContext,
            UUID definitionId
    ) {
        return definitionRepository.findByIdAndTenantId(
                definitionId, tenantContext.tenantId())
                .orElseThrow(() -> notFound("Workflow definition not found"));
    }

    private void validateScope(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId,
            UUID branchId
    ) {
        if (branchId != null && dealerId == null) {
            throw badRequest("dealerId is required when branchId is provided");
        }
        if (dealerId != null && dealerRepository.findByIdAndTenantId(
                dealerId, tenantContext.tenantId()).isEmpty()) {
            throw notFound("Dealer not found");
        }
        if (branchId != null) {
            Branch branch = branchRepository.findByIdAndTenantId(
                    branchId, tenantContext.tenantId())
                    .orElseThrow(() -> notFound("Branch not found"));
            if (!dealerId.equals(branch.getDealerId())) {
                throw badRequest("Branch does not belong to dealer");
            }
        }
    }

    private void requireTenantContext(AuthenticatedTenantContext tenantContext) {
        if (tenantContext == null
                || tenantContext.tenantId() == null
                || tenantContext.userRefId() == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Authenticated tenant context is required");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}
