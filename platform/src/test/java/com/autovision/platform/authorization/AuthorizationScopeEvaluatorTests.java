package com.autovision.platform.authorization;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationScopeEvaluatorTests {

    private final AuthorizationScopeRepository scopeRepository =
            mock(AuthorizationScopeRepository.class);

    private final AuthorizationScopeEvaluator evaluator =
            new AuthorizationScopeEvaluator(scopeRepository);

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @Test
    void tenantGrantAllowsSameTenant() {
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantA
        );

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.TENANT,
                tenantA
        ));
    }

    @Test
    void tenantGrantAllowsDealerInSameTenant() {
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantA
        );

        when(scopeRepository.dealerBelongsToTenant(dealerId, tenantA))
                .thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void tenantGrantDeniesDealerInAnotherTenant() {
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantA
        );

        when(scopeRepository.dealerBelongsToTenant(dealerId, tenantA))
                .thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void dealerGrantAllowsExactDealer() {
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(scopeRepository.dealerBelongsToTenant(dealerId, tenantA))
                .thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void dealerGrantAllowsBranchUnderDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(scopeRepository.branchBelongsToDealer(
                branchId,
                dealerId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                branchId
        ));
    }

    @Test
    void dealerGrantAllowsLocationUnderDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(scopeRepository.locationBelongsToDealer(
                locationId,
                dealerId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                locationId
        ));
    }

    @Test
    void dealerGrantDeniesAnotherDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID otherDealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                otherDealerId
        ));
    }

    @Test
    void dealerGrantDeniesBranchUnderAnotherDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER,
                dealerId
        );

        when(scopeRepository.branchBelongsToDealer(
                branchId,
                dealerId,
                tenantA
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                branchId
        ));
    }

    @Test
    void dealerGroupGrantAllowsMemberDealer() {
        UUID dealerGroupId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId
        );

        when(scopeRepository.dealerIsMemberOfGroup(
                dealerId,
                dealerGroupId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void dealerGroupGrantAllowsMemberDealersBranch() {
        UUID dealerGroupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId
        );

        when(scopeRepository.branchBelongsToDealerGroup(
                branchId,
                dealerGroupId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                branchId
        ));
    }

    @Test
    void dealerGroupGrantAllowsMemberDealersLocation() {
        UUID dealerGroupId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId
        );

        when(scopeRepository.locationBelongsToDealerGroup(
                locationId,
                dealerGroupId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                locationId
        ));
    }

    @Test
    void dealerGroupGrantDeniesNonMemberDealer() {
        UUID dealerGroupId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP,
                dealerGroupId
        );

        when(scopeRepository.dealerIsMemberOfGroup(
                dealerId,
                dealerGroupId,
                tenantA
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void branchGrantAllowsExactBranch() {
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        when(scopeRepository.branchBelongsToTenant(branchId, tenantA))
                .thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                branchId
        ));
    }

    @Test
    void branchGrantAllowsLocationUnderBranch() {
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        when(scopeRepository.locationBelongsToBranch(
                locationId,
                branchId,
                tenantA
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                locationId
        ));
    }

    @Test
    void branchGrantDeniesSiblingBranch() {
        UUID branchId = UUID.randomUUID();
        UUID siblingBranchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                siblingBranchId
        ));
    }

    @Test
    void branchGrantDeniesParentDealer() {
        UUID branchId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH,
                branchId
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void locationGrantAllowsExactLocation() {
        UUID locationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.LOCATION,
                locationId
        );

        when(scopeRepository.locationBelongsToTenant(locationId, tenantA))
                .thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                locationId
        ));
    }

    @Test
    void locationGrantDeniesSiblingLocation() {
        UUID locationId = UUID.randomUUID();
        UUID siblingLocationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.LOCATION,
                locationId
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                siblingLocationId
        ));
    }

    @Test
    void locationGrantDeniesParentBranch() {
        UUID locationId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.LOCATION,
                locationId
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.BRANCH,
                branchId
        ));
    }

    @Test
    void tenantGroupGrantAllowsMemberTenant() {
        UUID tenantGroupId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(scopeRepository.tenantBelongsToActiveTenantGroup(
                tenantB,
                tenantGroupId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.TENANT,
                tenantB
        ));
    }

    @Test
    void tenantGroupGrantAllowsDealerInMemberTenant() {
        UUID tenantGroupId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(scopeRepository.dealerBelongsToActiveTenantGroup(
                dealerId,
                tenantGroupId
        )).thenReturn(true);

        assertTrue(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.DEALER,
                dealerId
        ));
    }

    @Test
    void tenantGroupGrantDeniesResourceOutsideGroup() {
        UUID tenantGroupId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP,
                tenantGroupId
        );

        when(scopeRepository.locationBelongsToActiveTenantGroup(
                locationId,
                tenantGroupId
        )).thenReturn(false);

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.LOCATION,
                locationId
        ));
    }

    @Test
    void tenantAToTenantBIsDenied() {
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT,
                tenantA
        );

        assertFalse(evaluator.contains(
                grant,
                tenantA,
                AuthorizationResourceType.TENANT,
                tenantB
        ));
    }

    @Test
    void tenantGrantAllowsWorkflowInSameTenant() {
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT, tenantA);
        when(scopeRepository.serviceWorkflowBelongsToTenant(workflowId, tenantA))
                .thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void tenantGrantDeniesWorkflowInAnotherTenant() {
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT, tenantA);
        when(scopeRepository.serviceWorkflowBelongsToTenant(workflowId, tenantA))
                .thenReturn(false);

        assertFalse(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void dealerGrantAllowsWorkflowScopedToDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER, dealerId);
        when(scopeRepository.serviceWorkflowBelongsToDealer(
                workflowId, dealerId, tenantA)).thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void dealerGrantAllowsWorkflowScopedToContainedBranch() {
        UUID dealerId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER, dealerId);
        when(scopeRepository.serviceWorkflowBelongsToDealer(
                workflowId, dealerId, tenantA)).thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void dealerGrantDeniesWorkflowOutsideDealer() {
        UUID dealerId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER, dealerId);
        when(scopeRepository.serviceWorkflowBelongsToDealer(
                workflowId, dealerId, tenantA)).thenReturn(false);

        assertFalse(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void branchGrantAllowsWorkflowScopedToBranch() {
        UUID branchId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH, branchId);
        when(scopeRepository.serviceWorkflowBelongsToBranch(
                workflowId, branchId, tenantA)).thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void branchGrantDeniesWorkflowOutsideBranch() {
        UUID branchId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.BRANCH, branchId);
        when(scopeRepository.serviceWorkflowBelongsToBranch(
                workflowId, branchId, tenantA)).thenReturn(false);

        assertFalse(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void dealerGroupGrantAllowsWorkflowForMemberDealer() {
        UUID dealerGroupId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.DEALER_GROUP, dealerGroupId);
        when(scopeRepository.serviceWorkflowBelongsToDealerGroup(
                workflowId, dealerGroupId, tenantA)).thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void tenantGroupGrantAllowsWorkflowInActiveMemberTenant() {
        UUID tenantGroupId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.TENANT_GROUP, tenantGroupId);
        when(scopeRepository.serviceWorkflowBelongsToActiveTenantGroup(
                workflowId, tenantGroupId)).thenReturn(true);

        assertTrue(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
    }

    @Test
    void locationGrantDoesNotContainWorkflow() {
        UUID locationId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        AuthorizationGrant grant = new AuthorizationGrant(
                AuthorizationScopeType.LOCATION, locationId);

        assertFalse(evaluator.contains(grant, tenantA,
                AuthorizationResourceType.SERVICE_WORKFLOW, workflowId));
        org.mockito.Mockito.verifyNoInteractions(scopeRepository);
    }
}
