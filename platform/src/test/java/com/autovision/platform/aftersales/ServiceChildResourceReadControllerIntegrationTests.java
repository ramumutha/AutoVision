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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceChildResourceReadControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceOrderAccessService accessService;

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
    void findsJobsForServiceOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceJob job = job(orderId);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.findJobs(context, orderId))
                .thenReturn(List.of(job));

        mockMvc.perform(
                get("/api/v1/service-orders/{orderId}/jobs", orderId)
                        .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id")
                .value(job.getId().toString()))
        .andExpect(jsonPath("$[0].serviceOrderId")
                .value(orderId.toString()))
        .andExpect(jsonPath("$[0].jobNumber")
                .value(job.getJobNumber()))
        .andExpect(jsonPath("$[0].summary")
                .value(job.getSummary()))
        .andExpect(jsonPath("$[0].status")
                .value("OPEN"))
        .andExpect(jsonPath("$[0].approvalStatus")
                .value("NOT_REQUIRED"));

        verify(accessService).findJobs(context, orderId);
    }

    @Test
    void findsIndividualJobForServiceOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceJob job = job(orderId);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.requireJob(
                context,
                orderId,
                job.getId()
        )).thenReturn(job);

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}",
                        orderId,
                        job.getId()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(job.getId().toString()))
        .andExpect(jsonPath("$.serviceOrderId")
                .value(orderId.toString()))
        .andExpect(jsonPath("$.jobNumber")
                .value(job.getJobNumber()));

        verify(accessService).requireJob(
                context,
                orderId,
                job.getId()
        );
    }

    @Test
    void findsLinesForServiceOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceLine line = line(orderId, null);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.findLines(context, orderId))
                .thenReturn(List.of(line));

        mockMvc.perform(
                get("/api/v1/service-orders/{orderId}/lines", orderId)
                        .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id")
                .value(line.getId().toString()))
        .andExpect(jsonPath("$[0].serviceOrderId")
                .value(orderId.toString()))
        .andExpect(jsonPath("$[0].lineNumber")
                .value(line.getLineNumber()))
        .andExpect(jsonPath("$[0].description")
                .value(line.getDescription()))
        .andExpect(jsonPath("$[0].lineType")
                .value("LABOR"));

        verify(accessService).findLines(context, orderId);
    }

    @Test
    void findsIndividualLineForServiceOrder() throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceLine line = line(orderId, null);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.requireLine(
                context,
                orderId,
                line.getId()
        )).thenReturn(line);

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}",
                        orderId,
                        line.getId()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(line.getId().toString()))
        .andExpect(jsonPath("$.serviceOrderId")
                .value(orderId.toString()))
        .andExpect(jsonPath("$.description")
                .value(line.getDescription()));

        verify(accessService).requireLine(
                context,
                orderId,
                line.getId()
        );
    }

    @Test
    void findsLinesForServiceJob() throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceJob job = job(orderId);
        ServiceLine line = line(orderId, job.getId());

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.findLinesForJob(
                context,
                orderId,
                job.getId()
        )).thenReturn(List.of(line));

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}/lines",
                        orderId,
                        job.getId()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id")
                .value(line.getId().toString()))
        .andExpect(jsonPath("$[0].serviceJobId")
                .value(job.getId().toString()))
        .andExpect(jsonPath("$[0].description")
                .value(line.getDescription()));

        verify(accessService).findLinesForJob(
                context,
                orderId,
                job.getId()
        );
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorizedWithoutAccessCall()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/jobs",
                        UUID.randomUUID()
                )
                .with(csrf())
        )
        .andExpect(status().isUnauthorized());

        verify(accessService, never()).findJobs(any(), any());
    }

    @Test
    void authorizationDenialPropagatesAsForbidden() throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.findJobs(context, orderId))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(
                get("/api/v1/service-orders/{orderId}/jobs", orderId)
                        .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void parentServiceOrderNotFoundPropagatesAsNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.findLines(context, orderId))
                .thenThrow(new ResponseStatusException(
                        NOT_FOUND,
                        "Service order not found"
                ));

        mockMvc.perform(
                get("/api/v1/service-orders/{orderId}/lines", orderId)
                        .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void containedJobNotFoundPropagatesAsNotFound() throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.requireJob(context, orderId, jobId))
                .thenThrow(new ResponseStatusException(
                        NOT_FOUND,
                        "Service job not found"
                ));

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/jobs/{jobId}",
                        orderId,
                        jobId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void containedLineNotFoundPropagatesAsNotFound() throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(accessService.requireLine(context, orderId, lineId))
                .thenThrow(new ResponseStatusException(
                        NOT_FOUND,
                        "Service line not found"
                ));

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}",
                        orderId,
                        lineId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void malformedOrderIdReturnsBadRequestWithoutAccessCall()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/service-orders/{orderId}/jobs", "not-a-uuid")
                        .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());

        verify(accessService, never()).findJobs(any(), any());
    }

    private ServiceJob job(UUID orderId) {
        return ServiceJob.open(
                UUID.randomUUID(),
                orderId,
                "JOB-READ-001",
                "Read-side diagnostic work",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                userRefId,
                OffsetDateTime.now()
        );
    }

    private ServiceLine line(UUID orderId, UUID jobId) {
        return ServiceLine.create(
                UUID.randomUUID(),
                orderId,
                jobId,
                1,
                ServiceLineType.LABOR,
                "Read-side labor",
                BigDecimal.ONE,
                "HOUR",
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
