package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class OperationalAuthorizationEvaluationService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceJobRepository jobRepository;
    private final ServiceLineRepository lineRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final ServiceQuoteLineRepository quoteLineRepository;
    private final CustomerAuthorizationRepository authorizationRepository;
    private final AuthorizationService authorizationService;

    public OperationalAuthorizationEvaluationService(
            ServiceOrderRepository orderRepository,
            ServiceJobRepository jobRepository,
            ServiceLineRepository lineRepository,
            ServiceQuoteRepository quoteRepository,
            ServiceQuoteLineRepository quoteLineRepository,
            CustomerAuthorizationRepository authorizationRepository,
            AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
        this.lineRepository = lineRepository;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.authorizationRepository = authorizationRepository;
        this.authorizationService = authorizationService;
    }

    public OperationalAuthorizationEvaluation evaluate(
            AuthenticatedTenantContext context,
            UUID serviceOrderId,
            UUID serviceJobId
    ) {
        requireValue(context, "Tenant context is required");
        requireValue(serviceOrderId, "Service order ID is required");
        requireValue(serviceJobId, "Service job ID is required");

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.SERVICE_ORDER_READ,
                        AuthorizationResourceType.SERVICE_ORDER,
                        serviceOrderId
                )
        );

        ServiceOrder order = orderRepository.findByIdAndTenantId(
                serviceOrderId,
                context.tenantId()
        ).orElseThrow(() -> notFound("Service order not found"));

        ServiceJob job = jobRepository.findByIdAndServiceOrderId(
                serviceJobId,
                order.getId()
        ).orElseThrow(() -> notFound("Service job not found"));

        List<ServiceLine> currentLines =
                lineRepository.findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
                        order.getId(),
                        job.getId()
                );

        if (job.getApprovalStatus() == ServiceJobApprovalStatus.NOT_REQUIRED) {
            return evaluation(
                    job,
                    order,
                    OperationalAuthorizationStatus.NOT_REQUIRED,
                    currentLines.size(),
                    0,
                    0,
                    0
            );
        }

        if (currentLines.isEmpty()) {
            return evaluation(
                    job,
                    order,
                    OperationalAuthorizationStatus.NOT_AUTHORIZED,
                    0,
                    0,
                    0,
                    0
            );
        }

        List<ServiceQuote> quotes =
                quoteRepository.findByServiceOrderIdAndTenantIdOrderByCreatedAtAsc(
                        order.getId(),
                        context.tenantId()
                );
        Set<UUID> quoteIds = quotes.stream()
                .map(quote -> quote.getId())
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));

        if (quoteIds.isEmpty()) {
            return evaluateLines(job, order, currentLines, Map.of());
        }

        List<CustomerAuthorization> authorizations =
                authorizationRepository.findAllByTenantIdAndServiceQuoteIdIn(
                        context.tenantId(),
                        List.copyOf(quoteIds)
                );
        List<ServiceQuoteLine> quoteLines =
                quoteLineRepository
                        .findAllByServiceQuoteIdInOrderByServiceQuoteIdAscSequenceAsc(
                                List.copyOf(quoteIds)
                        );

        Map<UUID, Integer> evidenceRankByServiceLine = new HashMap<>();
        Map<UUID, UUID> quoteIdByAuthorizationId = authorizations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        authorization -> authorization.getId(),
                        authorization -> authorization.getServiceQuoteId()
                ));
        for (ServiceQuoteLine quoteLine : quoteLines) {
            for (CustomerAuthorization authorization : authorizations) {
                if (!quoteLine.getServiceQuoteId().equals(
                        quoteIdByAuthorizationId.get(authorization.getId())
                )) {
                    continue;
                }
                int rank = rank(authorization.getAuthorizationStatus());
                evidenceRankByServiceLine.merge(
                        quoteLine.getServiceLineId(),
                        rank,
                        (existing, candidate) -> Math.max(existing, candidate)
                );
            }
        }

        return evaluateLines(job, order, currentLines, evidenceRankByServiceLine);
    }

    private OperationalAuthorizationEvaluation evaluateLines(
            ServiceJob job,
            ServiceOrder order,
            List<ServiceLine> currentLines,
            Map<UUID, Integer> evidenceRankByServiceLine
    ) {
        int authorized = 0;
        int pending = 0;
        for (ServiceLine line : currentLines) {
            int rank = evidenceRankByServiceLine.getOrDefault(
                    line.getId(),
                    0
            );
            if (rank == 3) {
                authorized++;
            } else if (rank == 2) {
                pending++;
            }
        }

        int notAuthorized = currentLines.size() - authorized - pending;
        OperationalAuthorizationStatus status;
        if (authorized == currentLines.size()) {
            status = OperationalAuthorizationStatus.FULLY_AUTHORIZED;
        } else if (authorized > 0) {
            status = OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED;
        } else if (pending > 0) {
            status = OperationalAuthorizationStatus.PENDING;
        } else {
            status = OperationalAuthorizationStatus.NOT_AUTHORIZED;
        }

        return evaluation(
                job,
                order,
                status,
                currentLines.size(),
                authorized,
                pending,
                notAuthorized
        );
    }

    private static int rank(CustomerAuthorizationStatus status) {
        return switch (status) {
            case AUTHORIZED -> 3;
            case REQUESTED, DEFERRED -> 2;
            case DECLINED, CANCELLED -> 1;
        };
    }

    private static OperationalAuthorizationEvaluation evaluation(
            ServiceJob job,
            ServiceOrder order,
            OperationalAuthorizationStatus status,
            int total,
            int authorized,
            int pending,
            int notAuthorized
    ) {
        return new OperationalAuthorizationEvaluation(
                job.getId(),
                order.getId(),
                status,
                total,
                authorized,
                pending,
                notAuthorized
        );
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
