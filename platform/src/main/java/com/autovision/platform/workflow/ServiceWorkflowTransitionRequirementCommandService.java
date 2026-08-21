package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceWorkflowTransitionRequirementCommandService {

    private final ServiceWorkflowDefinitionRepository definitionRepository;
    private final ServiceWorkflowVersionRepository versionRepository;
    private final ServiceWorkflowTransitionRepository transitionRepository;
    private final ServiceWorkflowTransitionRequirementRepository requirementRepository;
    private final AuthorizationService authorizationService;

    public ServiceWorkflowTransitionRequirementCommandService(
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

    @Transactional
    public ServiceWorkflowTransitionRequirement addTransitionRequirement(
            AuthenticatedTenantContext context,
            UUID workflowVersionId,
            UUID workflowTransitionId,
            ProcessRequirementKey requirementKey,
            ProcessRequirementMode mode
    ) {
        requireContext(context);
        Objects.requireNonNull(requirementKey, "Process requirement key is required");
        Objects.requireNonNull(mode, "Process requirement mode is required");

        ServiceWorkflowVersion version = requireVersionInTenant(
                context,
                workflowVersionId
        );
        authorizationService.requirePermission(new AuthorizationRequest(
                context,
                ServiceWorkflowPermissions.MANAGE,
                AuthorizationResourceType.SERVICE_WORKFLOW,
                version.getWorkflowDefinitionId()
        ));
        requireDraft(version);

        ServiceWorkflowTransition transition = transitionRepository
                .findByIdAndWorkflowVersionId(workflowTransitionId, version.getId())
                .orElseThrow(() -> notFound("Workflow transition not found"));
        if (requirementRepository.existsByWorkflowTransitionIdAndRequirementKey(
                transition.getId(),
                requirementKey.value()
        )) {
            throw conflict("Transition requirement key already exists");
        }

        try {
            return requirementRepository.save(
                    ServiceWorkflowTransitionRequirement.create(
                            UUID.randomUUID(),
                            version.getId(),
                            transition.getId(),
                            requirementKey,
                            mode,
                            OffsetDateTime.now(),
                            context.userRefId()
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    private ServiceWorkflowVersion requireVersionInTenant(
            AuthenticatedTenantContext context,
            UUID versionId
    ) {
        if (versionId == null) {
            throw badRequest("Workflow version ID is required");
        }
        ServiceWorkflowVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> notFound("Workflow version not found"));
        definitionRepository.findByIdAndTenantId(
                        version.getWorkflowDefinitionId(),
                        context.tenantId()
                )
                .orElseThrow(() -> notFound("Workflow version not found"));
        return version;
    }

    private void requireDraft(ServiceWorkflowVersion version) {
        if (version.getStatus() != ServiceWorkflowVersionStatus.DRAFT) {
            throw badRequest(
                    "Only draft workflow versions can be structurally changed"
            );
        }
    }

    private void requireContext(AuthenticatedTenantContext context) {
        if (context == null || context.tenantId() == null
                || context.userRefId() == null) {
            throw badRequest("Authenticated tenant context is required");
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