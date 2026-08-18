package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/api/v1/service-orders")
public class ServiceOrderController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceOrderAccessService accessService;
    private final ServiceOrderCommandService commandService;

    public ServiceOrderController(
            TenantContextResolver tenantContextResolver,
            ServiceOrderAccessService accessService,
            ServiceOrderCommandService commandService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.accessService = accessService;
        this.commandService = commandService;
    }

    @GetMapping("/{orderId}/aggregate")
    public ServiceOrderAggregateResponse aggregate(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceOrderAggregateView aggregate =
                accessService.requireAggregate(
                        tenantContext,
                        orderId
                );

        return ServiceOrderAggregateResponse.from(
                aggregate
        );
    }

    @PostMapping("/{orderId}/start")
    public ServiceOrderMutationResponse start(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jwt,
                commandService::start
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ServiceOrderMutationResponse cancel(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jwt,
                commandService::cancel
        );
    }

    @PostMapping("/{orderId}/complete-work")
    public ServiceOrderMutationResponse completeWork(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jwt,
                commandService::completeWork
        );
    }

    @PostMapping("/{orderId}/close")
    public ServiceOrderMutationResponse close(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mutate(
                orderId,
                jwt,
                commandService::close
        );
    }

    private ServiceOrderMutationResponse mutate(
            UUID orderId,
            Jwt jwt,
            ServiceOrderMutation mutation
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        try {
            ServiceOrder order =
                    mutation.apply(
                            tenantContext,
                            orderId
                    );

            return ServiceOrderMutationResponse.from(
                    order
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
    private interface ServiceOrderMutation {

        ServiceOrder apply(
                AuthenticatedTenantContext tenantContext,
                UUID orderId
        );
    }
}