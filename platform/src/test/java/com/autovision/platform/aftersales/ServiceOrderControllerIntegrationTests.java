package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

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
    void aggregateRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        UUID.randomUUID()
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void aggregateReturnsContainedServiceOrder()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        OffsetDateTime now =
                OffsetDateTime.parse(
                        "2026-08-17T12:00:00+00:00"
                );

        ServiceOrder order =
                ServiceOrder.open(
                        orderId,
                        tenantId,
                        dealerId,
                        branchId,
                        "SO-6001",
                        vehicleId,
                        userRefId,
                        now
                );

        ServiceJob job =
                ServiceJob.open(
                        jobId,
                        orderId,
                        "JOB-001",
                        "Brake service",
                        ServiceJobApprovalStatus.NOT_REQUIRED,
                        userRefId,
                        now
                );

        ServiceLine line =
                ServiceLine.create(
                        lineId,
                        orderId,
                        jobId,
                        10,
                        ServiceLineType.LABOR,
                        "Brake labor",
                        new BigDecimal("2.0000"),
                        "HOUR",
                        userRefId,
                        now
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireAggregate(
                context,
                orderId
        )).thenReturn(
                new ServiceOrderAggregateView(
                        order,
                        List.of(job),
                        List.of(line)
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.order.id")
                        .value(orderId.toString())
        )
        .andExpect(
                jsonPath("$.order.tenantId")
                        .value(tenantId.toString())
        )
        .andExpect(
                jsonPath("$.order.dealerId")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$.order.branchId")
                        .value(branchId.toString())
        )
        .andExpect(
                jsonPath("$.order.orderNumber")
                        .value("SO-6001")
        )
        .andExpect(
                jsonPath("$.order.status")
                        .value("OPEN")
        )
        .andExpect(
                jsonPath("$.jobs[0].id")
                        .value(jobId.toString())
        )
        .andExpect(
                jsonPath("$.jobs[0].jobNumber")
                        .value("JOB-001")
        )
        .andExpect(
                jsonPath("$.jobs[0].status")
                        .value("OPEN")
        )
        .andExpect(
                jsonPath("$.lines[0].id")
                        .value(lineId.toString())
        )
        .andExpect(
                jsonPath("$.lines[0].serviceJobId")
                        .value(jobId.toString())
        )
        .andExpect(
                jsonPath("$.lines[0].lineType")
                        .value("LABOR")
        )
        .andExpect(
                jsonPath("$.lines[0].quantity")
                        .value(2.0000)
        );
    }

    @Test
    void aggregateSupportsOrderWithoutJobsOrLines()
            throws Exception {

        UUID orderId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        ServiceOrder order =
                ServiceOrder.open(
                        orderId,
                        tenantId,
                        null,
                        null,
                        "SO-6002",
                        vehicleId,
                        userRefId,
                        OffsetDateTime.now()
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireAggregate(
                context,
                orderId
        )).thenReturn(
                new ServiceOrderAggregateView(
                        order,
                        List.of(),
                        List.of()
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.jobs")
                        .isEmpty()
        )
        .andExpect(
                jsonPath("$.lines")
                        .isEmpty()
        );
    }

    @Test
    void aggregatePropagatesNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireAggregate(
                context,
                orderId
        )).thenThrow(
                new ResponseStatusException(
                        NOT_FOUND,
                        "Service order not found"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void aggregateRejectsAuthenticatedRequestWithoutAuthorizedScope()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireAggregate(
                context,
                orderId
        )).thenThrow(
                new org.springframework.security.access.AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }
    @Test
    void aggregateRejectsMalformedOrderId()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-orders/{orderId}/aggregate",
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
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