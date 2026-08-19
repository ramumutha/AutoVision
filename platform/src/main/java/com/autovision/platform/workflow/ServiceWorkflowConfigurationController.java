package com.autovision.platform.workflow;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/service-workflows")
public class ServiceWorkflowConfigurationController {

    private final ServiceWorkflowConfigurationReadService readService;
    private final ServiceWorkflowConfigurationCommandService commandService;
    private final TenantContextResolver tenantContextResolver;

    public ServiceWorkflowConfigurationController(
            ServiceWorkflowConfigurationReadService readService,
            ServiceWorkflowConfigurationCommandService commandService,
            TenantContextResolver tenantContextResolver
    ) {
        this.readService = readService;
        this.commandService = commandService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<ServiceWorkflowDefinitionResponse> findDefinitions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return readService.findDefinitions(context(jwt)).stream()
                .map(ServiceWorkflowDefinitionResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceWorkflowDefinitionResponse createDefinition(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateServiceWorkflowDefinitionRequest request
    ) {
        AuthenticatedTenantContext context = context(jwt);
        return ServiceWorkflowDefinitionResponse.from(commandService.createDefinition(
                context, request.dealerId(), request.branchId(),
                request.code(), request.displayName()));
    }

    @GetMapping("/{workflowDefinitionId}")
    public ServiceWorkflowDefinitionResponse requireDefinition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId
    ) {
        return ServiceWorkflowDefinitionResponse.from(readService.requireDefinition(
                context(jwt), workflowDefinitionId));
    }

    @GetMapping("/{workflowDefinitionId}/versions")
    public List<ServiceWorkflowVersionResponse> findVersions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId
    ) {
        return readService.findVersions(context(jwt), workflowDefinitionId).stream()
                .map(ServiceWorkflowVersionResponse::from).toList();
    }

    @PostMapping("/{workflowDefinitionId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceWorkflowVersionResponse createDraftVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @Valid @RequestBody CreateServiceWorkflowVersionRequest request
    ) {
        return ServiceWorkflowVersionResponse.from(commandService.createDraftVersion(
                context(jwt), workflowDefinitionId, request.versionNumber()));
    }

    @GetMapping("/{workflowDefinitionId}/versions/{workflowVersionId}")
    public ServiceWorkflowVersionResponse requireVersion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId
    ) {
        return ServiceWorkflowVersionResponse.from(readService.requireVersion(
                context(jwt), workflowDefinitionId, workflowVersionId));
    }

    @GetMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/stages")
    public List<ServiceWorkflowStageResponse> findStages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId
    ) {
        return readService.findStages(context(jwt), workflowDefinitionId, workflowVersionId)
                .stream().map(ServiceWorkflowStageResponse::from).toList();
    }

    @PostMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceWorkflowStageResponse addStage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId,
            @Valid @RequestBody CreateServiceWorkflowStageRequest request
    ) {
        return ServiceWorkflowStageResponse.from(commandService.addStage(
                context(jwt), workflowVersionId, request.code(),
                request.displayName(), request.sequence()));
    }

    @GetMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/stages/{workflowStageId}/statuses")
    public List<ServiceWorkflowStatusResponse> findStatuses(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId,
            @PathVariable UUID workflowStageId
    ) {
        return readService.findStatuses(context(jwt), workflowDefinitionId,
                workflowVersionId, workflowStageId).stream()
                .map(ServiceWorkflowStatusResponse::from).toList();
    }

    @PostMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/stages/{workflowStageId}/statuses")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceWorkflowStatusResponse addStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId,
            @PathVariable UUID workflowStageId,
            @Valid @RequestBody CreateServiceWorkflowStatusRequest request
    ) {
        return ServiceWorkflowStatusResponse.from(commandService.addStatus(
                context(jwt), workflowStageId, request.code(),
                request.displayName(), request.sequence()));
    }

    @PostMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/publish")
    public ServiceWorkflowVersionResponse publish(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId
    ) {
        return ServiceWorkflowVersionResponse.from(commandService.publish(
                context(jwt), workflowVersionId));
    }

    @PostMapping("/{workflowDefinitionId}/versions/{workflowVersionId}/retire")
    public ServiceWorkflowVersionResponse retire(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workflowDefinitionId,
            @PathVariable UUID workflowVersionId
    ) {
        return ServiceWorkflowVersionResponse.from(commandService.retire(
                context(jwt), workflowVersionId));
    }

    private AuthenticatedTenantContext context(Jwt jwt) {
        return tenantContextResolver.resolve(jwt);
    }
}