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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitOpportunityAccessServiceTests {

    private ServiceProfitOpportunityRepository repository;
    private AuthorizationService authorizationService;
    private ServiceProfitOpportunityAccessService service;

    private UUID tenantId;
    private AuthenticatedTenantContext tenantContext;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceProfitOpportunityRepository.class);
        authorizationService = mock(AuthorizationService.class);

        service = new ServiceProfitOpportunityAccessService(
                repository,
                authorizationService
        );

        tenantId = UUID.randomUUID();

        tenantContext = new AuthenticatedTenantContext(
                UUID.randomUUID(),
                tenantId,
                "service-profit-user"
        );
    }

    @Test
    void requireOpportunityAuthorizesBusinessResource() {

        UUID opportunityId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                opportunity(
                        opportunityId,
                        tenantId
                );

        when(repository.findByIdAndTenantId(
                opportunityId,
                tenantId
        )).thenReturn(Optional.of(opportunity));

        ServiceProfitOpportunity result =
                service.requireOpportunity(
                        tenantContext,
                        opportunityId
                );

        assertEquals(opportunityId, result.getId());

        ArgumentCaptor<AuthorizationRequest> captor =
                ArgumentCaptor.forClass(
                        AuthorizationRequest.class
                );

        verify(authorizationService)
                .requirePermission(captor.capture());

        AuthorizationRequest request = captor.getValue();

        assertEquals(
                ServiceProfitPermissions.OPPORTUNITY_READ,
                request.permissionCode()
        );

        assertEquals(
                AuthorizationResourceType.SERVICE_PROFIT_OPPORTUNITY,
                request.resourceType()
        );

        assertEquals(
                opportunityId,
                request.resourceId()
        );
    }

    @Test
    void requireOpportunityDoesNotReturnAnotherTenantRecord() {

        UUID opportunityId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(
                opportunityId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requireOpportunity(
                                tenantContext,
                                opportunityId
                        )
                );

        assertEquals(404, exception.getStatusCode().value());
    }

    private ServiceProfitOpportunity opportunity(
            UUID id,
            UUID tenantId
    ) {
        return ServiceProfitOpportunity.detect(
                id,
                tenantId,
                null,
                null,
                null,
                null,
                null,
                "OPP-READ-1",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined work",
                null,
                null,
                null,
                "DEALER_IMPORT",
                "RO_LINE",
                "SRC-READ-1",
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                null,
                OffsetDateTime.now()
        );
    }
}
