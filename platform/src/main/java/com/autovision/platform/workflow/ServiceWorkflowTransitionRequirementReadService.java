package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementDefinition;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceWorkflowTransitionRequirementReadService {

    private final ServiceWorkflowDefinitionRepository definitionRepository;
    private final ServiceWorkflowVersionRepository versionRepository;
    private final ServiceWorkflowTransitionRepository transitionRepository;
    private final ServiceWorkflowTransitionRequirementRepository requirementRepository;
    private final AuthorizationService authorizationService;

    public ServiceWorkflowTransitionRequirementReadService(
            ServiceWorkflowDefinitionRepository definitionRepository,
            ServiceWorkflowVersionRepository versionRepository,
            ServiceWorkflowTransitionRepository transitionRepository,
            ServiceWorkflowTransitionRequirementRepository requirementRepository,
            AuthorizationService authorizationService
    ) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.transitionRepository = transitionRepository;
        this.requirementRepository = requirementRepository;
        this.authorizationService = authorizationService;
    }

    public List<ProcessRequirementDefinition> findDefinitions(
            AuthenticatedTenantContext context,
            UUID workflowVersionId,
            UUID workflowTransitionId
    ) {
        if (context == null || context.tenantId() == null
                || context.userRefId() == null) {
            throw badRequest("Authenticated tenant context is required");
        }
        ServiceWorkflowVersion version = versionRepository.findById(workflowVersionId)
                .orElseThrow(() -> notFound("Workflow version not found"));
        definitionRepository.findByIdAndTenantId(
                        version.getWorkflowDefinitionId(),
                        context.tenantId()
                )
                .orElseThrow(() -> notFound("Workflow version not found"));
        authorizationService.requirePermission(new AuthorizationRequest(
                context,
                ServiceWorkflowPermissions.READ,
                AuthorizationResourceType.SERVICE_WORKFLOW,
                version.getWorkflowDefinitionId()
        ));
        transitionRepository.findByIdAndWorkflowVersionId(
                        workflowTransitionId,
                        version.getId()
                )
                .orElseThrow(() -> notFound("Workflow transition not found"));

        return requirementRepository
                .findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(
                        version.getId(),
                        workflowTransitionId
                )
                .stream()
                .map(binding -> new ProcessRequirementDefinition(
                        binding.getRequirementKey(),
                        binding.getRequirementMode()
                ))
                .toList();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}