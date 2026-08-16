package com.autovision.platform.organization;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationScopeRepository;
import com.autovision.platform.authorization.AuthorizationScopeType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocationServiceTests {

    private final LocationRepository repository =
            mock(LocationRepository.class);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final AuthorizationRepository authorizationRepository =
            mock(AuthorizationRepository.class);

    private final AuthorizationScopeRepository authorizationScopeRepository =
            mock(AuthorizationScopeRepository.class);

    private final LocationService service =
            new LocationService(
                    repository,
                    authorizationService,
                    authorizationRepository,
                    authorizationScopeRepository
            );

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.fromString(
                            "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
                    ),
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void tenantGrantListsAllTenantLocations()
            throws Exception {

        Location location = location(
                UUID.randomUUID(),
                tenantId,
                "L001",
                "Demo Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.TENANT,
                                tenantId
                        )
                )
        );

        when(repository.findAllByTenantId(tenantId))
                .thenReturn(List.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("L001", result.getFirst().code());

        verify(repository)
                .findAllByTenantId(tenantId);
    }

    @Test
    void dealerGroupGrantListsOnlyMemberDealerLocations()
            throws Exception {

        UUID dealerGroupId = UUID.randomUUID();
        UUID memberDealerId = UUID.randomUUID();

        Location location = location(
                UUID.randomUUID(),
                tenantId,
                "L002",
                "Dealer Group Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.DEALER_GROUP,
                                dealerGroupId
                        )
                )
        );

        when(authorizationScopeRepository.findDealerIdsInGroup(
                dealerGroupId,
                tenantId
        )).thenReturn(List.of(memberDealerId));

        when(repository.findAllByTenantIdAndDealerIdIn(
                tenantId,
                List.of(memberDealerId)
        )).thenReturn(List.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("L002", result.getFirst().code());

        verify(repository)
                .findAllByTenantIdAndDealerIdIn(
                        tenantId,
                        List.of(memberDealerId)
                );
    }

    @Test
    void dealerGrantListsOnlyThatDealersLocations()
            throws Exception {

        UUID dealerId = UUID.randomUUID();

        Location location = location(
                UUID.randomUUID(),
                tenantId,
                "L003",
                "Dealer Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.DEALER,
                                dealerId
                        )
                )
        );

        when(repository.findAllByTenantIdAndDealerIdIn(
                tenantId,
                List.of(dealerId)
        )).thenReturn(List.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("L003", result.getFirst().code());

        verify(repository)
                .findAllByTenantIdAndDealerIdIn(
                        tenantId,
                        List.of(dealerId)
                );
    }

    @Test
    void branchGrantListsOnlyThatBranchLocations()
            throws Exception {

        UUID branchId = UUID.randomUUID();

        Location location = location(
                UUID.randomUUID(),
                tenantId,
                "L004",
                "Branch Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.BRANCH,
                                branchId
                        )
                )
        );

        when(repository.findAllByTenantIdAndBranchIdIn(
                tenantId,
                List.of(branchId)
        )).thenReturn(List.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("L004", result.getFirst().code());

        verify(repository)
                .findAllByTenantIdAndBranchIdIn(
                        tenantId,
                        List.of(branchId)
                );
    }

    @Test
    void locationGrantListsOnlyExactLocation()
            throws Exception {

        UUID locationId = UUID.randomUUID();

        Location location = location(
                locationId,
                tenantId,
                "L005",
                "Exact Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.LOCATION,
                                locationId
                        )
                )
        );

        when(repository.findByIdAndTenantId(
                locationId,
                tenantId
        )).thenReturn(Optional.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(locationId, result.getFirst().id());

        verify(repository)
                .findByIdAndTenantId(
                        locationId,
                        tenantId
                );
    }

    @Test
    void combinesAndDeduplicatesMultipleLocationGrants()
            throws Exception {

        UUID dealerId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Location location = location(
                locationId,
                tenantId,
                "L006",
                "Shared Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.DEALER,
                                dealerId
                        ),
                        new AuthorizationGrant(
                                AuthorizationScopeType.LOCATION,
                                locationId
                        )
                )
        );

        when(repository.findAllByTenantIdAndDealerIdIn(
                tenantId,
                List.of(dealerId)
        )).thenReturn(List.of(location));

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                locationId,
                tenantId
        )).thenReturn(Optional.of(location));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(locationId, result.getFirst().id());
    }

    @Test
    void tenantGroupGrantListsResourcesAcrossMemberTenants() throws Exception {
        UUID tenantGroupId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Location resource = location(
                resourceId,
                otherTenantId,
                "L-TG",
                "Tenant Group Location"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT_GROUP,
                        tenantGroupId
                )
        ));

        when(repository.findAllWithinActiveTenantGroup(
                tenantGroupId,
                tenantId
        )).thenReturn(List.of(resource));

        List<LocationResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(resourceId, result.getFirst().id());
        assertEquals(otherTenantId, result.getFirst().tenantId());

        verify(repository).findAllWithinActiveTenantGroup(
                tenantGroupId,
                tenantId
        );
    }

    @Test
    void deniesFindAllWhenNoPermissionBearingGrants() {

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.findAll(context)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void tenantGrantForAnotherTenantGrantsNoLocationAccess() {

        UUID otherTenantId = UUID.randomUUID();

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.LOCATION_READ
        )).thenReturn(
                List.of(
                        new AuthorizationGrant(
                                AuthorizationScopeType.TENANT,
                                otherTenantId
                        )
                )
        );

        List<LocationResponse> result =
                service.findAll(context);

        assertTrue(result.isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void findsLocationOnlyWithinAuthenticatedTenant()
            throws Exception {

        UUID locationId = UUID.randomUUID();

        Location location = location(
                locationId,
                tenantId,
                "L001",
                "Demo Location"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                locationId,
                tenantId
        )).thenReturn(Optional.of(location));

        LocationResponse result =
                service.findById(
                        context,
                        locationId
                );

        assertEquals(
                locationId,
                result.id()
        );

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        locationId,
                        tenantId
                );

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.LOCATION_READ,
                                AuthorizationResourceType.LOCATION,
                                locationId
                        )
                );
    }

    @Test
    void findsLocationInAuthorizedTenantGroupMemberTenant()
            throws Exception {

        UUID otherTenantId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Location location = location(
                locationId,
                otherTenantId,
                "L-XTENANT",
                "Cross Tenant Location"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                locationId,
                tenantId
        )).thenReturn(Optional.of(location));

        LocationResponse result =
                service.findById(context, locationId);

        assertEquals(locationId, result.id());

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.LOCATION_READ,
                                AuthorizationResourceType.LOCATION,
                                locationId
                        )
                );

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        locationId,
                        tenantId
                );
    }

    @Test
    void returnsNotFoundForLocationOutsideTenant() {

        UUID locationId = UUID.randomUUID();

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                locationId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                locationId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void deniesFindByIdWhenPermissionMissing() {

        UUID locationId = UUID.randomUUID();

        doThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        )
                .when(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.LOCATION_READ,
                                AuthorizationResourceType.LOCATION,
                                locationId
                        )
                );

        assertThrows(
                AccessDeniedException.class,
                () -> service.findById(
                        context,
                        locationId
                )
        );

        verifyNoInteractions(repository);
    }

    private Location location(
            UUID id,
            UUID tenantId,
            String code,
            String name
    ) throws Exception {

        Location location = new Location();

        set(location, "id", id);
        set(location, "tenantId", tenantId);
        set(location, "code", code);
        set(location, "name", name);
        set(location, "city", "Bengaluru");
        set(location, "countryCode", "IN");
        set(location, "timezone", "Asia/Kolkata");

        set(
                location,
                "latitude",
                new BigDecimal("12.971599")
        );

        set(
                location,
                "longitude",
                new BigDecimal("77.594566")
        );

        set(
                location,
                "status",
                OrganizationStatus.ACTIVE
        );

        set(
                location,
                "createdAt",
                OffsetDateTime.now()
        );

        set(
                location,
                "updatedAt",
                OffsetDateTime.now()
        );

        return location;
    }

    private void set(
            Object target,
            String fieldName,
            Object value
    ) throws Exception {

        Field field =
                target.getClass()
                        .getDeclaredField(fieldName);

        field.setAccessible(true);
        field.set(target, value);
    }
}
