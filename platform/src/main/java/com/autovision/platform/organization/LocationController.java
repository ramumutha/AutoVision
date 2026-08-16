package com.autovision.platform.organization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;
    private final TenantContextResolver tenantContextResolver;

    public LocationController(
            LocationService locationService,
            TenantContextResolver tenantContextResolver
    ) {
        this.locationService = locationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<LocationResponse> findAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return locationService.findAll(context);
    }

    @GetMapping("/{locationId}")
    public LocationResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID locationId
    ) {
        AuthenticatedTenantContext context =
                tenantContextResolver.resolve(jwt);

        return locationService.findById(
                context,
                locationId
        );
    }
}