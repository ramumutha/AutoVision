package com.autovision.platform.organization;

import java.lang.reflect.Field;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DealerServiceTests {

    private final DealerRepository repository =
            mock(DealerRepository.class);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final AuthorizationRepository authorizationRepository =
            mock(AuthorizationRepository.class);

    private final AuthorizationScopeRepository authorizationScopeRepository =
            mock(AuthorizationScopeRepository.class);

    private final DealerService service =
            new DealerService(
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
    void tenantGrantListsAllTenantDealers() throws Exception {
        Dealer dealer = dealer(
                UUID.randomUUID(),
                tenantId,
                "D001",
                "Demo Dealer"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(AuthorizationScopeType.TENANT, tenantId)
        ));

        when(repository.findAllByTenantId(tenantId))
                .thenReturn(List.of(dealer));

        List<DealerResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals("D001", result.getFirst().code());

        verify(repository)
                .findAllByTenantId(tenantId);
    }

    @Test
    void dealerGroupGrantListsOnlyMemberDealers() throws Exception {
        UUID dealerGroupId = UUID.randomUUID();
        UUID memberDealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                memberDealerId,
                tenantId,
                "D002",
                "Member Dealer"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER_GROUP,
                        dealerGroupId
                )
        ));

        when(authorizationScopeRepository.findDealerIdsInGroup(
                dealerGroupId,
                tenantId
        )).thenReturn(List.of(memberDealerId));

        when(repository.findAllByTenantIdAndIdIn(
                tenantId,
                List.of(memberDealerId)
        )).thenReturn(List.of(dealer));

        List<DealerResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(memberDealerId, result.getFirst().id());

        verify(repository, never()).findAllByTenantId(tenantId);
    }

    @Test
    void dealerGrantListsOnlyThatDealer() throws Exception {
        UUID dealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                dealerId,
                tenantId,
                "D003",
                "Exact Dealer"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(AuthorizationScopeType.DEALER, dealerId)
        ));

        when(repository.findByIdAndTenantId(dealerId, tenantId))
                .thenReturn(Optional.of(dealer));

        List<DealerResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(dealerId, result.getFirst().id());
    }

    @Test
    void combinesAndDeduplicatesMultipleGrants() throws Exception {
        UUID dealerGroupId = UUID.randomUUID();
        UUID sharedDealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                sharedDealerId,
                tenantId,
                "D004",
                "Shared Dealer"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER,
                        sharedDealerId
                ),
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER_GROUP,
                        dealerGroupId
                )
        ));

        when(repository.findByIdWithinAuthorizedTenantBoundary(sharedDealerId, tenantId))
                .thenReturn(Optional.of(dealer));

        when(authorizationScopeRepository.findDealerIdsInGroup(
                dealerGroupId,
                tenantId
        )).thenReturn(List.of(sharedDealerId));

        when(repository.findAllByTenantIdAndIdIn(
                tenantId,
                List.of(sharedDealerId)
        )).thenReturn(List.of(dealer));

        List<DealerResponse> result =
                service.findAll(context);

        assertEquals(1, result.size());
        assertEquals(sharedDealerId, result.getFirst().id());
    }

    @Test
    void tenantGroupGrantListsResourcesAcrossMemberTenants() throws Exception {
        UUID tenantGroupId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Dealer resource = dealer(
                resourceId,
                otherTenantId,
                "D-TG",
                "Tenant Group Dealer"
        );

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
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

        List<DealerResponse> result =
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
    void unsupportedNarrowerScopeReturnsEmptyCollection() {
        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.BRANCH,
                        UUID.randomUUID()
                )
        ));

        List<DealerResponse> result = service.findAll(context);

        assertTrue(result.isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void deniesFindAllWhenNoPermissionBearingGrants() {
        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.findAll(context)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void tenantGrantForAnotherTenantGrantsNoAccess() {
        UUID otherTenantId = UUID.randomUUID();

        when(authorizationRepository.findActivePermissionGrants(
                context.userRefId(),
                tenantId,
                OrganizationPermissions.DEALER_READ
        )).thenReturn(List.of(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        otherTenantId
                )
        ));

        List<DealerResponse> result = service.findAll(context);

        assertTrue(result.isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void findsDealerOnlyWithinAuthenticatedTenant()
            throws Exception {

        UUID dealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                dealerId,
                tenantId,
                "D001",
                "Demo Dealer"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                dealerId,
                tenantId
        )).thenReturn(Optional.of(dealer));

        DealerResponse result =
                service.findById(context, dealerId);

        assertEquals(dealerId, result.id());

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        dealerId,
                        tenantId
                );

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.DEALER_READ,
                                AuthorizationResourceType.DEALER,
                                dealerId
                        )
                );
    }

    @Test
    void findsDealerInAuthorizedTenantGroupMemberTenant()
            throws Exception {

        UUID otherTenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        Dealer dealer = dealer(
                dealerId,
                otherTenantId,
                "D-XTENANT",
                "Cross Tenant Dealer"
        );

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                dealerId,
                tenantId
        )).thenReturn(Optional.of(dealer));

        DealerResponse result =
                service.findById(context, dealerId);

        assertEquals(dealerId, result.id());

        verify(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.DEALER_READ,
                                AuthorizationResourceType.DEALER,
                                dealerId
                        )
                );

        verify(repository)
                .findByIdWithinAuthorizedTenantBoundary(
                        dealerId,
                        tenantId
                );
    }

    @Test
    void returnsNotFoundForDealerOutsideTenant() {
        UUID dealerId = UUID.randomUUID();

        when(repository.findByIdWithinAuthorizedTenantBoundary(
                dealerId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                dealerId
                        )
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );
    }

    @Test
    void deniesFindByIdWhenPermissionMissing() {
        UUID dealerId = UUID.randomUUID();

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(
                        new AuthorizationRequest(
                                context,
                                OrganizationPermissions.DEALER_READ,
                                AuthorizationResourceType.DEALER,
                                dealerId
                        )
                );

        assertThrows(
                AccessDeniedException.class,
                () -> service.findById(context, dealerId)
        );

        verifyNoInteractions(repository);
    }

    private Dealer dealer(
            UUID id,
            UUID tenantId,
            String code,
            String name
    ) throws Exception {

        Dealer dealer = new Dealer();

        set(dealer, "id", id);
        set(dealer, "tenantId", tenantId);
        set(dealer, "code", code);
        set(dealer, "name", name);
        set(
                dealer,
                "status",
                OrganizationStatus.ACTIVE
        );
        set(
                dealer,
                "createdAt",
                OffsetDateTime.now()
        );
        set(
                dealer,
                "updatedAt",
                OffsetDateTime.now()
        );

        return dealer;
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
