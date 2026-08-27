package com.autovision.platform.serviceprofit;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/service-profit/follow-ups")
public class ServiceProfitWorkQueueController {
    private final TenantContextResolver tenantContextResolver;
    private final ServiceProfitWorkQueueQueryService queryService;

    public ServiceProfitWorkQueueController(TenantContextResolver tenantContextResolver,
                                            ServiceProfitWorkQueueQueryService queryService) {
        this.tenantContextResolver = tenantContextResolver;
        this.queryService = queryService;
    }

    @GetMapping
    public ServiceProfitWorkQueuePageResponse findPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ServiceProfitFollowUpHandlingStatus handlingStatus,
            @RequestParam(required = false) ServiceProfitWorkQueueOwnership ownership,
            @RequestParam(required = false) ServiceProfitWorkQueueDueState dueState,
            @RequestParam(required = false) ServiceProfitFollowUpDisposition disposition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        AuthenticatedTenantContext tenantContext = tenantContextResolver.resolve(jwt);
        try {
            ServiceProfitWorkQueueQuery query = new ServiceProfitWorkQueueQuery(
                    handlingStatus, ownership, dueState, disposition, page, size);
            return ServiceProfitWorkQueuePageResponse.from(queryService.findPage(tenantContext, query));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}