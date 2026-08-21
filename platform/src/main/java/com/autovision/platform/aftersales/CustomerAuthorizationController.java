package com.autovision.platform.aftersales;

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
@RequestMapping(
        "/api/v1/aftersales-cases/{caseId}/customer-authorizations"
)
public class CustomerAuthorizationController {

    private final CustomerAuthorizationService service;
    private final TenantContextResolver tenantContextResolver;

    public CustomerAuthorizationController(
            CustomerAuthorizationService service,
            TenantContextResolver tenantContextResolver
    ) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<CustomerAuthorizationResponse> findAllForCase(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return service.findAllForCase(
                context,
                caseId
        )
        .stream()
        .map(CustomerAuthorizationController::toResponse)
        .toList();
    }

    @GetMapping("/{authorizationId}")
    public CustomerAuthorizationResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID authorizationId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.findById(
                        context,
                        caseId,
                        authorizationId
                )
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAuthorizationResponse request(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateCustomerAuthorizationRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.request(
                        context,
                        caseId,
                        request.serviceQuoteId(),
                        request.authorizationNumber(),
                        request.customerReference(),
                        request.customerDisplayNameSnapshot(),
                        request.authorizationSummary(),
                        request.authorizationScopeSnapshot(),
                        request.commercialSnapshot(),
                        request.termsSnapshot(),
                        request.disclaimerSnapshot()
                )
        );
    }

    @PostMapping("/{authorizationId}/authorize")
    public CustomerAuthorizationResponse authorize(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID authorizationId,
            @Valid @RequestBody CustomerAuthorizationDecisionRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.authorize(
                        context,
                        caseId,
                        authorizationId,
                        request.decisionChannel(),
                        request.decisionReference()
                )
        );
    }

    @PostMapping("/{authorizationId}/decline")
    public CustomerAuthorizationResponse decline(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID authorizationId,
            @Valid @RequestBody CustomerAuthorizationDecisionRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.decline(
                        context,
                        caseId,
                        authorizationId,
                        request.decisionChannel(),
                        request.decisionReference()
                )
        );
    }

    @PostMapping("/{authorizationId}/defer")
    public CustomerAuthorizationResponse defer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID authorizationId,
            @Valid @RequestBody CustomerAuthorizationDecisionRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.defer(
                        context,
                        caseId,
                        authorizationId,
                        request.decisionChannel(),
                        request.decisionReference()
                )
        );
    }

    @PostMapping("/{authorizationId}/cancel")
    public CustomerAuthorizationResponse cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID authorizationId,
            @Valid @RequestBody CustomerAuthorizationDecisionRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return toResponse(
                service.cancel(
                        context,
                        caseId,
                        authorizationId,
                        request.decisionChannel(),
                        request.decisionReference()
                )
        );
    }

        static CustomerAuthorizationResponse toResponse(
            CustomerAuthorization authorization
    ) {
        return new CustomerAuthorizationResponse(
                authorization.getId(),
                authorization.getTenantId(),
                authorization.getDealerId(),
                authorization.getBranchId(),
                authorization.getAftersalesCaseId(),
                authorization.getServiceQuoteId(),
                authorization.getAuthorizationNumber(),
                authorization.getAuthorizationStatus(),
                authorization.getCustomerReference(),
                authorization.getCustomerDisplayNameSnapshot(),
                authorization.getAuthorizationSummary(),
                authorization.getAuthorizationScopeSnapshot(),
                authorization.getCommercialSnapshot(),
                authorization.getTermsSnapshot(),
                authorization.getDisclaimerSnapshot(),
                authorization.getRequestedAt(),
                authorization.getDecidedAt(),
                authorization.getDecisionChannel(),
                authorization.getDecisionReference(),
                authorization.getVersion(),
                authorization.getCreatedByPrincipalId(),
                authorization.getUpdatedByPrincipalId(),
                authorization.getCreatedAt(),
                authorization.getUpdatedAt()
        );
    }
}
