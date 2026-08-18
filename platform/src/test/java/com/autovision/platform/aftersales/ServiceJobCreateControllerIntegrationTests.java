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
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceJobCreateControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createsServiceJobAndReturnsMutationResponse()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        String jobNumber = "JOB-REST-001";
        String summary = "Initial diagnostic work";
        ServiceJobApprovalStatus approvalStatus =
                ServiceJobApprovalStatus.PENDING;
        ServiceJob job = ServiceJob.open(
                jobId,
                orderId,
                jobNumber,
                summary,
                approvalStatus,
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderId,
                jobNumber,
                summary,
                approvalStatus
        )).thenReturn(job);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        orderId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(json(jobNumber, summary, approvalStatus))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(jobId.toString()))
        .andExpect(jsonPath("$.serviceOrderId")
                .value(orderId.toString()))
        .andExpect(jsonPath("$.jobNumber").value(jobNumber))
        .andExpect(jsonPath("$.summary").value(summary))
        .andExpect(jsonPath("$.approvalStatus")
                .value(approvalStatus.name()))
        .andExpect(jsonPath("$.status").value("OPEN"));

        verify(tenantContextResolver).resolve(any());
        verify(commandService).create(
                context,
                orderId,
                jobNumber,
                summary,
                approvalStatus
        );
    }

    @Test
    void rejectsUnauthenticatedRequestWithoutCreatingJob()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        UUID.randomUUID()
                )
                .with(csrf())
                .contentType("application/json")
                .content(json(
                        "JOB-UNAUTH-001",
                        "Unauthenticated job",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                ))
        )
        .andExpect(status().isUnauthorized());

        verify(commandService, never()).create(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void propagatesAuthorizationDenialAsForbidden()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        String jobNumber = "JOB-FORBIDDEN-001";
        String summary = "Forbidden job";
        ServiceJobApprovalStatus approvalStatus =
                ServiceJobApprovalStatus.NOT_REQUIRED;

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderId,
                jobNumber,
                summary,
                approvalStatus
        )).thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        orderId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(json(jobNumber, summary, approvalStatus))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void propagatesParentServiceOrderNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        String jobNumber = "JOB-NOT-FOUND-001";
        String summary = "Missing parent job";
        ServiceJobApprovalStatus approvalStatus =
                ServiceJobApprovalStatus.NOT_REQUIRED;

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderId,
                jobNumber,
                summary,
                approvalStatus
        )).thenThrow(new ResponseStatusException(
                NOT_FOUND,
                "Service order not found"
        ));

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        orderId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(json(jobNumber, summary, approvalStatus))
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void propagatesDuplicateJobNumberConflict()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        String jobNumber = "JOB-DUPLICATE-001";
        String summary = "Duplicate job";
        ServiceJobApprovalStatus approvalStatus =
                ServiceJobApprovalStatus.NOT_REQUIRED;

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderId,
                jobNumber,
                summary,
                approvalStatus
        )).thenThrow(new ResponseStatusException(
                CONFLICT,
                "Service job number already exists"
        ));

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        orderId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(json(jobNumber, summary, approvalStatus))
        )
        .andExpect(status().isConflict());
    }

    @Test
    void rejectsMalformedOrderIdWithoutCallingCommandService()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/jobs",
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(json(
                        "JOB-MALFORMED-001",
                        "Malformed order job",
                        ServiceJobApprovalStatus.NOT_REQUIRED
                ))
        )
        .andExpect(status().isBadRequest());

        verify(commandService, never()).create(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private String json(
            String jobNumber,
            String summary,
            ServiceJobApprovalStatus approvalStatus
    ) throws Exception {
        return objectMapper.writeValueAsString(
                new CreateServiceJobRequest(
                        jobNumber,
                        summary,
                        approvalStatus
                )
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
