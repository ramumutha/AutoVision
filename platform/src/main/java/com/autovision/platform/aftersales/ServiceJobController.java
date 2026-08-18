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

import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/api/v1/service-orders/{orderId}/jobs")
public class ServiceJobController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceJobCommandService commandService;
        private final ServiceOrderAccessService accessService;

    public ServiceJobController(
            TenantContextResolver tenantContextResolver,
                        ServiceJobCommandService commandService,
                        ServiceOrderAccessService accessService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
                this.accessService = accessService;
    }

    @PostMapping
    public ServiceJobMutationResponse create(
            @PathVariable UUID orderId,
            @RequestBody CreateServiceJobRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceJob job = commandService.create(
                tenantContext,
                orderId,
                request.jobNumber(),
                request.summary(),
                request.approvalStatus()
        );

        return ServiceJobMutationResponse.from(job);
    }

    @GetMapping
    public List<ServiceOrderAggregateResponse.ServiceJobResponse> findJobs(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return accessService.findJobs(
                        tenantContext,
                        orderId
                ).stream()
                .map(ServiceOrderAggregateResponse.ServiceJobResponse::from)
                .toList();
    }

    @GetMapping("/{jobId}")
    public ServiceOrderAggregateResponse.ServiceJobResponse requireJob(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return ServiceOrderAggregateResponse.ServiceJobResponse.from(
                accessService.requireJob(
                        tenantContext,
                        orderId,
                        jobId
                )
        );
    }

    @GetMapping("/{jobId}/lines")
    public List<ServiceOrderAggregateResponse.ServiceLineResponse>
            findLinesForJob(
                    @PathVariable UUID orderId,
                    @PathVariable UUID jobId,
                    @AuthenticationPrincipal Jwt jwt
            ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return accessService.findLinesForJob(
                        tenantContext,
                        orderId,
                        jobId
                ).stream()
                .map(ServiceOrderAggregateResponse.ServiceLineResponse::from)
                .toList();
    }

    @PostMapping("/{jobId}/mark-ready")
    public ServiceJobMutationResponse markReady(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jobId,
                jwt,
                commandService::markReady
        );
    }

    @PostMapping("/{jobId}/start")
    public ServiceJobMutationResponse start(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jobId,
                jwt,
                commandService::start
        );
    }

    @PostMapping("/{jobId}/complete-work")
    public ServiceJobMutationResponse completeWork(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jobId,
                jwt,
                commandService::completeWork
        );
    }

    @PostMapping("/{jobId}/close")
    public ServiceJobMutationResponse close(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jobId,
                jwt,
                commandService::close
        );
    }

    @PostMapping("/{jobId}/cancel")
    public ServiceJobMutationResponse cancel(
            @PathVariable UUID orderId,
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jobId,
                jwt,
                commandService::cancel
        );
    }

    private ServiceJobMutationResponse mutate(
            UUID orderId,
            UUID jobId,
            Jwt jwt,
            ServiceJobMutation mutation
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        try {
            ServiceJob job =
                    mutation.apply(
                            tenantContext,
                            orderId,
                            jobId
                    );

            return ServiceJobMutationResponse.from(
                    job
            );
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    CONFLICT,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface ServiceJobMutation {

        ServiceJob apply(
                AuthenticatedTenantContext tenantContext,
                UUID orderId,
                UUID jobId
        );
    }
}