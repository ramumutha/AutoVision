package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceQuoteReadService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final ServiceQuoteLineRepository quoteLineRepository;
    private final AuthorizationService authorizationService;

    public ServiceQuoteReadService(
            ServiceOrderRepository orderRepository,
            ServiceQuoteRepository quoteRepository,
            ServiceQuoteLineRepository quoteLineRepository,
            AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.authorizationService = authorizationService;
    }

    public List<ServiceQuote> listForServiceOrder(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId
    ) {
        ServiceOrder order = requireOrder(tenantContext, serviceOrderId);
        return quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                order.getId(), tenantContext.tenantId());
    }

    public ServiceQuote getForServiceOrder(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId,
            UUID quoteId
    ) {
        ServiceOrder order = requireOrder(tenantContext, serviceOrderId);
        ServiceQuote quote = quoteRepository.findByIdAndTenantId(
                quoteId, tenantContext.tenantId()
        ).orElseThrow(() -> notFound("Service quote not found"));

        if (!order.getId().equals(quote.getServiceOrderId())) {
            throw notFound("Service quote not found");
        }
        return quote;
    }

    public List<ServiceQuoteLine> linesFor(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId,
            UUID quoteId
    ) {
        ServiceQuote quote = getForServiceOrder(tenantContext, serviceOrderId, quoteId);
        return quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quote.getId());
    }

    private ServiceOrder requireOrder(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId
    ) {
        if (tenantContext == null || tenantContext.tenantId() == null) {
            throw new IllegalArgumentException("Authenticated tenant context is required");
        }
        authorizationService.requirePermission(new AuthorizationRequest(
                tenantContext,
                AfterSalesPermissions.SERVICE_ORDER_READ,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));
        return orderRepository.findByIdAndTenantId(
                serviceOrderId, tenantContext.tenantId()
        ).orElseThrow(() -> notFound("Service order not found"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}