package com.autovision.platform.serviceprofit;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/service-profit/opportunities")
public class ServiceProfitOpportunityController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceProfitOpportunityCommandService commandService;
    private final ServiceProfitOpportunityAccessService accessService;
    private final ServiceProfitOpportunityQueryService queryService;
        private final ServiceProfitOpportunityContextService contextService;

    public ServiceProfitOpportunityController(
            TenantContextResolver tenantContextResolver,
            ServiceProfitOpportunityCommandService commandService,
            ServiceProfitOpportunityAccessService accessService,
            ServiceProfitOpportunityQueryService queryService,
            ServiceProfitOpportunityContextService contextService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
        this.accessService = accessService;
        this.queryService = queryService;
        this.contextService = contextService;
    }

    @PostMapping
    public ServiceProfitOpportunityResponse create(
            @RequestBody CreateServiceProfitOpportunityRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        return ServiceProfitOpportunityResponse.from(
                commandService.create(
                        tenantContext,
                        request
                )
        );
    }

    @GetMapping
    public ServiceProfitOpportunityPage findPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false)
            ServiceProfitOpportunityStatus status,
            @RequestParam(required = false)
            ServiceProfitPriority priority,
            @RequestParam(required = false)
            ServiceProfitOpportunityType opportunityType,
            @RequestParam(required = false)
            ServiceProfitEvidenceClass evidenceClass,
            @RequestParam(required = false)
            ServiceProfitEvidenceStrength evidenceStrength,
            @RequestParam(required = false)
            ServiceProfitActionability actionability,
            @RequestParam(required = false)
            UUID dealerId,
            @RequestParam(required = false)
            UUID branchId,
            @RequestParam(required = false)
            UUID locationId,
            @RequestParam(
                    defaultValue = "DETECTED_DESC"
            )
            ServiceProfitOpportunitySort sort
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        try {
            ServiceProfitOpportunityQuery query =
                    new ServiceProfitOpportunityQuery(
                            status,
                            priority,
                            opportunityType,
                            evidenceClass,
                            evidenceStrength,
                            actionability,
                            dealerId,
                            branchId,
                            locationId,
                            page,
                            size,
                            sort
                    );

            return queryService.findPage(
                    tenantContext,
                    query
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @GetMapping("/summary")
    public ServiceProfitOpportunitySummary findSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            ServiceProfitOpportunityStatus status,
            @RequestParam(required = false)
            ServiceProfitPriority priority,
            @RequestParam(required = false)
            ServiceProfitOpportunityType opportunityType,
            @RequestParam(required = false)
            ServiceProfitEvidenceClass evidenceClass,
            @RequestParam(required = false)
            ServiceProfitEvidenceStrength evidenceStrength,
            @RequestParam(required = false)
            ServiceProfitActionability actionability,
            @RequestParam(required = false)
            UUID dealerId,
            @RequestParam(required = false)
            UUID branchId,
            @RequestParam(required = false)
            UUID locationId
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceProfitOpportunityQuery query =
                new ServiceProfitOpportunityQuery(
                        status,
                        priority,
                        opportunityType,
                        evidenceClass,
                        evidenceStrength,
                        actionability,
                        dealerId,
                        branchId,
                        locationId,
                        0,
                        1,
                        ServiceProfitOpportunitySort.DETECTED_DESC
                );

        return queryService.findSummary(
                tenantContext,
                query
        );
    }

    @GetMapping("/{opportunityId}")
    public ServiceProfitOpportunityResponse get(
            @PathVariable UUID opportunityId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext tenantContext =
                tenantContextResolver.resolve(jwt);

        ServiceProfitOpportunity opportunity =
                accessService.requireOpportunity(
                    tenantContext,
                    opportunityId
                );

        return ServiceProfitOpportunityResponse.from(
                opportunity,
                contextService.findFor(opportunity).orElse(null)
        );
    }
}
