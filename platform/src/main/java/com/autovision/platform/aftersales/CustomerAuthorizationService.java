package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class CustomerAuthorizationService {

    private final CustomerAuthorizationRepository repository;
    private final AfterSalesCaseRepository caseRepository;
    private final AuthorizationService authorizationService;

    public CustomerAuthorizationService(
            CustomerAuthorizationRepository repository,
            AfterSalesCaseRepository caseRepository,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.caseRepository = caseRepository;
        this.authorizationService = authorizationService;
    }

    public List<CustomerAuthorization> findAllForCase(
            AuthenticatedTenantContext tenantContext,
            UUID caseId
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ
        );

        requireCase(
                tenantContext,
                caseId
        );

        return repository.findAllByTenantIdAndAftersalesCaseId(
                tenantContext.tenantId(),
                caseId
        );
    }

    public CustomerAuthorization findById(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ
        );

        requireCase(
                tenantContext,
                caseId
        );

        return requireAuthorization(
                tenantContext,
                caseId,
                authorizationId
        );
    }

    @Transactional
    public CustomerAuthorization request(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            String authorizationNumber,
            String customerReference,
            String customerDisplayNameSnapshot,
            String authorizationSummary,
            JsonNode authorizationScopeSnapshot,
            JsonNode commercialSnapshot,
            String termsSnapshot,
            String disclaimerSnapshot
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_CREATE
        );

        AfterSalesCase afterSalesCase =
                requireCase(
                        tenantContext,
                        caseId
                );

        if (repository.existsByTenantIdAndAuthorizationNumber(
                tenantContext.tenantId(),
                authorizationNumber
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Customer authorization number already exists"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        CustomerAuthorization authorization =
                CustomerAuthorization.request(
                        UUID.randomUUID(),
                        tenantContext.tenantId(),
                        afterSalesCase.getDealerId(),
                        afterSalesCase.getBranchId(),
                        afterSalesCase.getId(),
                        authorizationNumber,
                        customerReference,
                        customerDisplayNameSnapshot,
                        authorizationSummary,
                        authorizationScopeSnapshot,
                        commercialSnapshot,
                        termsSnapshot,
                        disclaimerSnapshot,
                        tenantContext.userRefId(),
                        now
                );

        return repository.save(authorization);
    }

    @Transactional
    public CustomerAuthorization authorize(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId,
            String decisionChannel,
            String decisionReference
    ) {
        CustomerAuthorization authorization =
                requireForDecision(
                        tenantContext,
                        caseId,
                        authorizationId
                );

        decide(
                authorization,
                CustomerAuthorizationStatus.AUTHORIZED,
                decisionChannel,
                decisionReference,
                tenantContext
        );

        return authorization;
    }

    @Transactional
    public CustomerAuthorization decline(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId,
            String decisionChannel,
            String decisionReference
    ) {
        CustomerAuthorization authorization =
                requireForDecision(
                        tenantContext,
                        caseId,
                        authorizationId
                );

        decide(
                authorization,
                CustomerAuthorizationStatus.DECLINED,
                decisionChannel,
                decisionReference,
                tenantContext
        );

        return authorization;
    }

    @Transactional
    public CustomerAuthorization defer(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId,
            String decisionChannel,
            String decisionReference
    ) {
        CustomerAuthorization authorization =
                requireForDecision(
                        tenantContext,
                        caseId,
                        authorizationId
                );

        decide(
                authorization,
                CustomerAuthorizationStatus.DEFERRED,
                decisionChannel,
                decisionReference,
                tenantContext
        );

        return authorization;
    }

    @Transactional
    public CustomerAuthorization cancel(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId,
            String decisionChannel,
            String decisionReference
    ) {
        CustomerAuthorization authorization =
                requireForDecision(
                        tenantContext,
                        caseId,
                        authorizationId
                );

        decide(
                authorization,
                CustomerAuthorizationStatus.CANCELLED,
                decisionChannel,
                decisionReference,
                tenantContext
        );

        return authorization;
    }

    private CustomerAuthorization requireForDecision(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_DECIDE
        );

        requireCase(
                tenantContext,
                caseId
        );

        return requireAuthorization(
                tenantContext,
                caseId,
                authorizationId
        );
    }

    private AfterSalesCase requireCase(
            AuthenticatedTenantContext tenantContext,
            UUID caseId
    ) {
        return caseRepository.findByIdAndTenantId(
                caseId,
                tenantContext.tenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "AfterSales case not found"
        ));
    }

    private CustomerAuthorization requireAuthorization(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID authorizationId
    ) {
        CustomerAuthorization authorization =
                repository.findByIdAndTenantId(
                        authorizationId,
                        tenantContext.tenantId()
                ).orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Customer authorization not found"
                ));

        if (!caseId.equals(authorization.getAftersalesCaseId())) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Customer authorization not found"
            );
        }

        return authorization;
    }

    private void requireCasePermission(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            String permissionCode
    ) {
        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        permissionCode,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    private void decide(
            CustomerAuthorization authorization,
            CustomerAuthorizationStatus targetStatus,
            String decisionChannel,
            String decisionReference,
            AuthenticatedTenantContext tenantContext
    ) {
        try {
            switch (targetStatus) {
                case AUTHORIZED -> authorization.authorize(
                        decisionChannel,
                        decisionReference,
                        tenantContext.userRefId(),
                        OffsetDateTime.now()
                );
                case DECLINED -> authorization.decline(
                        decisionChannel,
                        decisionReference,
                        tenantContext.userRefId(),
                        OffsetDateTime.now()
                );
                case DEFERRED -> authorization.defer(
                        decisionChannel,
                        decisionReference,
                        tenantContext.userRefId(),
                        OffsetDateTime.now()
                );
                case CANCELLED -> authorization.cancel(
                        decisionChannel,
                        decisionReference,
                        tenantContext.userRefId(),
                        OffsetDateTime.now()
                );
                case REQUESTED -> throw new IllegalArgumentException(
                        "REQUESTED is not a decision state"
                );
            }
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    CONFLICT,
                    exception.getMessage(),
                    exception
            );
        }
    }
}
