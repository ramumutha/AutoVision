package com.autovision.platform.organization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final AuthorizationService authorizationService;

    public LocationService(
            LocationRepository locationRepository,
            AuthorizationService authorizationService
    ) {
        this.locationRepository = locationRepository;
        this.authorizationService = authorizationService;
    }

    public List<LocationResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        authorizationService.requirePermission(
                tenantContext,
                OrganizationPermissions.LOCATION_READ
        );

        return locationRepository
                .findAllByTenantId(tenantContext.tenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LocationResponse findById(
            AuthenticatedTenantContext tenantContext,
            UUID locationId
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        OrganizationPermissions.LOCATION_READ,
                        AuthorizationResourceType.LOCATION,
                        locationId
                )
        );

        Location location = locationRepository
                .findByIdAndTenantId(
                        locationId,
                        tenantContext.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Location not found"
                ));

        return toResponse(location);
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getAddressLine1(),
                location.getAddressLine2(),
                location.getCity(),
                location.getStateProvince(),
                location.getPostalCode(),
                location.getCountryCode(),
                location.getTimezone(),
                location.getLatitude(),
                location.getLongitude(),
                location.getStatus(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}