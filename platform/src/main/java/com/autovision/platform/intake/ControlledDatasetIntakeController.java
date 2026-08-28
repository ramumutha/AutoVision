package com.autovision.platform.intake;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/controlled-data-intake")
public class ControlledDatasetIntakeController {

    private final TenantContextResolver tenantContextResolver;
    private final ControlledDatasetIntakeCommandService commandService;

    public ControlledDatasetIntakeController(
            TenantContextResolver tenantContextResolver,
            ControlledDatasetIntakeCommandService commandService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
    }

    @PostMapping
    public ControlledDatasetIntakeResponse process(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ControlledDatasetIntakeRequest request
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        return ControlledDatasetIntakeResponse.from(
                commandService.process(context, request.packageReference()));
    }
}