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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceOrderCommandService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceJobRepository jobRepository;
    private final ServiceLineRepository lineRepository;
    private final AuthorizationService authorizationService;
    private final ServiceLifecyclePolicy lifecyclePolicy;
    private final ServiceOrderAggregateLifecycle aggregateLifecycle;
    private final ServiceOrderAggregatePolicy aggregatePolicy;

    public ServiceOrderCommandService(
            ServiceOrderRepository orderRepository,
            ServiceJobRepository jobRepository,
            ServiceLineRepository lineRepository,
            AuthorizationService authorizationService,
            ServiceLifecyclePolicy lifecyclePolicy,
            ServiceOrderAggregateLifecycle aggregateLifecycle,
            ServiceOrderAggregatePolicy aggregatePolicy
    ) {
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
        this.lineRepository = lineRepository;
        this.authorizationService = authorizationService;
        this.lifecyclePolicy = lifecyclePolicy;
        this.aggregateLifecycle = aggregateLifecycle;
        this.aggregatePolicy = aggregatePolicy;
    }

    @Transactional
    public ServiceOrder create(
            AuthenticatedTenantContext tenantContext,
            String orderNumber,
            UUID dealerId,
            UUID branchId,
            UUID vehicleId
    ) {
        if (branchId != null && dealerId == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "dealerId is required when branchId is provided"
            );
        }

        requireCreatePermission(
                tenantContext,
                dealerId,
                branchId
        );

        if (orderRepository.existsByTenantIdAndOrderNumber(
                tenantContext.tenantId(),
                orderNumber
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Service order number already exists"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        ServiceOrder serviceOrder = ServiceOrder.open(
                UUID.randomUUID(),
                tenantContext.tenantId(),
                dealerId,
                branchId,
                orderNumber,
                vehicleId,
                tenantContext.userRefId(),
                now
        );

        return orderRepository.save(serviceOrder);
    }

    @Transactional
    public ServiceOrder start(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        order.start(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
    }

    @Transactional
    public ServiceOrder cancel(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        order.cancel(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
    }

    @Transactional
    public ServiceOrder completeWork(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
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

        aggregateLifecycle.completeWork(
                order,
                jobs,
                lines,
                lifecyclePolicy,
                aggregatePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
    }

    @Transactional
    public ServiceOrder close(
            AuthenticatedTenantContext tenantContext,
            UUID orderId
    ) {
        ServiceOrder order =
                requireMutableOrder(
                        tenantContext,
                        orderId
                );

        List<ServiceJob> jobs =
                jobRepository
                        .findAllByServiceOrderIdOrderByJobNumber(
                                order.getId()
                        );

        aggregateLifecycle.close(
                order,
                jobs,
                lifecyclePolicy,
                aggregatePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return order;
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
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "Service order not found"
        ));
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
                                                AfterSalesPermissions.SERVICE_ORDER_CREATE,
                                                resourceType,
                                                resourceId
                                )
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
}