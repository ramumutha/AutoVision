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
public class ServiceOrderAccessService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceJobRepository jobRepository;
    private final ServiceLineRepository lineRepository;
    private final AuthorizationService authorizationService;

    public ServiceOrderAccessService(
            ServiceOrderRepository orderRepository,
            ServiceJobRepository jobRepository,
            ServiceLineRepository lineRepository,
            AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
        this.lineRepository = lineRepository;
        this.authorizationService = authorizationService;
    }

    public ServiceOrder requireOrder(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.SERVICE_ORDER_READ,
                        AuthorizationResourceType.SERVICE_ORDER,
                        orderId
                )
        );

        return orderRepository.findByIdAndTenantId(
                orderId,
                tenantContext.tenantId()
        ).orElseThrow(() -> notFound(
                "Service order not found"
        ));
    }

    public List<ServiceJob> findJobs(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        return jobRepository
                .findAllByServiceOrderIdOrderByJobNumber(
                        order.getId()
                );
    }

    public ServiceJob requireJob(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        return jobRepository.findByIdAndServiceOrderId(
                jobId,
                order.getId()
        ).orElseThrow(() -> notFound(
                "Service job not found"
        ));
    }

    public List<ServiceLine> findLines(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        return lineRepository
                .findAllByServiceOrderIdOrderByLineNumber(
                        order.getId()
                );
    }

    public ServiceLine requireLine(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID lineId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        return lineRepository.findByIdAndServiceOrderId(
                lineId,
                order.getId()
        ).orElseThrow(() -> notFound(
                "Service line not found"
        ));
    }

    public List<ServiceLine> findLinesForJob(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        ServiceJob job =
                jobRepository.findByIdAndServiceOrderId(
                        jobId,
                        order.getId()
                ).orElseThrow(() -> notFound(
                        "Service job not found"
                ));

        return lineRepository
                .findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
                        order.getId(),
                        job.getId()
                );
    }

    public ServiceOrderAggregateView requireAggregate(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireOrder(
                        tenantContext,
                        orderId
                );

        List<ServiceJob> jobs =
                jobRepository
                        .findAllByServiceOrderIdOrderByJobNumber(
                                order.getId()
                        );

        List<ServiceLine> lines =
                lineRepository
                        .findAllByServiceOrderIdOrderByLineNumber(
                                order.getId()
                        );

        return new ServiceOrderAggregateView(
                order,
                jobs,
                lines
        );
    }

    private void requireTenantContext(
            AuthenticatedTenantContext tenantContext
    ) {
        if (
                tenantContext == null
                        || tenantContext.tenantId() == null
        ) {
            throw new IllegalArgumentException(
                    "Authenticated tenant context is required"
            );
        }
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                NOT_FOUND,
                message
        );
    }
}