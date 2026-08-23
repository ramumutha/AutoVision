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
        )
        .andExpect(jsonPath("$.items[0].context").doesNotExist())
        .andExpect(jsonPath("$.items[0].customerId").doesNotExist())
        .andExpect(jsonPath("$.items[0].vehicleId").doesNotExist());
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

    @Test
    void summaryRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/summary"
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void summaryReturnsManagerProjection()
            throws Exception {

        ServiceProfitOpportunitySummary summary =
                new ServiceProfitOpportunitySummary(
                        7,
                        3,
                        2,
                        4,
                        1,
                        List.of(
                                new ServiceProfitCurrencyPotential(
                                        "INR",
                                        new BigDecimal("42500.0000")
                                ),
                                new ServiceProfitCurrencyPotential(
                                        "USD",
                                        new BigDecimal("250.0000")
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "DECLINED_WORK",
                                        5
                                ),
                                new ServiceProfitOpportunityCount(
                                        "OVERDUE_SERVICE",
                                        2
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "HIGH",
                                        3
                                ),
                                new ServiceProfitOpportunityCount(
                                        "MEDIUM",
                                        4
                                )
                        ),
                        List.of(
                                new ServiceProfitOpportunityCount(
                                        "READY",
                                        4
                                ),
                                new ServiceProfitOpportunityCount(
                                        "REVIEW_REQUIRED",
                                        2
                                ),
                                new ServiceProfitOpportunityCount(
                                        "SUPPRESSED",
                                        1
                                )
                        )
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(queryService.findSummary(
                any(),
                any()
        )).thenReturn(summary);

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/summary"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.totalOpportunities")
                        .value(7)
        )
        .andExpect(
                jsonPath("$.highPriorityCount")
                        .value(3)
        )
        .andExpect(
                jsonPath("$.reviewRequiredCount")
                        .value(2)
        )
        .andExpect(
                jsonPath("$.readyCount")
                        .value(4)
        )
        .andExpect(
                jsonPath("$.suppressedCount")
                        .value(1)
        )
        .andExpect(
                jsonPath("$.potentialByCurrency[0].currencyCode")
                        .value("INR")
        )
        .andExpect(
                jsonPath("$.potentialByCurrency[0].amount")
                        .value(42500.0000)
        )
        .andExpect(
                jsonPath("$.potentialByCurrency[1].currencyCode")
                        .value("USD")
        )
        .andExpect(
                jsonPath("$.potentialByCurrency[1].amount")
                        .value(250.0000)
        )
        .andExpect(
                jsonPath("$.byOpportunityType[0].key")
                        .value("DECLINED_WORK")
        )
        .andExpect(
                jsonPath("$.byOpportunityType[0].count")
                        .value(5)
        )
        .andExpect(
                jsonPath("$.byPriority[0].key")
                        .value("HIGH")
        )
        .andExpect(
                jsonPath("$.byPriority[0].count")
                        .value(3)
        )
        .andExpect(
                jsonPath("$.byActionability[0].key")
                        .value("READY")
        )
        .andExpect(
                jsonPath("$.byActionability[0].count")
                        .value(4)
        );
    }

    @Test
    void summaryPropagatesBusinessFilters()
            throws Exception {

        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(queryService.findSummary(
                any(),
                any()
        )).thenReturn(
                new ServiceProfitOpportunitySummary(
                        0,
                        0,
                        0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/summary"
                )
                .param("status", "DETECTED")
                .param("priority", "HIGH")
                .param(
                        "opportunityType",
                        "DECLINED_WORK"
                )
                .param(
                        "evidenceClass",
                        "SOURCE_CONFIRMED"
                )
                .param(
                        "evidenceStrength",
                        "STRONG"
                )
                .param(
                        "actionability",
                        "READY"
                )
                .param(
                        "dealerId",
                        dealerId.toString()
                )
                .param(
                        "branchId",
                        branchId.toString()
                )
                .param(
                        "locationId",
                        locationId.toString()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<ServiceProfitOpportunityQuery>
                queryCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        ServiceProfitOpportunityQuery.class
                );

        org.mockito.Mockito.verify(queryService)
                .findSummary(
                        org.mockito.ArgumentMatchers.eq(context),
                        queryCaptor.capture()
                );

        ServiceProfitOpportunityQuery captured =
                queryCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitOpportunityStatus.DETECTED,
                captured.status()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitPriority.HIGH,
                captured.priority()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitOpportunityType.DECLINED_WORK,
                captured.opportunityType()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                captured.evidenceClass()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitEvidenceStrength.STRONG,
                captured.evidenceStrength()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ServiceProfitActionability.READY,
                captured.actionability()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                dealerId,
                captured.dealerId()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                branchId,
                captured.branchId()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                locationId,
                captured.locationId()
        );
    }

    @Test
    void summaryRejectsInvalidFilter()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/summary"
                )
                .param(
                        "priority",
                        "NOT_A_PRIORITY"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void summaryPropagatesAuthorizationDenial()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(queryService.findSummary(
                any(),
                any()
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/summary"
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
