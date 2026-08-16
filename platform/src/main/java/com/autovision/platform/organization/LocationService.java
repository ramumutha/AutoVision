package com.autovision.platform.organization;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationScopeRepository;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final AuthorizationService authorizationService;
    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationScopeRepository authorizationScopeRepository;

    public LocationService(
            LocationRepository locationRepository,
            AuthorizationService authorizationService,
            AuthorizationRepository authorizationRepository,
            AuthorizationScopeRepository authorizationScopeRepository
    ) {
        this.locationRepository = locationRepository;
        this.authorizationService = authorizationService;
        this.authorizationRepository = authorizationRepository;
        this.authorizationScopeRepository = authorizationScopeRepository;
    }

    /**
     * Returns only locations contained by the caller's active permission-
     * bearing grants (S4.7.7.3D): TENANT -> all tenant locations, DEALER_GROUP
     * -> locations of member dealers, DEALER -> locations of that dealer,
     * BRANCH -> locations under that branch, LOCATION -> that exact location.
     * TENANT_GROUP grants no Location collection access. Results from
     * multiple grants are unioned and deduplicated; tenant isolation is
     * enforced by every repository call.
     */
    public List<LocationResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        List<AuthorizationGrant> grants =
                authorizationRepository.findActivePermissionGrants(
                        tenantContext.userRefId(),
                        tenantContext.tenantId(),
                        OrganizationPermissions.LOCATION_READ
                );

        if (grants.isEmpty()) {
            throw new AccessDeniedException("Access is denied");
        }

        Set<Location> locations = new LinkedHashSet<>();

        for (AuthorizationGrant grant : grants) {
            switch (grant.scopeType()) {
                case TENANT -> {
                    if (grant.scopeId().equals(tenantContext.tenantId())) {
                        locations.addAll(
                                locationRepository.findAllByTenantId(
                                        tenantContext.tenantId()
                                )
                        );
                    }
                }
                case DEALER_GROUP -> {
                    List<UUID> memberDealerIds =
                            authorizationScopeRepository.findDealerIdsInGroup(
                                    grant.scopeId(),
                                    tenantContext.tenantId()
                            );

                    if (!memberDealerIds.isEmpty()) {
                        locations.addAll(
                                locationRepository.findAllByTenantIdAndDealerIdIn(
                                        tenantContext.tenantId(),
                                        memberDealerIds
                                )
                        );
                    }
                }
                case DEALER -> locations.addAll(
                        locationRepository.findAllByTenantIdAndDealerIdIn(
                                tenantContext.tenantId(),
                                List.of(grant.scopeId())
                        )
                );
                case BRANCH -> locations.addAll(
                        locationRepository.findAllByTenantIdAndBranchIdIn(
                                tenantContext.tenantId(),
                                List.of(grant.scopeId())
                        )
                );
                case LOCATION -> locationRepository
                        .findByIdAndTenantId(
                                grant.scopeId(),
                                tenantContext.tenantId()
                        )
                        .ifPresent(locations::add);
                case TENANT_GROUP -> {
                    // This scope grants no Location collection access.
                }
            }
        }

        return locations.stream().map(this::toResponse).toList();
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