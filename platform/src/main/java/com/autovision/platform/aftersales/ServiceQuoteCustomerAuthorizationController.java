package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/aftersales-cases/{caseId}/service-quotes/{quoteId}"
)
public class ServiceQuoteCustomerAuthorizationController {

    private final CustomerAuthorizationService service;
    private final TenantContextResolver tenantContextResolver;

    public ServiceQuoteCustomerAuthorizationController(
            CustomerAuthorizationService service,
            TenantContextResolver tenantContextResolver
    ) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/customer-authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAuthorizationResponse request(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID caseId,
            @PathVariable UUID quoteId,
            @Valid @RequestBody RequestQuoteCustomerAuthorizationRequest request
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return CustomerAuthorizationController.toResponse(
                service.requestFromServiceQuote(
                        context,
                        caseId,
                        quoteId,
                        request.authorizationNumber(),
                        request.customerReference(),
                        request.customerDisplayNameSnapshot(),
                        request.authorizationSummary()
                )
        );
    }

        @GetMapping("/customer-authorizations")
        public List<CustomerAuthorizationResponse> findAll(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable UUID caseId,
                        @PathVariable UUID quoteId
        ) {
                AuthenticatedTenantContext context =
                                tenantContextResolver.resolve(jwt);

                return service.findAllForServiceQuote(
                                context,
                                caseId,
                                quoteId
                )
                .stream()
                .map(CustomerAuthorizationController::toResponse)
                .toList();
        }
}