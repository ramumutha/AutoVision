package com.autovision.platform.authorization;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Evaluates whether an AuthorizationGrant contains a requested resource.
 * Knows nothing about permission codes, roles, permission sets, or JWTs -
 * scope containment only. TENANT_GROUP containment is based on active
 * persisted tenant-group membership; unsupported combinations deny.
 */
@Component
public class AuthorizationScopeEvaluator {

    private final AuthorizationScopeRepository scopeRepository;

    public AuthorizationScopeEvaluator(
            AuthorizationScopeRepository scopeRepository
    ) {
        this.scopeRepository = scopeRepository;
    }

    public boolean contains(
            AuthorizationGrant grant,
            UUID authenticatedTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (grant.scopeType()) {
            case TENANT -> containsFromTenant(
                    grant.scopeId(),
                    resourceType,
                    resourceId
            );
            case DEALER_GROUP -> containsFromDealerGroup(
                    grant.scopeId(),
                    authenticatedTenantId,
                    resourceType,
                    resourceId
            );
            case DEALER -> containsFromDealer(
                    grant.scopeId(),
                    authenticatedTenantId,
                    resourceType,
                    resourceId
            );
            case BRANCH -> containsFromBranch(
                    grant.scopeId(),
                    authenticatedTenantId,
                    resourceType,
                    resourceId
            );
            case LOCATION -> containsFromLocation(
                    grant.scopeId(),
                    authenticatedTenantId,
                    resourceType,
                    resourceId
            );
            case TENANT_GROUP -> containsFromTenantGroup(
                    grant.scopeId(),
                    resourceType,
                    resourceId
            );
        };
    }

    private boolean containsFromTenantGroup(
            UUID tenantGroupId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> scopeRepository.tenantBelongsToActiveTenantGroup(
                    resourceId,
                    tenantGroupId
            );
            case DEALER -> scopeRepository.dealerBelongsToActiveTenantGroup(
                    resourceId,
                    tenantGroupId
            );
            case BRANCH -> scopeRepository.branchBelongsToActiveTenantGroup(
                    resourceId,
                    tenantGroupId
            );
            case LOCATION -> scopeRepository.locationBelongsToActiveTenantGroup(
                    resourceId,
                    tenantGroupId
            );
        };
    }

    private boolean containsFromTenant(
            UUID grantTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> grantTenantId.equals(resourceId);
            case DEALER -> scopeRepository.dealerBelongsToTenant(
                    resourceId,
                    grantTenantId
            );
            case BRANCH -> scopeRepository.branchBelongsToTenant(
                    resourceId,
                    grantTenantId
            );
            case LOCATION -> scopeRepository.locationBelongsToTenant(
                    resourceId,
                    grantTenantId
            );
        };
    }

    private boolean containsFromDealerGroup(
            UUID dealerGroupId,
            UUID authenticatedTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> false;
            case DEALER -> scopeRepository.dealerIsMemberOfGroup(
                    resourceId,
                    dealerGroupId,
                    authenticatedTenantId
            );
            case BRANCH -> scopeRepository.branchBelongsToDealerGroup(
                    resourceId,
                    dealerGroupId,
                    authenticatedTenantId
            );
            case LOCATION -> scopeRepository.locationBelongsToDealerGroup(
                    resourceId,
                    dealerGroupId,
                    authenticatedTenantId
            );
        };
    }

    private boolean containsFromDealer(
            UUID grantDealerId,
            UUID authenticatedTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> false;
            case DEALER -> grantDealerId.equals(resourceId)
                    && scopeRepository.dealerBelongsToTenant(
                            resourceId,
                            authenticatedTenantId
                    );
            case BRANCH -> scopeRepository.branchBelongsToDealer(
                    resourceId,
                    grantDealerId,
                    authenticatedTenantId
            );
            case LOCATION -> scopeRepository.locationBelongsToDealer(
                    resourceId,
                    grantDealerId,
                    authenticatedTenantId
            );
        };
    }

    private boolean containsFromBranch(
            UUID grantBranchId,
            UUID authenticatedTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> false;
            case DEALER -> false;
            case BRANCH -> grantBranchId.equals(resourceId)
                    && scopeRepository.branchBelongsToTenant(
                            resourceId,
                            authenticatedTenantId
                    );
            case LOCATION -> scopeRepository.locationBelongsToBranch(
                    resourceId,
                    grantBranchId,
                    authenticatedTenantId
            );
        };
    }

    private boolean containsFromLocation(
            UUID grantLocationId,
            UUID authenticatedTenantId,
            AuthorizationResourceType resourceType,
            UUID resourceId
    ) {
        return switch (resourceType) {
            case TENANT -> false;
            case DEALER -> false;
            case BRANCH -> false;
            case LOCATION -> grantLocationId.equals(resourceId)
                    && scopeRepository.locationBelongsToTenant(
                            resourceId,
                            authenticatedTenantId
                    );
        };
    }
}
