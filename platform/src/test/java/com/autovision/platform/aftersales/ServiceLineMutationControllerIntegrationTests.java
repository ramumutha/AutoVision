package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
class ServiceLineMutationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceLineCommandService commandService;

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
    void createRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        UUID.randomUUID()
                )
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void createsDirectOrderLevelLine()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        ServiceLine line =
                line(orderId, null, 10);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                context,
                orderId,
                null,
                10,
                ServiceLineType.LABOR,
                "Diagnostic labor",
                new BigDecimal("1.5000"),
                "HOUR"
        )).thenReturn(line);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        orderId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.serviceOrderId")
                        .value(orderId.toString())
        )
        .andExpect(
                jsonPath("$.serviceJobId")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.lineNumber")
                        .value(10)
        )
        .andExpect(
                jsonPath("$.lineType")
                        .value("LABOR")
        );
    }

    @Test
    void createsLineAssignedToJob()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceLine line =
                line(orderId, jobId, 10);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                context,
                orderId,
                jobId,
                10,
                ServiceLineType.LABOR,
                "Diagnostic labor",
                new BigDecimal("1.5000"),
                "HOUR"
        )).thenReturn(line);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        orderId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(jobId))
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.serviceJobId")
                        .value(jobId.toString())
        );
    }

    @Test
    void updateDetailsReturnsUpdatedLine()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        ServiceLine line =
                ServiceLine.create(
                        lineId,
                        orderId,
                        null,
                        10,
                        ServiceLineType.PART,
                        "Updated part",
                        new BigDecimal("2.0000"),
                        "EA",
                        userRefId,
                        OffsetDateTime.now()
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.updateDetails(
                context,
                orderId,
                lineId,
                ServiceLineType.PART,
                "Updated part",
                new BigDecimal("2.0000"),
                "EA"
        )).thenReturn(line);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}/update-details",
                        orderId,
                        lineId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "lineType": "PART",
                          "description": "Updated part",
                          "quantity": 2.0000,
                          "unitOfMeasure": "EA"
                        }
                        """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.description")
                        .value("Updated part")
        )
        .andExpect(
                jsonPath("$.lineType")
                        .value("PART")
        );
    }

    @Test
    void assignJobReturnsAssignedLine()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ServiceLine line =
                ServiceLine.create(
                        lineId,
                        orderId,
                        jobId,
                        10,
                        ServiceLineType.LABOR,
                        "Assigned labor",
                        BigDecimal.ONE,
                        "HOUR",
                        userRefId,
                        OffsetDateTime.now()
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.assignToJob(
                context,
                orderId,
                lineId,
                jobId
        )).thenReturn(line);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}/assign-job/{jobId}",
                        orderId,
                        lineId,
                        jobId
                )
                .with(authenticatedJwt())
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.serviceJobId")
                        .value(jobId.toString())
        );

        verify(commandService)
                .assignToJob(
                        context,
                        orderId,
                        lineId,
                        jobId
                );
    }

    @Test
    void unassignJobReturnsDirectLine()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        ServiceLine line =
                line(orderId, null, 10);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.unassignFromJob(
                context,
                orderId,
                lineId
        )).thenReturn(line);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}/unassign-job",
                        orderId,
                        lineId
                )
                .with(authenticatedJwt())
                .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.serviceJobId")
                        .doesNotExist()
        );
    }

    @Test
    void mutationRejectsAuthenticatedRequestWithoutAuthorizedScope()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        orderId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mutationPropagatesNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.unassignFromJob(
                context,
                orderId,
                lineId
        )).thenThrow(
                new ResponseStatusException(
                        NOT_FOUND,
                        "Service line not found"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}/unassign-job",
                        orderId,
                        lineId
                )
                .with(authenticatedJwt())
                .with(csrf())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void duplicateLineNumberReturnsConflict()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(
                new ResponseStatusException(
                        CONFLICT,
                        "Service line number already exists"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        orderId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isConflict());
    }

    @Test
    void invalidLineDetailsReturnBadRequest()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(
                new IllegalArgumentException(
                        "Service line quantity must be greater than zero"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        orderId
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void mutationRejectsMalformedOrderId()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines",
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateJson(null))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void mutationRejectsMalformedLineId()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/lines/{lineId}/unassign-job",
                        UUID.randomUUID(),
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
                .with(csrf())
        )
        .andExpect(status().isBadRequest());
    }

    private ServiceLine line(
            UUID orderId,
            UUID jobId,
            int lineNumber
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                orderId,
                jobId,
                lineNumber,
                ServiceLineType.LABOR,
                "Diagnostic labor",
                new BigDecimal("1.5000"),
                "HOUR",
                userRefId,
                OffsetDateTime.now()
        );
    }

    private String validCreateJson(
            UUID serviceJobId
    ) {
        String jobValue =
                serviceJobId == null
                        ? "null"
                        : "\"" + serviceJobId + "\"";

        return """
                {
                  "serviceJobId": %s,
                  "lineNumber": 10,
                  "lineType": "LABOR",
                  "description": "Diagnostic labor",
                  "quantity": 1.5000,
                  "unitOfMeasure": "HOUR"
                }
                """.formatted(jobValue);
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