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
public class DealerService {

    private final DealerRepository dealerRepository;
    private final AuthorizationService authorizationService;
    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationScopeRepository authorizationScopeRepository;

    public DealerService(
            DealerRepository dealerRepository,
            AuthorizationService authorizationService,
            AuthorizationRepository authorizationRepository,
            AuthorizationScopeRepository authorizationScopeRepository
    ) {
        this.dealerRepository = dealerRepository;
        this.authorizationService = authorizationService;
        this.authorizationRepository = authorizationRepository;
        this.authorizationScopeRepository = authorizationScopeRepository;
    }

    /**
     * Returns only dealers contained by the caller's active permission-bearing
     * grants (S4.7.7.3D): TENANT -> all tenant dealers, DEALER_GROUP -> member
     * dealers, DEALER -> that exact dealer. BRANCH/LOCATION/TENANT_GROUP grant
     * no Dealer collection access. Results from multiple grants are unioned
     * and deduplicated; tenant isolation is enforced by every repository call.
     */
    public List<DealerResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        List<AuthorizationGrant> grants =
                authorizationRepository.findActivePermissionGrants(
                        tenantContext.userRefId(),
                        tenantContext.tenantId(),
                        OrganizationPermissions.DEALER_READ
                );

        if (grants.isEmpty()) {
            throw new AccessDeniedException("Access is denied");
        }

        Set<Dealer> dealers = new LinkedHashSet<>();

        for (AuthorizationGrant grant : grants) {
            switch (grant.scopeType()) {
                case TENANT -> {
                    if (grant.scopeId().equals(tenantContext.tenantId())) {
                        dealers.addAll(
                                dealerRepository.findAllByTenantId(
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
                        dealers.addAll(
                                dealerRepository.findAllByTenantIdAndIdIn(
                                        tenantContext.tenantId(),
                                        memberDealerIds
                                )
                        );
                    }
                }
                case DEALER -> dealerRepository
                        .findByIdAndTenantId(
                                grant.scopeId(),
                                tenantContext.tenantId()
                        )
                        .ifPresent(dealers::add);
                case BRANCH, LOCATION, TENANT_GROUP -> {
                    // These scopes grant no Dealer collection access.
                }
            }
        }

        return dealers.stream().map(this::toResponse).toList();
    }

    public DealerResponse findById(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        OrganizationPermissions.DEALER_READ,
                        AuthorizationResourceType.DEALER,
                        dealerId
                )
        );

        Dealer dealer = dealerRepository
                .findByIdWithinAuthorizedTenantBoundary(
                        dealerId,
                        tenantContext.tenantId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Dealer not found"
                ));

        return toResponse(dealer);
    }

    private DealerResponse toResponse(Dealer dealer) {
        return new DealerResponse(
                dealer.getId(),
                dealer.getCode(),
                dealer.getName(),
                dealer.getLegalName(),
                dealer.getPrimaryLocationId(),
                dealer.getStatus(),
                dealer.getCreatedAt(),
                dealer.getUpdatedAt()
        );
    }
}