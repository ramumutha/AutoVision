package com.autovision.platform.authorization;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AfterSalesAuthorizationScopeEvaluatorTests {

    private final AuthorizationScopeRepository scopeRepository =
            mock(AuthorizationScopeRepository.class);

    private final AuthorizationScopeEvaluator evaluator =
            new AuthorizationScopeEvaluator(scopeRepository);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();

    @Test
    void tenantGrantContainsCaseInTenant() {
        when(scopeRepository.afterSalesCaseBelongsToTenant(
                caseId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        tenantId
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }

    @Test
    void dealerGrantContainsCaseForDealer() {
        UUID dealerId = UUID.randomUUID();

        when(scopeRepository.afterSalesCaseBelongsToDealer(
                caseId,
                dealerId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER,
                        dealerId
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }

    @Test
    void branchGrantContainsCaseForBranch() {
        UUID branchId = UUID.randomUUID();

        when(scopeRepository.afterSalesCaseBelongsToBranch(
                caseId,
                branchId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.BRANCH,
                        branchId
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }

    @Test
    void dealerGroupGrantContainsCaseForMemberDealer() {
        UUID dealerGroupId = UUID.randomUUID();

        when(scopeRepository.afterSalesCaseBelongsToDealerGroup(
                caseId,
                dealerGroupId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.DEALER_GROUP,
                        dealerGroupId
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }

    @Test
    void tenantGroupGrantContainsCaseInActiveMemberTenant() {
        UUID tenantGroupId = UUID.randomUUID();

        when(scopeRepository.afterSalesCaseBelongsToActiveTenantGroup(
                caseId,
                tenantGroupId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT_GROUP,
                        tenantGroupId
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }

    @Test
    void locationGrantDoesNotContainAfterSalesCase() {
        assertFalse(evaluator.contains(
                new AuthorizationGrant(
                        AuthorizationScopeType.LOCATION,
                        UUID.randomUUID()
                ),
                tenantId,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        ));
    }
}