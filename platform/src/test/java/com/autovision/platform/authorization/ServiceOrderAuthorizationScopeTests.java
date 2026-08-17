package com.autovision.platform.authorization;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServiceOrderAuthorizationScopeTests {

    private final AuthorizationScopeRepository repository =
            mock(AuthorizationScopeRepository.class);

    private final AuthorizationScopeEvaluator evaluator =
            new AuthorizationScopeEvaluator(repository);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();

    @Test
    void tenantGrantContainsServiceOrderWhenOrderBelongsToTenant() {
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantId
        );

        when(repository.serviceOrderBelongsToTenant(
                serviceOrderId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));

        verify(repository).serviceOrderBelongsToTenant(
                serviceOrderId,
                tenantId
        );
    }

    @Test
    void tenantGrantRejectsServiceOrderOutsideTenant() {
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantId
        );

        when(repository.serviceOrderBelongsToTenant(
                serviceOrderId,
                tenantId
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void dealerGrantContainsServiceOrderWhenOrderBelongsToDealer() {
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(repository.serviceOrderBelongsToDealer(
                serviceOrderId,
                dealerId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void dealerGrantRejectsServiceOrderFromSiblingDealer() {
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(repository.serviceOrderBelongsToDealer(
                serviceOrderId,
                dealerId,
                tenantId
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void branchGrantContainsServiceOrderWhenOrderBelongsToBranch() {
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        when(repository.serviceOrderBelongsToBranch(
                serviceOrderId,
                branchId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void branchGrantRejectsServiceOrderFromSiblingBranch() {
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        when(repository.serviceOrderBelongsToBranch(
                serviceOrderId,
                branchId,
                tenantId
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void dealerGroupGrantContainsServiceOrderWhenDealerIsMember() {
        UUID dealerGroupId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId
        );

        when(repository.serviceOrderBelongsToDealerGroup(
                serviceOrderId,
                dealerGroupId,
                tenantId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void tenantGroupGrantContainsServiceOrderWhenTenantIsActiveMember() {
        UUID tenantGroupId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(repository.serviceOrderBelongsToActiveTenantGroup(
                serviceOrderId,
                tenantGroupId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void inactiveOrNonMemberTenantGroupRejectsServiceOrder() {
        UUID tenantGroupId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(repository.serviceOrderBelongsToActiveTenantGroup(
                serviceOrderId,
                tenantGroupId
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
    }

    @Test
    void locationGrantNeverContainsServiceOrder() {
        AuthorizationScopeRepository isolatedRepository =
                mock(AuthorizationScopeRepository.class);

        AuthorizationScopeEvaluator isolatedEvaluator =
                new AuthorizationScopeEvaluator(isolatedRepository);

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.LOCATION,
                UUID.randomUUID()
        );

        assertFalse(isolatedEvaluator.contains(
                grant,
                tenantId,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));

        verifyNoInteractions(isolatedRepository);
    }
}