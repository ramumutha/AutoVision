package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/service-orders/{orderId}/lines")
public class ServiceLineController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceLineCommandService commandService;
        private final ServiceOrderAccessService accessService;

    public ServiceLineController(
            TenantContextResolver tenantContextResolver,
                        ServiceLineCommandService commandService,
                        ServiceOrderAccessService accessService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
                this.accessService = accessService;
    }

    @PostMapping
    public ServiceLineMutationResponse create(
            @PathVariable UUID orderId,
            @RequestBody CreateServiceLineRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        try {
            ServiceLine line =
                    commandService.create(
                            tenantContext,
                            orderId,
                            request.serviceJobId(),
                            request.lineNumber(),
                            request.lineType(),
                            request.description(),
                            request.quantity(),
                            request.unitOfMeasure()
                    );

            return ServiceLineMutationResponse.from(line);
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception);
        }
    }

    @GetMapping
    public List<ServiceOrderAggregateResponse.ServiceLineResponse> findLines(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return accessService.findLines(
                        tenantContext,
                        orderId
                ).stream()
                .map(ServiceOrderAggregateResponse.ServiceLineResponse::from)
                .toList();
    }

    @GetMapping("/{lineId}")
    public ServiceOrderAggregateResponse.ServiceLineResponse requireLine(
            @PathVariable UUID orderId,
            @PathVariable UUID lineId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return ServiceOrderAggregateResponse.ServiceLineResponse.from(
                accessService.requireLine(
                        tenantContext,
                        orderId,
                        lineId
                )
        );
    }

    @PostMapping("/{lineId}/update-details")
    public ServiceLineMutationResponse updateDetails(
            @PathVariable UUID orderId,
            @PathVariable UUID lineId,
            @RequestBody UpdateServiceLineDetailsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        try {
            ServiceLine line =
                    commandService.updateDetails(
                            tenantContext,
                            orderId,
                            lineId,
                            request.lineType(),
                            request.description(),
                            request.quantity(),
                            request.unitOfMeasure()
                    );

            return ServiceLineMutationResponse.from(line);
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception);
        }
    }

    @PostMapping("/{lineId}/assign-job/{jobId}")
    public ServiceLineMutationResponse assignToJob(
            @PathVariable UUID orderId,
            @PathVariable UUID lineId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceLine line =
                commandService.assignToJob(
                        tenantContext,
                        orderId,
                        lineId,
                        jobId
                );

        return ServiceLineMutationResponse.from(line);
    }

    @PostMapping("/{lineId}/unassign-job")
    public ServiceLineMutationResponse unassignFromJob(
            @PathVariable UUID orderId,
            @PathVariable UUID lineId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceLine line =
                commandService.unassignFromJob(
                        tenantContext,
                        orderId,
                        lineId
                );

        return ServiceLineMutationResponse.from(line);
    }

    private ResponseStatusException badRequest(
            IllegalArgumentException exception
    ) {
        return new ResponseStatusException(
                BAD_REQUEST,
                exception.getMessage(),
                exception
        );
    }
}