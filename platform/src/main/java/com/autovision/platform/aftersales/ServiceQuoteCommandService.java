package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceQuoteCommandService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceLineRepository lineRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final ServiceQuoteLineRepository quoteLineRepository;
    private final AuthorizationService authorizationService;
    private final ServiceQuoteLineEligibilityPolicy eligibilityPolicy;

    public ServiceQuoteCommandService(
            ServiceOrderRepository orderRepository,
            ServiceLineRepository lineRepository,
            ServiceQuoteRepository quoteRepository,
            ServiceQuoteLineRepository quoteLineRepository,
            AuthorizationService authorizationService,
            ServiceQuoteLineEligibilityPolicy eligibilityPolicy
    ) {
        this.orderRepository = orderRepository;
        this.lineRepository = lineRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.authorizationService = authorizationService;
        this.eligibilityPolicy = eligibilityPolicy;
    }

    @Transactional
    public ServiceQuote create(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId,
            String quoteNumber,
            String currencyCode,
            OffsetDateTime validUntil,
            String termsSnapshot,
            String disclaimerSnapshot
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.SERVICE_ORDER_UPDATE,
                        AuthorizationResourceType.SERVICE_ORDER,
                        serviceOrderId
                )
        );

        ServiceOrder order = orderRepository.findByIdAndTenantId(
                serviceOrderId,
                tenantContext.tenantId()
        ).orElseThrow(() -> notFound("Service order not found"));

        if (quoteRepository.existsByTenantIdAndQuoteNumber(
                order.getTenantId(), quoteNumber
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Quote number already exists"
            );
        }

        List<ServiceLine> eligibleLines = resolveEligibleLines(
                order,
                currencyCode
        );

        if (eligibleLines.isEmpty()) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "No eligible service lines available for quotation"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        ServiceQuote quote = ServiceQuote.create(
                UUID.randomUUID(),
                order.getTenantId(),
                order.getDealerId(),
                order.getBranchId(),
                null,
                order.getId(),
                quoteNumber,
                currencyCode,
                validUntil,
                termsSnapshot,
                disclaimerSnapshot,
                tenantContext.userRefId(),
                now
        );

        quoteRepository.save(quote);

        List<ServiceQuoteLine> quoteLines = new ArrayList<>(eligibleLines.size());
        for (int sequence = 0; sequence < eligibleLines.size(); sequence++) {
            ServiceLine line = eligibleLines.get(sequence);
            quoteLines.add(ServiceQuoteLine.create(
                    UUID.randomUUID(),
                    quote.getId(),
                    line.getId(),
                    line.getServiceJobId(),
                    line.getDescription(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getCurrencyCode(),
                    line.getNetAmount(),
                    line.getTaxAmount(),
                    line.getGrossAmount(),
                    sequence,
                    tenantContext.userRefId(),
                    now
            ));
        }
        quoteLineRepository.saveAll(quoteLines);

        return quote;
    }

    private List<ServiceLine> resolveEligibleLines(
            ServiceOrder order,
            String currencyCode
    ) {
        List<ServiceLine> lines = lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(order.getId());

        return lines.stream()
                .filter(ServiceLine::hasCommercialSnapshot)
                .filter(line -> currencyCode.equals(line.getCurrencyCode()))
                .filter(line -> quoteLineRepository
                        .findStatusesByServiceLineIdAndServiceOrderIdAndTenantId(
                                line.getId(), order.getId(), order.getTenantId()
                        )
                        .stream()
                        .noneMatch(eligibilityPolicy::blocksRequotation))
                .toList();
    }

    private void requireTenantContext(
            AuthenticatedTenantContext tenantContext
    ) {
        if (tenantContext == null || tenantContext.tenantId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated tenant context is required"
            );
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}