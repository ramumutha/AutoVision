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
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuthorizationService authorizationService;
    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationScopeRepository authorizationScopeRepository;

    public BranchService(
            BranchRepository branchRepository,
            AuthorizationService authorizationService,
            AuthorizationRepository authorizationRepository,
            AuthorizationScopeRepository authorizationScopeRepository
    ) {
        this.branchRepository = branchRepository;
        this.authorizationService = authorizationService;
        this.authorizationRepository = authorizationRepository;
        this.authorizationScopeRepository = authorizationScopeRepository;
    }

    /**
     * Returns only branches contained by the caller's active permission-bearing
     * grants (S4.7.7.3D): TENANT -> all tenant branches, DEALER_GROUP ->
     * branches of member dealers, DEALER -> branches of that dealer, BRANCH ->
     * that exact branch. TENANT_GROUP -> resources across active member tenants. LOCATION grants
     * no Branch collection access. Results from multiple grants are unioned and deduplicated;
     * tenant isolation is enforced by every repository call.
     */
    public List<BranchResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        List<AuthorizationGrant> grants =
                authorizationRepository.findActivePermissionGrants(
                        tenantContext.userRefId(),
                        tenantContext.tenantId(),
                        OrganizationPermissions.BRANCH_READ
                );

        if (grants.isEmpty()) {
            throw new AccessDeniedException("Access is denied");
        }

        Set<Branch> branches = new LinkedHashSet<>();

        for (AuthorizationGrant grant : grants) {
            switch (grant.scopeType()) {
                case TENANT -> {
                    if (grant.scopeId().equals(tenantContext.tenantId())) {
                        branches.addAll(
                                branchRepository.findAllByTenantId(
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
                        branches.addAll(
                                branchRepository.findAllByTenantIdAndDealerIdIn(
                                        tenantContext.tenantId(),
                                        memberDealerIds
                                )
                        );
                    }
                }
                case DEALER -> branches.addAll(
                        branchRepository.findAllByTenantIdAndDealerId(
                                tenantContext.tenantId(),
                                grant.scopeId()
                        )
                );
                case BRANCH -> branchRepository
                        .findByIdAndTenantId(
                                grant.scopeId(),
                                tenantContext.tenantId()
                        )
                        .ifPresent(branches::add);
                case TENANT_GROUP -> branches.addAll(
                        branchRepository.findAllWithinActiveTenantGroup(
                                grant.scopeId(),
                                tenantContext.tenantId()
                        )
                );
                case LOCATION -> {
                    // This narrower scope grants no Branch collection access.
                }
            }
        }

        return branches.stream().map(this::toResponse).toList();
    }

    public BranchResponse findById(
            AuthenticatedTenantContext tenantContext,
            UUID branchId
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        OrganizationPermissions.BRANCH_READ,
                        AuthorizationResourceType.BRANCH,
                        branchId
                )
        );

        Branch branch = branchRepository
                .findByIdWithinAuthorizedTenantBoundary(
                        branchId,
                        tenantContext.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Branch not found"
                ));

        return toResponse(branch);
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getTenantId(),
                branch.getDealerId(),
                branch.getLocationId(),
                branch.getCode(),
                branch.getName(),
                branch.getStatus(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }
}