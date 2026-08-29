package com.autovision.platform.commercial;

import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.TenantContextResolver;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@Profile("local | test")
@RequestMapping("/internal/dev/commercial-verifications")
public class LocalCommercialVerificationController {
    private static final String OPERATE_PERMISSION = "COMMERCIAL_ENQUIRY.OPERATE";

    private final LocalCommercialVerificationDelivery delivery;
    private final TenantContextResolver tenantContextResolver;
    private final AuthorizationService authorizationService;

    public LocalCommercialVerificationController(
            LocalCommercialVerificationDelivery delivery,
            TenantContextResolver tenantContextResolver,
            AuthorizationService authorizationService
    ) {
        this.delivery = delivery;
        this.tenantContextResolver = tenantContextResolver;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/latest")
    public VerificationLink latest(@AuthenticationPrincipal Jwt jwt) {
        authorizationService.requireSystemPermission(
                tenantContextResolver.resolve(jwt),
                OPERATE_PERMISSION
        );
        String link = delivery.latestLink();
        if (link == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No local verification link available");
        return new VerificationLink(link);
    }

    public record VerificationLink(String link) { }
}