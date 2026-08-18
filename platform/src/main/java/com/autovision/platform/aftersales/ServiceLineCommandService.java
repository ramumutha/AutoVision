package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceLineCommandService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceJobRepository jobRepository;
    private final ServiceLineRepository lineRepository;
    private final AuthorizationService authorizationService;

    public ServiceLineCommandService(
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

    @Transactional
    public ServiceLine create(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID serviceJobId,
            int lineNumber,
            ServiceLineType lineType,
            String description,
            BigDecimal quantity,
            String unitOfMeasure
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        if (
                lineRepository.existsByServiceOrderIdAndLineNumber(
                        order.getId(),
                        lineNumber
                )
        ) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Service line number already exists"
            );
        }

        if (serviceJobId != null) {
            requireContainedJob(
                    order,
                    serviceJobId
            );
        }

        ServiceLine line =
                ServiceLine.create(
                        UUID.randomUUID(),
                        order.getId(),
                        serviceJobId,
                        lineNumber,
                        lineType,
                        description,
                        quantity,
                        unitOfMeasure,
                        tenantContext.userRefId(),
                        OffsetDateTime.now()
                );

        return lineRepository.save(line);
    }

    @Transactional
    public ServiceLine updateDetails(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID lineId,
            ServiceLineType lineType,
            String description,
            BigDecimal quantity,
            String unitOfMeasure
    ) {
        ServiceLine line =
                requireMutableLine(
                        tenantContext,
                        orderId,
                        lineId
                );

        line.updateDetails(
                lineType,
                description,
                quantity,
                unitOfMeasure,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return line;
    }

    @Transactional
    public ServiceLine assignToJob(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID lineId,
            UUID serviceJobId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        ServiceLine line =
                lineRepository.findByIdAndServiceOrderId(
                        lineId,
                        order.getId()
                ).orElseThrow(() -> notFound(
                        "Service line not found"
                ));

        ServiceJob job =
                requireContainedJob(
                        order,
                        serviceJobId
                );

        line.assignToJob(
                job.getId(),
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return line;
    }

    @Transactional
    public ServiceLine unassignFromJob(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID lineId
    ) {
        ServiceLine line =
                requireMutableLine(
                        tenantContext,
                        orderId,
                        lineId
                );

        line.unassignFromJob(
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return line;
    }

    private ServiceLine requireMutableLine(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID lineId
    ) {
        ServiceOrder order =
                requireMutableOrder(
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

    private ServiceOrder requireMutableOrder(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        AfterSalesPermissions.SERVICE_ORDER_UPDATE,
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

    private ServiceJob requireContainedJob(
            ServiceOrder order,
            UUID serviceJobId
    ) {
        if (serviceJobId == null) {
            throw new IllegalArgumentException(
                    "Service job ID is required"
            );
        }

        return jobRepository.findByIdAndServiceOrderId(
                serviceJobId,
                order.getId()
        ).orElseThrow(() -> notFound(
                "Service job not found"
        ));
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