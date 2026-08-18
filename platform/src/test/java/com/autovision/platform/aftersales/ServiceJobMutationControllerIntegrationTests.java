package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceJobMutationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceJobCommandService commandService;

    private final UUID userRefId =
            UUID.fromString(
                    "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
            );

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    userRefId,
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void markReadyRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/mark-ready",
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
                .with(csrf())
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void markReadyReturnsReadyJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceJob job =
                newJob(
                        orderId,
                        jobId,
                        ServiceJobApprovalStatus.NOT_REQUIRED
                );

        job.markReady(
                new DefaultServiceLifecyclePolicy(),
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.markReady(
                context,
                orderId,
                jobId
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/mark-ready",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(jobId.toString())
        )
        .andExpect(
                jsonPath("$.serviceOrderId")
                        .value(orderId.toString())
        )
        .andExpect(
                jsonPath("$.status")
                        .value("READY")
        )
        .andExpect(
                jsonPath("$.approvalStatus")
                        .value("NOT_REQUIRED")
        );

        verify(commandService)
                .markReady(
                        context,
                        orderId,
                        jobId
                );
    }

    @Test
    void startReturnsInProgressJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceLifecyclePolicy policy =
                new DefaultServiceLifecyclePolicy();

        ServiceJob job =
                newJob(
                        orderId,
                        jobId,
                        ServiceJobApprovalStatus.NOT_REQUIRED
                );

        job.markReady(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.start(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId,
                jobId
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/start",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("IN_PROGRESS")
        );
    }

    @Test
    void completeWorkReturnsCompletedJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceLifecyclePolicy policy =
                new DefaultServiceLifecyclePolicy();

        ServiceJob job =
                newJob(
                        orderId,
                        jobId,
                        ServiceJobApprovalStatus.NOT_REQUIRED
                );

        job.markReady(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.start(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.completeWork(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.completeWork(
                context,
                orderId,
                jobId
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/complete-work",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("WORK_COMPLETED")
        );
    }

    @Test
    void closeReturnsClosedJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceLifecyclePolicy policy =
                new DefaultServiceLifecyclePolicy();

        ServiceJob job =
                newJob(
                        orderId,
                        jobId,
                        ServiceJobApprovalStatus.NOT_REQUIRED
                );

        job.markReady(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.start(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.completeWork(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        job.close(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.close(
                context,
                orderId,
                jobId
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/close",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("CLOSED")
        );
    }

    @Test
    void cancelReturnsCancelledJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceJob job =
                newJob(
                        orderId,
                        jobId,
                        ServiceJobApprovalStatus.NOT_REQUIRED
                );

        job.cancel(
                new DefaultServiceLifecyclePolicy(),
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.cancel(
                context,
                orderId,
                jobId
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/cancel",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("CANCELLED")
        );
    }

    @Test
    void mutationRejectsAuthenticatedRequestWithoutAuthorizedScope()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.markReady(
                context,
                orderId,
                jobId
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/mark-ready",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mutationPropagatesNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId,
                jobId
        )).thenThrow(
                new ResponseStatusException(
                        NOT_FOUND,
                        "Service job not found"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/start",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void invalidJobLifecycleTransitionReturnsConflict()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId,
                jobId
        )).thenThrow(
                new IllegalStateException(
                        "Service job transition is not allowed"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/start",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isConflict());
    }

    @Test
    void pendingApprovalMarkReadyReturnsConflict()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.markReady(
                context,
                orderId,
                jobId
        )).thenThrow(
                new IllegalStateException(
                        "Service job approval requirements are not satisfied"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/mark-ready",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isConflict());
    }

    @Test
    void mutationRejectsMalformedOrderId()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/start",
                        "not-a-uuid",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void mutationRejectsMalformedJobId()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/start",
                        UUID.randomUUID(),
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    private ServiceJob newJob(
            UUID orderId,
            UUID jobId,
            ServiceJobApprovalStatus approvalStatus
    ) {
        return ServiceJob.open(
                jobId,
                orderId,
                "JOB-" + jobId,
                "Service job mutation API test",
                approvalStatus,
                userRefId,
                OffsetDateTime.now()
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor
            authenticatedJwt() {

        return jwt().jwt(token -> token
                .subject("test-subject")
                .claim(
                        "autovision_user_ref_id",
                        userRefId.toString()
                ));
    }
}