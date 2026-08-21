package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log =
            LoggerFactory.getLogger(CustomerAuthorizationService.class);

    private final CustomerAuthorizationRepository repository;
    private final AfterSalesCaseRepository caseRepository;
        private final ServiceQuoteRepository quoteRepository;
        private final ServiceQuoteLineRepository quoteLineRepository;
        private final ServiceQuoteAuthorizationSnapshotFactory snapshotFactory;
    private final AuthorizationService authorizationService;

    public CustomerAuthorizationService(
            CustomerAuthorizationRepository repository,
            AfterSalesCaseRepository caseRepository,
            ServiceQuoteRepository quoteRepository,
            ServiceQuoteLineRepository quoteLineRepository,
            ServiceQuoteAuthorizationSnapshotFactory snapshotFactory,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.caseRepository = caseRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.snapshotFactory = snapshotFactory;
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

    public List<CustomerAuthorization> findAllForServiceQuote(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID quoteId
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ
        );

        requireCase(tenantContext, caseId);
        requireQuoteForCase(tenantContext, caseId, quoteId);

        return repository
                .findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
                        tenantContext.tenantId(),
                        quoteId
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
        return request(
                tenantContext,
                caseId,
                null,
                authorizationNumber,
                customerReference,
                customerDisplayNameSnapshot,
                authorizationSummary,
                authorizationScopeSnapshot,
                commercialSnapshot,
                termsSnapshot,
                disclaimerSnapshot
        );
    }

    @Transactional
    public CustomerAuthorization request(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID serviceQuoteId,
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

        if (serviceQuoteId != null) {
            ServiceQuote serviceQuote = quoteRepository.findByIdAndTenantId(
                    serviceQuoteId,
                    tenantContext.tenantId()
            ).orElseThrow(() -> new ResponseStatusException(
                    NOT_FOUND,
                    "Service quote not found"
            ));

            if (serviceQuote.getAfterSalesCaseId() == null
                    || !caseId.equals(serviceQuote.getAfterSalesCaseId())) {
                throw new ResponseStatusException(
                        NOT_FOUND,
                        "Service quote not found"
                );
            }
        }

        OffsetDateTime now = OffsetDateTime.now();

        CustomerAuthorization authorization =
                CustomerAuthorization.request(
                        UUID.randomUUID(),
                        tenantContext.tenantId(),
                        afterSalesCase.getDealerId(),
                        afterSalesCase.getBranchId(),
                        afterSalesCase.getId(),
                        serviceQuoteId,
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

        CustomerAuthorization saved =
                repository.save(authorization);

        logLifecycleEvent(
                "customer_authorization.requested",
                saved,
                tenantContext,
                null
        );

        return saved;
    }

    @Transactional
    public CustomerAuthorization requestFromServiceQuote(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID quoteId,
            String authorizationNumber,
            String customerReference,
            String customerDisplayNameSnapshot,
            String authorizationSummary
    ) {
        requireCasePermission(
                tenantContext,
                caseId,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_CREATE
        );

        AfterSalesCase afterSalesCase = requireCase(tenantContext, caseId);
        ServiceQuote quote = requireQuoteForCase(
                tenantContext,
                caseId,
                quoteId
        );

        if (quote.getStatus() != ServiceQuoteStatus.ISSUED) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Service quote is not eligible for authorization"
            );
        }

        if (repository.existsByTenantIdAndServiceQuoteIdAndAuthorizationStatus(
                tenantContext.tenantId(),
                quoteId,
                CustomerAuthorizationStatus.REQUESTED
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "A customer authorization request already exists for this service quote"
            );
        }

        ServiceQuoteAuthorizationSnapshotFactory.Snapshots snapshots =
                snapshotFactory.create(
                        quote,
                        quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(
                                quoteId
                        )
                );

        CustomerAuthorization authorization = CustomerAuthorization.request(
                UUID.randomUUID(),
                tenantContext.tenantId(),
                afterSalesCase.getDealerId(),
                afterSalesCase.getBranchId(),
                afterSalesCase.getId(),
                quoteId,
                authorizationNumber,
                customerReference,
                customerDisplayNameSnapshot,
                authorizationSummary,
                snapshots.authorizationScopeSnapshot(),
                snapshots.commercialSnapshot(),
                quote.getTermsSnapshot(),
                quote.getDisclaimerSnapshot(),
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        CustomerAuthorization saved = repository.save(authorization);
        logLifecycleEvent(
                "customer_authorization.requested",
                saved,
                tenantContext,
                null
        );
        return saved;
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

    private ServiceQuote requireQuoteForCase(
            AuthenticatedTenantContext tenantContext,
            UUID caseId,
            UUID quoteId
    ) {
        ServiceQuote quote = quoteRepository.findByIdAndTenantId(
                quoteId,
                tenantContext.tenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "Service quote not found"
        ));

        if (quote.getAfterSalesCaseId() == null
                || !caseId.equals(quote.getAfterSalesCaseId())) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Service quote not found"
            );
        }

        return quote;
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

        logLifecycleEvent(
                switch (targetStatus) {
                    case AUTHORIZED ->
                            "customer_authorization.authorized";
                    case DECLINED ->
                            "customer_authorization.declined";
                    case DEFERRED ->
                            "customer_authorization.deferred";
                    case CANCELLED ->
                            "customer_authorization.cancelled";
                    case REQUESTED ->
                            throw new IllegalArgumentException(
                                    "REQUESTED is not a decision state"
                            );
                },
                authorization,
                tenantContext,
                decisionChannel
        );
    }

    private void logLifecycleEvent(
            String event,
            CustomerAuthorization authorization,
            AuthenticatedTenantContext tenantContext,
            String decisionChannel
    ) {
        if (decisionChannel == null) {
            log.info(
                    "customer authorization lifecycle event={} tenantId={} dealerId={} branchId={} caseId={} authorizationId={} authorizationNumber={} principalId={} status={}",
                    event,
                    authorization.getTenantId(),
                    authorization.getDealerId(),
                    authorization.getBranchId(),
                    authorization.getAftersalesCaseId(),
                    authorization.getId(),
                    authorization.getAuthorizationNumber(),
                    tenantContext.userRefId(),
                    authorization.getAuthorizationStatus()
            );
        } else {
            log.info(
                    "customer authorization lifecycle event={} tenantId={} dealerId={} branchId={} caseId={} authorizationId={} authorizationNumber={} principalId={} status={} decisionChannel={}",
                    event,
                    authorization.getTenantId(),
                    authorization.getDealerId(),
                    authorization.getBranchId(),
                    authorization.getAftersalesCaseId(),
                    authorization.getId(),
                    authorization.getAuthorizationNumber(),
                    tenantContext.userRefId(),
                    authorization.getAuthorizationStatus(),
                    decisionChannel
            );
        }
    }
}
