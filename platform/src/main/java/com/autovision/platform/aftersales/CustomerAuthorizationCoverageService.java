package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class CustomerAuthorizationCoverageService {

    private final CustomerAuthorizationRepository authorizationRepository;
    private final AfterSalesCaseRepository caseRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final ServiceQuoteLineRepository quoteLineRepository;
    private final AuthorizationService authorizationService;

    public CustomerAuthorizationCoverageService(
            CustomerAuthorizationRepository authorizationRepository,
            AfterSalesCaseRepository caseRepository,
            ServiceQuoteRepository quoteRepository,
            ServiceQuoteLineRepository quoteLineRepository,
            AuthorizationService authorizationService
    ) {
        this.authorizationRepository = authorizationRepository;
        this.caseRepository = caseRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.authorizationService = authorizationService;
    }

    public CustomerAuthorizationCoverage resolve(
            AuthenticatedTenantContext context,
            UUID caseId,
            UUID authorizationId
    ) {
        requireValue(context, "Tenant context is required");
        requireValue(caseId, "AfterSales case ID is required");
        requireValue(authorizationId, "Customer authorization ID is required");

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );

        requireCase(context, caseId);

        CustomerAuthorization authorization =
                authorizationRepository.findByIdAndTenantId(
                        authorizationId,
                        context.tenantId()
                ).orElseThrow(() -> notFound(
                        "Customer authorization not found"
                ));

        if (!caseId.equals(authorization.getAftersalesCaseId())) {
            throw notFound("Customer authorization not found");
        }

        UUID quoteId = authorization.getServiceQuoteId();
        if (quoteId == null) {
            throw notFound("Customer authorization quote not found");
        }

        ServiceQuote quote = quoteRepository.findByIdAndTenantId(
                quoteId,
                context.tenantId()
        ).orElseThrow(() -> notFound("Service quote not found"));

        if (quote.getAfterSalesCaseId() == null
                || !caseId.equals(quote.getAfterSalesCaseId())) {
            throw notFound("Service quote not found");
        }

        List<ServiceQuoteLine> quoteLines =
                quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(
                        quoteId
                );

        List<UUID> serviceLineIds = new ArrayList<>(quoteLines.size());
        LinkedHashSet<UUID> serviceJobIds = new LinkedHashSet<>();
        for (ServiceQuoteLine quoteLine : quoteLines) {
            serviceLineIds.add(quoteLine.getServiceLineId());
            if (quoteLine.getServiceJobId() != null) {
                serviceJobIds.add(quoteLine.getServiceJobId());
            }
        }

        return new CustomerAuthorizationCoverage(
                authorization.getId(),
                quote.getId(),
                quote.getServiceOrderId(),
                serviceLineIds,
                new ArrayList<>(serviceJobIds)
        );
    }

    private AfterSalesCase requireCase(
            AuthenticatedTenantContext context,
            UUID caseId
    ) {
        return caseRepository.findByIdAndTenantId(
                caseId,
                context.tenantId()
        ).orElseThrow(() -> notFound("AfterSales case not found"));
    }

    private static void requireValue(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}
