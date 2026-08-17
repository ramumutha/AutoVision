package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AfterSalesCaseServiceTests {

    private final AfterSalesCaseRepository repository =
            mock(AfterSalesCaseRepository.class);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final AfterSalesCaseService service =
            new AfterSalesCaseService(
                    repository,
                    authorizationService
            );

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final UUID principalId =
            UUID.fromString(
                    "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    principalId,
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void findsCaseOnlyWithinAuthenticatedTenant() {
        UUID caseId = UUID.randomUUID();
        AfterSalesCase afterSalesCase = caseRecord(caseId);

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(afterSalesCase));

        AfterSalesCase result =
                service.findById(context, caseId);

        assertSame(afterSalesCase, result);

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    @Test
    void deniesFindByIdBeforeRepositoryAccess() {
        UUID caseId = UUID.randomUUID();
        AuthorizationRequest request =
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                );

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(request);

        assertThrows(
                AccessDeniedException.class,
                () -> service.findById(context, caseId)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void returnsNotFoundWhenCaseIsAbsent() {
        UUID caseId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(context, caseId)
                );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsDuplicateCaseNumberWithinTenant() {
        when(repository.existsByTenantIdAndCaseNumber(
                tenantId,
                "ASC-2002"
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.open(
                                context,
                                "ASC-2002",
                                null,
                                null,
                                AfterSalesCaseSourceChannel.SERVICE_ADVISOR
                        )
                );

        assertEquals(409, exception.getStatusCode().value());

        verify(repository, never())
                .save(any(AfterSalesCase.class));
    }

    @Test
    void createAtBranchAuthorizesAgainstBranch() {
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(repository.save(any(AfterSalesCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.open(
                context,
                "ASC-2003",
                dealerId,
                branchId,
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR
        );

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_CREATE,
                        AuthorizationResourceType.BRANCH,
                        branchId
                )
        );
    }

    @Test
    void createAtDealerAuthorizesAgainstDealer() {
        UUID dealerId = UUID.randomUUID();

        when(repository.save(any(AfterSalesCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.open(
                context,
                "ASC-2004",
                dealerId,
                null,
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR
        );

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_CREATE,
                        AuthorizationResourceType.DEALER,
                        dealerId
                )
        );
    }

    @Test
    void createWithoutDealerOrBranchAuthorizesAgainstTenant() {
        when(repository.save(any(AfterSalesCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.open(
                context,
                "ASC-2005",
                null,
                null,
                AfterSalesCaseSourceChannel.CUSTOMER_PORTAL
        );

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_CREATE,
                        AuthorizationResourceType.TENANT,
                        tenantId
                )
        );
    }

    @Test
    void opensCaseWithTenantAndAuditContext() {
        when(repository.save(any(AfterSalesCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AfterSalesCase result =
                service.open(
                        context,
                        "ASC-2006",
                        null,
                        null,
                        AfterSalesCaseSourceChannel.CUSTOMER_PORTAL
                );

        assertNotNull(result.getId());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(AfterSalesCaseStatus.OPEN, result.getLifecycleStatus());
        assertEquals(principalId, result.getCreatedByPrincipalId());
        assertNull(result.getClosedAt());
    }

    @Test
    void closeAuthorizesUpdateAgainstCase() {
        UUID caseId = UUID.randomUUID();
        AfterSalesCase afterSalesCase = caseRecord(caseId);

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(afterSalesCase));

        AfterSalesCase result =
                service.close(context, caseId);

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CASE_UPDATE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );

        assertEquals(
                AfterSalesCaseStatus.CLOSED,
                result.getLifecycleStatus()
        );
        assertNotNull(result.getClosedAt());
    }


    @Test
    void rejectsBranchWithoutDealerBeforeAuthorizationOrPersistence() {
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.open(
                                context,
                                "ASC-INVALID",
                                null,
                                UUID.randomUUID(),
                                AfterSalesCaseSourceChannel.SERVICE_ADVISOR
                        )
                );

        assertEquals(400, exception.getStatusCode().value());

        verifyNoInteractions(repository);
        verifyNoInteractions(authorizationService);
    }
    private AfterSalesCase caseRecord(UUID caseId) {
        return AfterSalesCase.open(
                caseId,
                tenantId,
                null,
                null,
                "ASC-TEST",
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR,
                principalId,
                OffsetDateTime.now()
        );
    }
}