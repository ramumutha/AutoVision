package com.autovision.platform.workflow;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
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
    private final ServiceWorkflowTransitionRepository transitionRepository;
    private final DealerRepository dealerRepository;
    private final BranchRepository branchRepository;
    private final AuthorizationService authorizationService;

    public ServiceWorkflowConfigurationCommandService(
            ServiceWorkflowDefinitionRepository definitionRepository,
            ServiceWorkflowVersionRepository versionRepository,
            ServiceWorkflowStageRepository stageRepository,
            ServiceWorkflowStatusRepository statusRepository,
            ServiceWorkflowTransitionRepository transitionRepository,
            DealerRepository dealerRepository,
            BranchRepository branchRepository,
            AuthorizationService authorizationService
    ) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.dealerRepository = dealerRepository;
        this.branchRepository = branchRepository;
        this.authorizationService = authorizationService;
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
        validateRequestedScope(dealerId, branchId);
        authorizeCreateDefinition(tenantContext, dealerId, branchId);
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
        authorizeWorkflow(tenantContext, definition.getId(),
            ServiceWorkflowPermissions.MANAGE);
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
        ServiceWorkflowVersion version = requireVersionInTenant(
            tenantContext, versionId);
        authorizeWorkflow(tenantContext, version.getWorkflowDefinitionId(),
            ServiceWorkflowPermissions.MANAGE);
        requireDraft(version);
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
        ServiceWorkflowVersion version = requireVersionInTenant(
                tenantContext, stage.getWorkflowVersionId());
        authorizeWorkflow(tenantContext, version.getWorkflowDefinitionId(),
            ServiceWorkflowPermissions.MANAGE);
        requireDraft(version);
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
    public ServiceWorkflowTransition addTransition(
            AuthenticatedTenantContext tenantContext,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID fromStageId,
            UUID fromStatusId,
            UUID toStageId,
            UUID toStatusId,
            String code,
            String displayName,
            int sequence
    ) {
        requireTenantContext(tenantContext);
        ServiceWorkflowDefinition definition = requireDefinition(
                tenantContext, workflowDefinitionId);
        authorizeWorkflow(tenantContext, definition.getId(),
            ServiceWorkflowPermissions.MANAGE);

        ServiceWorkflowVersion version = requireVersionInDefinition(
                workflowVersionId, definition.getId());
        requireDraft(version);

        ServiceWorkflowStage fromStage = requireStageInVersion(
                fromStageId, version.getId());
        requireActive(fromStage.isActive(), "From workflow stage is not active");
        ServiceWorkflowStatus fromStatus = requireStatusInStage(
                fromStatusId, fromStage.getId());
        requireActive(fromStatus.isActive(), "From workflow status is not active");

        ServiceWorkflowStage toStage = requireStageInVersion(
                toStageId, version.getId());
        requireActive(toStage.isActive(), "To workflow stage is not active");
        ServiceWorkflowStatus toStatus = requireStatusInStage(
                toStatusId, toStage.getId());
        requireActive(toStatus.isActive(), "To workflow status is not active");

        if (transitionRepository.existsByWorkflowVersionIdAndCode(
                version.getId(), code)) {
            throw conflict("Workflow transition code already exists");
        }
        if (transitionRepository
                .existsByWorkflowVersionIdAndFromStageIdAndFromStatusIdAndToStageIdAndToStatusId(
                        version.getId(), fromStage.getId(), fromStatus.getId(),
                        toStage.getId(), toStatus.getId())) {
            throw conflict("Workflow transition edge already exists");
        }

        try {
            return transitionRepository.save(ServiceWorkflowTransition.create(
                    UUID.randomUUID(),
                    version.getId(),
                    fromStage.getId(),
                    fromStatus.getId(),
                    toStage.getId(),
                    toStatus.getId(),
                    code,
                    displayName,
                    sequence,
                    tenantContext.userRefId(),
                    OffsetDateTime.now()
            ));
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    @Transactional
    public ServiceWorkflowVersion publish(
            AuthenticatedTenantContext tenantContext,
            UUID versionId
    ) {
        ServiceWorkflowVersion version = requireVersionInTenant(
            tenantContext, versionId);
        authorizeWorkflow(tenantContext, version.getWorkflowDefinitionId(),
            ServiceWorkflowPermissions.PUBLISH);
        requireDraft(version);
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
        authorizeWorkflow(tenantContext, version.getWorkflowDefinitionId(),
            ServiceWorkflowPermissions.PUBLISH);
        try {
            version.retire();
        } catch (IllegalStateException exception) {
            throw badRequest(exception.getMessage());
        }
        return versionRepository.save(version);
    }

    private void requireDraft(ServiceWorkflowVersion version) {
        if (version.getStatus() != ServiceWorkflowVersionStatus.DRAFT) {
            throw badRequest("Only draft workflow versions can be structurally changed");
        }
    }

    private void requireActive(boolean active, String message) {
        if (!active) {
            throw badRequest(message);
        }
    }

    private ServiceWorkflowVersion requireVersionInDefinition(
            UUID workflowVersionId,
            UUID workflowDefinitionId
    ) {
        ServiceWorkflowVersion version = versionRepository.findById(workflowVersionId)
                .orElseThrow(() -> notFound("Workflow version not found"));
        if (!version.getWorkflowDefinitionId().equals(workflowDefinitionId)) {
            throw notFound("Workflow version not found");
        }
        return version;
    }

    private ServiceWorkflowStage requireStageInVersion(
            UUID stageId,
            UUID workflowVersionId
    ) {
        return stageRepository.findByIdAndWorkflowVersionId(stageId, workflowVersionId)
                .orElseThrow(() -> notFound("Workflow stage not found"));
    }

    private ServiceWorkflowStatus requireStatusInStage(
            UUID statusId,
            UUID workflowStageId
    ) {
        return statusRepository.findByIdAndWorkflowStageId(statusId, workflowStageId)
                .orElseThrow(() -> notFound("Workflow status not found"));
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

        private void validateRequestedScope(UUID dealerId, UUID branchId) {
        if (branchId != null && dealerId == null) {
            throw badRequest("dealerId is required when branchId is provided");
        }
        }

        private void authorizeCreateDefinition(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId,
            UUID branchId
        ) {
        AuthorizationResourceType resourceType = branchId != null
            ? AuthorizationResourceType.BRANCH
            : dealerId != null
                ? AuthorizationResourceType.DEALER
                : AuthorizationResourceType.TENANT;
        UUID resourceId = branchId != null
            ? branchId
            : dealerId != null
                ? dealerId
                : tenantContext.tenantId();
        authorizationService.requirePermission(new AuthorizationRequest(
            tenantContext,
            ServiceWorkflowPermissions.MANAGE,
            resourceType,
            resourceId
        ));
        }

        private void authorizeWorkflow(
            AuthenticatedTenantContext tenantContext,
            UUID definitionId,
            String permission
        ) {
        authorizationService.requirePermission(new AuthorizationRequest(
            tenantContext,
            permission,
            AuthorizationResourceType.SERVICE_WORKFLOW,
            definitionId
        ));
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
