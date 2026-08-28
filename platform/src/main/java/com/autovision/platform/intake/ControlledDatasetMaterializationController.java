package com.autovision.platform.intake;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/controlled-data-intake")
public class ControlledDatasetMaterializationController {

    private final TenantContextResolver tenantContextResolver;
    private final ControlledDatasetMaterializationCommandService commandService;

    public ControlledDatasetMaterializationController(
            TenantContextResolver tenantContextResolver,
            ControlledDatasetMaterializationCommandService commandService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
    }

    @PostMapping("/{datasetProcessingId}/materialize")
    public ControlledDatasetMaterializationResult materialize(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID datasetProcessingId
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        return commandService.materialize(context, datasetProcessingId);
    }

    @PostMapping("/{datasetProcessingId}/approve")
    public ControlledDatasetProcessingResult approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID datasetProcessingId
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        DatasetProcessing approved = commandService.approve(context, datasetProcessingId);
        return new ControlledDatasetProcessingResult(
                approved.getId(), approved.getDatasetId(), approved.getDatasetVersion(), approved.getStatus(),
                0, 0, 0, 0, 0, 0, 0, false);
    }
}