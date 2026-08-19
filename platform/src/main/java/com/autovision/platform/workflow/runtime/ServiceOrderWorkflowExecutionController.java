package com.autovision.platform.workflow.runtime;

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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aftersales/service-orders/{orderId}/workflow")
public class ServiceOrderWorkflowExecutionController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceOrderWorkflowExecutionCommandService commandService;
    private final ServiceOrderWorkflowExecutionReadService readService;

    public ServiceOrderWorkflowExecutionController(
            TenantContextResolver tenantContextResolver,
            ServiceOrderWorkflowExecutionCommandService commandService,
            ServiceOrderWorkflowExecutionReadService readService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
        this.readService = readService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOrderWorkflowExecutionResponse assign(
            @PathVariable UUID orderId,
            @Valid @RequestBody AssignServiceOrderWorkflowRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        return ServiceOrderWorkflowExecutionResponse.from(commandService.assign(
                context,
                orderId,
                request.workflowDefinitionId(),
                request.workflowVersionId(),
                request.initialStageId(),
                request.initialStatusId()
        ));
    }

    @GetMapping
    public ServiceOrderWorkflowExecutionResponse get(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        return ServiceOrderWorkflowExecutionResponse.from(
                readService.getForServiceOrder(context, orderId));
    }
}