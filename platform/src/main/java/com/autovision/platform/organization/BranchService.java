package com.autovision.platform.organization;

import java.util.List;
import java.util.UUID;

import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.authorization.OrganizationPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuthorizationService authorizationService;

    public BranchService(
            BranchRepository branchRepository,
            AuthorizationService authorizationService
    ) {
        this.branchRepository = branchRepository;
        this.authorizationService = authorizationService;
    }

    public List<BranchResponse> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        authorizationService.requirePermission(
                tenantContext,
                OrganizationPermissions.BRANCH_READ
        );

        return branchRepository
                .findAllByTenantId(tenantContext.tenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse findById(
            AuthenticatedTenantContext tenantContext,
            UUID branchId
    ) {
        authorizationService.requirePermission(
                tenantContext,
                OrganizationPermissions.BRANCH_READ
        );

        Branch branch = branchRepository
                .findByIdAndTenantId(
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