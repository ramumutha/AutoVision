package com.autovision.platform.serviceprofit;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceProfitOpportunityQueueControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceProfitOpportunityQueryService queryService;

    @MockitoBean
    private ServiceProfitOpportunityCommandService commandService;

    @MockitoBean
    private ServiceProfitOpportunityAccessService accessService;

    private final UUID userRefId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    userRefId,
                    tenantId,
                    "service-profit-manager"
            );

    @Test
    void queueRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void queueReturnsPagedProjection()
            throws Exception {

        UUID opportunityId = UUID.randomUUID();

        ServiceProfitOpportunityQueueItem item =
                new ServiceProfitOpportunityQueueItem(
                        opportunityId,
                        tenantId,
                        null,
                        null,
                        null,
                        "OPP-QUEUE-1",
                        ServiceProfitOpportunityType.DECLINED_WORK,
                        ServiceProfitOpportunityStatus.DETECTED,
                        ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                        ServiceProfitEvidenceStrength.STRONG,
                        ServiceProfitPriority.HIGH,
                        ServiceProfitActionability.READY,
                        "Declined brake work",
                        new BigDecimal("12400.0000"),
                        "INR",
                        OffsetDateTime.parse(
                                "2026-08-22T10:00:00+00:00"
                        )
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(queryService.findPage(
                any(),
                any()
        )).thenReturn(
                ServiceProfitOpportunityPage.of(
                        List.of(item),
                        0,
                        25,
                        1
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.items[0].id")
                        .value(opportunityId.toString())
        )
        .andExpect(
                jsonPath("$.items[0].priority")
                        .value("HIGH")
        )
        .andExpect(
                jsonPath("$.items[0].potentialAmount")
                        .value(12400.0000)
        )
        .andExpect(
                jsonPath("$.page")
                        .value(0)
        )
        .andExpect(
                jsonPath("$.size")
                        .value(25)
        )
        .andExpect(
                jsonPath("$.totalElements")
                        .value(1)
        )
        .andExpect(
                jsonPath("$.totalPages")
                        .value(1)
        );
    }

    @Test
    void queueRejectsInvalidPage()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
                .param("page", "-1")
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void queueRejectsOversizedPage()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
                .param("size", "101")
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void queueRejectsInvalidSort()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
                .param("sort", "DATABASE_COLUMN_DESC")
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void queuePropagatesAuthorizationDenial()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(queryService.findPage(
                any(),
                any()
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
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
