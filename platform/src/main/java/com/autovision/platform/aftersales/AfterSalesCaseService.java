package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class AfterSalesCaseService {

    private final AfterSalesCaseRepository repository;
    private final AuthorizationService authorizationService;

    public AfterSalesCaseService(
            AfterSalesCaseRepository repository,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public List<AfterSalesCase> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        authorizationService.requirePermission(
                tenantContext,
                AfterSalesPermissions.CASE_READ
        );

        return repository.findAllByTenantId(
                tenantContext.tenantId()
        );
    }

    public AfterSalesCase findById(
            AuthenticatedTenantContext tenantContext,
            UUID caseId
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.CASE_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );

        return repository.findByIdAndTenantId(
                caseId,
                tenantContext.tenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "AfterSales case not found"
        ));
    }

    @Transactional
    public AfterSalesCase open(
            AuthenticatedTenantContext tenantContext,
            String caseNumber,
            UUID dealerId,
            UUID branchId,
            AfterSalesCaseSourceChannel sourceChannel
    ) {
        requireCreatePermission(
                tenantContext,
                dealerId,
                branchId
        );

        if (repository.existsByTenantIdAndCaseNumber(
                tenantContext.tenantId(),
                caseNumber
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "AfterSales case number already exists"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        AfterSalesCase afterSalesCase = AfterSalesCase.open(
                UUID.randomUUID(),
                tenantContext.tenantId(),
                dealerId,
                branchId,
                caseNumber,
                sourceChannel,
                tenantContext.userRefId(),
                now
        );

        return repository.save(afterSalesCase);
    }

    @Transactional
    public AfterSalesCase close(
            AuthenticatedTenantContext tenantContext,
            UUID caseId
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.CASE_UPDATE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );

        AfterSalesCase afterSalesCase =
                repository.findByIdAndTenantId(
                        caseId,
                        tenantContext.tenantId()
                ).orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "AfterSales case not found"
                ));

        afterSalesCase.close(
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return afterSalesCase;
    }

    private void requireCreatePermission(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId,
            UUID branchId
    ) {
        AuthorizationResourceType resourceType;
        UUID resourceId;

        if (branchId != null) {
            resourceType = AuthorizationResourceType.BRANCH;
            resourceId = branchId;
        } else if (dealerId != null) {
            resourceType = AuthorizationResourceType.DEALER;
            resourceId = dealerId;
        } else {
            resourceType = AuthorizationResourceType.TENANT;
            resourceId = tenantContext.tenantId();
        }

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.CASE_CREATE,
                        resourceType,
                        resourceId
                )
        );
    }
}