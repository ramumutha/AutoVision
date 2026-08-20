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
@RequestMapping("/api/v1/aftersales/service-orders/{orderId}/quotes")
public class ServiceQuoteController {

    private final TenantContextResolver tenantContextResolver;
    private final ServiceQuoteCommandService commandService;
    private final ServiceQuoteReadService readService;

    public ServiceQuoteController(
            TenantContextResolver tenantContextResolver,
            ServiceQuoteCommandService commandService,
            ServiceQuoteReadService readService
    ) {
        this.tenantContextResolver = tenantContextResolver;
        this.commandService = commandService;
        this.readService = readService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        public ServiceQuoteSummaryResponse create(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateServiceQuoteRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        ServiceQuote quote = commandService.create(
                context, orderId, request.quoteNumber(), request.currencyCode(),
                request.validUntil(), request.termsSnapshot(), request.disclaimerSnapshot()
        );
        return ServiceQuoteSummaryResponse.from(quote);
    }

    @GetMapping
    public List<ServiceQuoteSummaryResponse> list(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        return readService.listForServiceOrder(context, orderId).stream()
                .map(ServiceQuoteSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{quoteId}")
    public ServiceQuoteResponse get(
            @PathVariable UUID orderId,
            @PathVariable UUID quoteId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context = tenantContextResolver.resolve(jwt);
        ServiceQuote quote = readService.getForServiceOrder(context, orderId, quoteId);
        return ServiceQuoteResponse.from(
                quote,
                readService.linesFor(context, orderId, quoteId)
        );
    }
}