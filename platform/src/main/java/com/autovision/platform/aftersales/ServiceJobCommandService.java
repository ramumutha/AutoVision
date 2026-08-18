package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceJobCommandService {

    private final ServiceOrderRepository orderRepository;
    private final ServiceJobRepository jobRepository;
    private final AuthorizationService authorizationService;
    private final ServiceLifecyclePolicy lifecyclePolicy;

    public ServiceJobCommandService(
            ServiceOrderRepository orderRepository,
            ServiceJobRepository jobRepository,
            AuthorizationService authorizationService,
            ServiceLifecyclePolicy lifecyclePolicy
    ) {
        this.orderRepository = orderRepository;
        this.jobRepository = jobRepository;
        this.authorizationService = authorizationService;
        this.lifecyclePolicy = lifecyclePolicy;
    }

    @Transactional
    public ServiceJob create(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            String jobNumber,
            String summary,
            ServiceJobApprovalStatus approvalStatus
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

        ServiceOrder order =
                orderRepository.findByIdAndTenantId(
                        orderId,
                        tenantContext.tenantId()
                ).orElseThrow(() -> notFound(
                        "Service order not found"
                ));

        if (jobRepository.existsByServiceOrderIdAndJobNumber(
                order.getId(),
                jobNumber
        )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Service job number already exists"
            );
        }

        ServiceJob job = ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                jobNumber,
                summary,
                approvalStatus,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return jobRepository.save(job);
    }

    @Transactional
    public ServiceJob markReady(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceJob job =
                requireMutableJob(
                        tenantContext,
                        orderId,
                        jobId
                );

        job.markReady(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return job;
    }

    @Transactional
    public ServiceJob start(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceJob job =
                requireMutableJob(
                        tenantContext,
                        orderId,
                        jobId
                );

        job.start(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return job;
    }

    @Transactional
    public ServiceJob completeWork(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceJob job =
                requireMutableJob(
                        tenantContext,
                        orderId,
                        jobId
                );

        job.completeWork(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return job;
    }

    @Transactional
    public ServiceJob close(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceJob job =
                requireMutableJob(
                        tenantContext,
                        orderId,
                        jobId
                );

        job.close(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return job;
    }

    @Transactional
    public ServiceJob cancel(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
    ) {
        ServiceJob job =
                requireMutableJob(
                        tenantContext,
                        orderId,
                        jobId
                );

        job.cancel(
                lifecyclePolicy,
                tenantContext.userRefId(),
                OffsetDateTime.now()
        );

        return job;
    }

    private ServiceJob requireMutableJob(
            AuthenticatedTenantContext tenantContext,
            UUID orderId,
            UUID jobId
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

        ServiceOrder order =
                orderRepository.findByIdAndTenantId(
                        orderId,
                        tenantContext.tenantId()
                ).orElseThrow(() -> notFound(
                        "Service order not found"
                ));

        return jobRepository.findByIdAndServiceOrderId(
                jobId,
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