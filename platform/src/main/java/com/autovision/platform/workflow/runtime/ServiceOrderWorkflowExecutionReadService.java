package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.AfterSalesPermissions;
import com.autovision.platform.aftersales.ServiceOrderRepository;
import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceOrderWorkflowExecutionReadService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceOrderWorkflowExecutionRepository executionRepository;
    private final AuthorizationService authorizationService;

    public ServiceOrderWorkflowExecutionReadService(
            ServiceOrderRepository orderRepository,
            ServiceOrderWorkflowExecutionRepository executionRepository,
            AuthorizationService authorizationService
    ) {
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.authorizationService = authorizationService;
    }

    public ServiceOrderWorkflowExecution getForServiceOrder(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId
    ) {
        requireInputs(tenantContext, serviceOrderId);

        authorizationService.requirePermission(new AuthorizationRequest(
                tenantContext,
                AfterSalesPermissions.SERVICE_ORDER_READ,
                AuthorizationResourceType.SERVICE_ORDER,
                serviceOrderId
        ));

        orderRepository.findByIdAndTenantId(serviceOrderId, tenantContext.tenantId())
                .orElseThrow(() -> notFound("Service order not found"));

        return executionRepository.findByServiceOrderIdAndTenantId(
                        serviceOrderId, tenantContext.tenantId())
                .orElseThrow(() -> notFound(
                        "Service order workflow execution not found"));
    }

    private void requireInputs(
            AuthenticatedTenantContext tenantContext,
            UUID serviceOrderId
    ) {
        if (tenantContext == null || tenantContext.tenantId() == null
                || tenantContext.userRefId() == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Authenticated tenant context is required");
        }
        if (serviceOrderId == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Service order ID is required");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }
}