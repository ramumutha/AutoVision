package com.autovision.platform.aftersales;

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

    public AfterSalesCaseService(
            AfterSalesCaseRepository repository
    ) {
        this.repository = repository;
    }

    public List<AfterSalesCase> findAll(
            AuthenticatedTenantContext tenantContext
    ) {
        return repository.findAllByTenantId(
                tenantContext.tenantId()
        );
    }

    public AfterSalesCase findById(
            AuthenticatedTenantContext tenantContext,
            UUID caseId
    ) {
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
        AfterSalesCase afterSalesCase =
                findById(tenantContext, caseId);

        afterSalesCase.close(
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return afterSalesCase;
    }
}