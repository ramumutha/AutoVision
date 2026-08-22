package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitOpportunityCommandServiceTests {

    private ServiceProfitOpportunityRepository repository;
    private AuthorizationService authorizationService;
    private ServiceProfitOpportunityCommandService service;

    private UUID tenantId;
    private UUID userRefId;
    private AuthenticatedTenantContext tenantContext;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceProfitOpportunityRepository.class);
        authorizationService = mock(AuthorizationService.class);

        service = new ServiceProfitOpportunityCommandService(
                repository,
                authorizationService
        );

        tenantId = UUID.randomUUID();
        userRefId = UUID.randomUUID();

        tenantContext = new AuthenticatedTenantContext(
                userRefId,
                tenantId,
                "service-profit-user"
        );

        when(repository.existsByTenantIdAndOpportunityKey(
                any(),
                any()
        )).thenReturn(false);

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUsesLocationScopeWhenLocationProvided() {

        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        service.create(
                tenantContext,
                request(
                        dealerId,
                        branchId,
                        locationId,
                        "OPP-LOCATION-1"
                )
        );

        AuthorizationRequest authorization =
                captureAuthorizationRequest();

        assertEquals(
                ServiceProfitPermissions.OPPORTUNITY_CREATE,
                authorization.permissionCode()
        );

        assertEquals(
                AuthorizationResourceType.LOCATION,
                authorization.resourceType()
        );

        assertEquals(
                locationId,
                authorization.resourceId()
        );
    }

    @Test
    void createUsesBranchScopeWhenNoLocationProvided() {

        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        service.create(
                tenantContext,
                request(
                        dealerId,
                        branchId,
                        null,
                        "OPP-BRANCH-1"
                )
        );

        AuthorizationRequest authorization =
                captureAuthorizationRequest();

        assertEquals(
                AuthorizationResourceType.BRANCH,
                authorization.resourceType()
        );

        assertEquals(
                branchId,
                authorization.resourceId()
        );
    }

    @Test
    void createUsesDealerScopeWhenNoBranchProvided() {

        UUID dealerId = UUID.randomUUID();

        service.create(
                tenantContext,
                request(
                        dealerId,
                        null,
                        null,
                        "OPP-DEALER-1"
                )
        );

        AuthorizationRequest authorization =
                captureAuthorizationRequest();

        assertEquals(
                AuthorizationResourceType.DEALER,
                authorization.resourceType()
        );

        assertEquals(
                dealerId,
                authorization.resourceId()
        );
    }

    @Test
    void createUsesTenantScopeWhenNoOrganizationChildProvided() {

        service.create(
                tenantContext,
                request(
                        null,
                        null,
                        null,
                        "OPP-TENANT-1"
                )
        );

        AuthorizationRequest authorization =
                captureAuthorizationRequest();

        assertEquals(
                AuthorizationResourceType.TENANT,
                authorization.resourceType()
        );

        assertEquals(
                tenantId,
                authorization.resourceId()
        );
    }

    @Test
    void createRejectsLocationWithoutBranch() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.create(
                                tenantContext,
                                request(
                                        UUID.randomUUID(),
                                        null,
                                        UUID.randomUUID(),
                                        "OPP-BAD-LOCATION"
                                )
                        )
                );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createRejectsBranchWithoutDealer() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.create(
                                tenantContext,
                                request(
                                        null,
                                        UUID.randomUUID(),
                                        null,
                                        "OPP-BAD-BRANCH"
                                )
                        )
                );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createRejectsDuplicateOpportunityKeyWithinTenant() {

        when(repository.existsByTenantIdAndOpportunityKey(
                tenantId,
                "OPP-DUPLICATE"
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.create(
                                tenantContext,
                                request(
                                        null,
                                        null,
                                        null,
                                        "OPP-DUPLICATE"
                                )
                        )
                );

        assertEquals(409, exception.getStatusCode().value());
    }

    private AuthorizationRequest captureAuthorizationRequest() {

        ArgumentCaptor<AuthorizationRequest> captor =
                ArgumentCaptor.forClass(
                        AuthorizationRequest.class
                );

        verify(authorizationService)
                .requirePermission(captor.capture());

        return captor.getValue();
    }

    private CreateServiceProfitOpportunityRequest request(
            UUID dealerId,
            UUID branchId,
            UUID locationId,
            String opportunityKey
    ) {
        return new CreateServiceProfitOpportunityRequest(
                dealerId,
                branchId,
                locationId,
                null,
                null,
                opportunityKey,
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined brake work",
                "Customer previously declined brake work.",
                null,
                null,
                "DEALER_IMPORT",
                "RO_LINE",
                "SRC-1",
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+00:00"
                )
        );
    }
}
