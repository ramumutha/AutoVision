package com.autovision.platform.workflow;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceWorkflowConfigurationReadService {

    private final ServiceWorkflowDefinitionRepository definitionRepository;
    private final ServiceWorkflowVersionRepository versionRepository;
    private final ServiceWorkflowStageRepository stageRepository;
    private final ServiceWorkflowStatusRepository statusRepository;
    private final ServiceWorkflowTransitionRepository transitionRepository;
    private final AuthorizationService authorizationService;

    public ServiceWorkflowConfigurationReadService(
            ServiceWorkflowDefinitionRepository definitionRepository,
            ServiceWorkflowVersionRepository versionRepository,
            ServiceWorkflowStageRepository stageRepository,
            ServiceWorkflowStatusRepository statusRepository,
            ServiceWorkflowTransitionRepository transitionRepository,
            AuthorizationService authorizationService
    ) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.transitionRepository = transitionRepository;
        this.authorizationService = authorizationService;
    }

    public List<ServiceWorkflowDefinition> findDefinitions(
            AuthenticatedTenantContext context
    ) {
        requireReadPermission(context);
        return definitionRepository.findAllByTenantId(context.tenantId());
    }

    public ServiceWorkflowDefinition requireDefinition(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId
    ) {
        requireReadPermission(context, workflowDefinitionId);
        return definitionRepository.findByIdAndTenantId(
                        workflowDefinitionId, context.tenantId())
                .orElseThrow(() -> notFound("Workflow definition not found"));
    }

    public List<ServiceWorkflowVersion> findVersions(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId
    ) {
        requireDefinition(context, workflowDefinitionId);
        return versionRepository.findAllByWorkflowDefinitionIdOrderByVersionNumber(
                workflowDefinitionId);
    }

    public ServiceWorkflowVersion requireVersion(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId,
            UUID workflowVersionId
    ) {
        requireDefinition(context, workflowDefinitionId);
        ServiceWorkflowVersion version = versionRepository.findById(workflowVersionId)
                .orElseThrow(() -> notFound("Workflow version not found"));
        if (!workflowDefinitionId.equals(version.getWorkflowDefinitionId())) {
            throw notFound("Workflow version not found");
        }
        return version;
    }

    public List<ServiceWorkflowStage> findStages(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId,
            UUID workflowVersionId
    ) {
        requireVersion(context, workflowDefinitionId, workflowVersionId);
        return stageRepository.findAllByWorkflowVersionIdOrderBySequence(
                workflowVersionId);
    }

    public List<ServiceWorkflowStatus> findStatuses(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID workflowStageId
    ) {
        requireVersion(context, workflowDefinitionId, workflowVersionId);
        ServiceWorkflowStage stage = stageRepository.findById(workflowStageId)
                .orElseThrow(() -> notFound("Workflow stage not found"));
        if (!workflowVersionId.equals(stage.getWorkflowVersionId())) {
            throw notFound("Workflow stage not found");
        }
        return statusRepository.findAllByWorkflowStageIdOrderBySequence(
                workflowStageId);
    }

    public List<ServiceWorkflowTransition> findTransitions(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId,
            UUID workflowVersionId
    ) {
        requireVersion(context, workflowDefinitionId, workflowVersionId);
        return transitionRepository.findByWorkflowVersionIdOrderBySequenceAsc(
                workflowVersionId);
    }

    public ServiceWorkflowTransition requireTransition(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId,
            UUID workflowVersionId,
            UUID transitionId
    ) {
        requireVersion(context, workflowDefinitionId, workflowVersionId);
        return transitionRepository.findByIdAndWorkflowVersionId(
                        transitionId, workflowVersionId)
                .orElseThrow(() -> notFound("Workflow transition not found"));
    }

    private void requireReadPermission(AuthenticatedTenantContext context) {
        requireContext(context);
        authorizationService.requirePermission(
                context, ServiceWorkflowPermissions.READ);
    }

    private void requireReadPermission(
            AuthenticatedTenantContext context,
            UUID workflowDefinitionId
    ) {
        requireContext(context);
        authorizationService.requirePermission(new AuthorizationRequest(
                context,
                ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW,
                workflowDefinitionId
        ));
    }

    private void requireContext(AuthenticatedTenantContext context) {
        if (context == null || context.tenantId() == null
                || context.userRefId() == null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Authenticated tenant context is required");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}