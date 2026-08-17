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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aftersales-cases")
public class AfterSalesCaseController {

    private final AfterSalesCaseService service;
    private final TenantContextResolver tenantContextResolver;

    public AfterSalesCaseController(
            AfterSalesCaseService service,
            TenantContextResolver tenantContextResolver
    ) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<AfterSalesCaseResponse> findAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return service.findAll(context)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{caseId}")
    public AfterSalesCaseResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.findById(context, caseId)
        );
    }

    @PostMapping
    public AfterSalesCaseResponse open(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody OpenAfterSalesCaseRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.open(
                        context,
                        request.caseNumber(),
                        request.dealerId(),
                        request.branchId(),
                        request.sourceChannel()
                )
        );
    }

    @PostMapping("/{caseId}/close")
    public AfterSalesCaseResponse close(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.close(context, caseId)
        );
    }

    private AfterSalesCaseResponse toResponse(
            AfterSalesCase afterSalesCase
    ) {
        return new AfterSalesCaseResponse(
                afterSalesCase.getId(),
                afterSalesCase.getTenantId(),
                afterSalesCase.getDealerId(),
                afterSalesCase.getBranchId(),
                afterSalesCase.getCaseNumber(),
                afterSalesCase.getLifecycleStatus(),
                afterSalesCase.getSourceChannel(),
                afterSalesCase.getOpenedAt(),
                afterSalesCase.getClosedAt(),
                afterSalesCase.getVersion(),
                afterSalesCase.getCreatedAt(),
                afterSalesCase.getUpdatedAt()
        );
    }
}