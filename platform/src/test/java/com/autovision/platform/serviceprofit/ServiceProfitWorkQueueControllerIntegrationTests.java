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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceProfitWorkQueueControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceProfitWorkQueueQueryService queryService;

        @MockitoBean
        private ServiceProfitFollowUpReadService followUpReadService;

    private final UUID principalId = UUID.fromString("761b3ab6-bd03-48c0-a107-44fa1403b0f3");
    private final UUID tenantId = UUID.fromString("2cf85fea-bc61-4405-be50-00a0ca45df3b");
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(principalId, tenantId, "svc-advisor-01");

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/service-profit/follow-ups"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(tenantContextResolver, queryService);
    }

    @Test
    void authorizedRequestReturnsSafeOperationalQueue() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(queryService.findPage(eq(context), eq(defaultQuery())))
                .thenReturn(ServiceProfitWorkQueuePage.of(List.of(item()), 0, 25, 1));

        mockMvc.perform(get("/api/v1/service-profit/follow-ups").with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].followUpId").value(item().followUpId().toString()))
                .andExpect(jsonPath("$.items[0].handlingStatus").value("OPEN"))
                .andExpect(jsonPath("$.items[0].title").value("Brake inspection"))
                .andExpect(jsonPath("$.items[0].summary").value("Brake evidence requires internal follow-up"))
                .andExpect(jsonPath("$.items[0].rawIntakePayload").doesNotExist())
                .andExpect(jsonPath("$.items[0].customerAuthorization").doesNotExist())
                .andExpect(jsonPath("$.items[0].recoveredRevenue").doesNotExist());
        verify(queryService).findPage(context, defaultQuery());
    }

    @Test
    void authorizedFollowUpReadReturnsSafeContract() throws Exception {
        UUID opportunityId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(followUpReadService.readCurrent(context, opportunityId)).thenReturn(new ServiceProfitFollowUpResponse(
                opportunityId, ServiceProfitFollowUpHandlingStatus.OPEN, ServiceProfitFollowUpOwnership.MINE,
                null, ServiceProfitWorkQueueDueState.NO_DUE_DATE, ServiceProfitFollowUpDisposition.NONE, 3));

        mockMvc.perform(get("/api/v1/service-profit/opportunities/{opportunityId}/follow-up", opportunityId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunityId").value(opportunityId.toString()))
                .andExpect(jsonPath("$.ownership").value("MINE"))
                .andExpect(jsonPath("$.ownerPrincipalId").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.recoveredRevenue").doesNotExist());
        verify(followUpReadService).readCurrent(context, opportunityId);
    }

    @Test
    void authorizedHistoryReadReturnsSafeContract() throws Exception {
        UUID opportunityId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(followUpReadService.readHistory(context, opportunityId)).thenReturn(List.of(
                new ServiceProfitFollowUpHistoryResponse(ServiceProfitFollowUpEventType.CREATED,
                        ServiceProfitFollowUpHistoryActorType.SYSTEM, "AUTOVISION_SERVICE_PROFIT",
                        "AUTOVISION_SERVICE_PROFIT", OffsetDateTime.parse("2026-08-20T10:00:00Z"), null, "OPEN")));

        mockMvc.perform(get("/api/v1/service-profit/opportunities/{opportunityId}/follow-up/history", opportunityId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorIdentity").value("AUTOVISION_SERVICE_PROFIT"))
                .andExpect(jsonPath("$[0].applicationIdentity").value("AUTOVISION_SERVICE_PROFIT"))
                .andExpect(jsonPath("$[0].actorPrincipalId").doesNotExist())
                .andExpect(jsonPath("$[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$[0].financialOutcome").doesNotExist());
        verify(followUpReadService).readHistory(context, opportunityId);
    }

    @Test
    void permissionDenialReturnsForbidden() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(queryService.findPage(eq(context), any()))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/v1/service-profit/follow-ups").with(authenticatedJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantIdParameterCannotOverrideAuthenticatedTenant() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(queryService.findPage(eq(context), eq(defaultQuery())))
                .thenReturn(ServiceProfitWorkQueuePage.of(List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/v1/service-profit/follow-ups")
                        .param("tenantId", UUID.randomUUID().toString())
                        .with(authenticatedJwt()))
                .andExpect(status().isOk());
        verify(queryService).findPage(context, defaultQuery());
    }

    @Test
    void mineUsesAuthenticatedPrincipalAndUnassignedMapsExplicitly() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        ServiceProfitWorkQueueQuery mine = new ServiceProfitWorkQueueQuery(
                null, ServiceProfitWorkQueueOwnership.MINE, null, null, 0, 25);
        ServiceProfitWorkQueueQuery unassigned = new ServiceProfitWorkQueueQuery(
                null, ServiceProfitWorkQueueOwnership.UNASSIGNED, null, null, 0, 25);
        when(queryService.findPage(context, mine)).thenReturn(ServiceProfitWorkQueuePage.of(List.of(), 0, 25, 0));
        when(queryService.findPage(context, unassigned)).thenReturn(ServiceProfitWorkQueuePage.of(List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/v1/service-profit/follow-ups")
                        .param("ownership", "MINE").with(authenticatedJwt()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/service-profit/follow-ups")
                        .param("ownership", "UNASSIGNED").with(authenticatedJwt()))
                .andExpect(status().isOk());
        verify(queryService).findPage(context, mine);
        verify(queryService).findPage(context, unassigned);
    }

    @Test
    void supportedFiltersAndPaginationMapToFrozenQuery() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        ServiceProfitWorkQueueQuery query = new ServiceProfitWorkQueueQuery(
                ServiceProfitFollowUpHandlingStatus.COMPLETED,
                ServiceProfitWorkQueueOwnership.ALL,
                ServiceProfitWorkQueueDueState.OVERDUE,
                ServiceProfitFollowUpDisposition.NO_RESPONSE_RECORDED,
                2,
                10);
        when(queryService.findPage(context, query)).thenReturn(ServiceProfitWorkQueuePage.of(List.of(), 2, 10, 0));

        mockMvc.perform(get("/api/v1/service-profit/follow-ups")
                        .param("handlingStatus", "COMPLETED")
                        .param("ownership", "ALL")
                        .param("dueState", "OVERDUE")
                        .param("disposition", "NO_RESPONSE_RECORDED")
                        .param("page", "2")
                        .param("size", "10")
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
        verify(queryService).findPage(context, query);
    }

    @Test
    void invalidPageSizeAndFiltersAreRejectedBeforeQueryExecution() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);

        mockMvc.perform(get("/api/v1/service-profit/follow-ups").param("page", "-1").with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/service-profit/follow-ups").param("size", "0").with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/service-profit/follow-ups").param("size", "101").with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/service-profit/follow-ups").param("dueState", "INVALID").with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queryService);
    }

    private ServiceProfitWorkQueueQuery defaultQuery() {
        return new ServiceProfitWorkQueueQuery(null, null, null, null, 0, 25);
    }

    private RequestPostProcessor authenticatedJwt() {
        return jwt().jwt(token -> token.subject("test-subject")
                .claim(TenantContextResolver.USER_REF_CLAIM, principalId.toString()));
    }

    private ServiceProfitWorkQueueItem item() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-27T10:00:00Z");
        return new ServiceProfitWorkQueueItem(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                ServiceProfitFollowUpHandlingStatus.OPEN, null, null, now.plusDays(1),
                ServiceProfitFollowUpDisposition.NONE, 0, now, now,
                ServiceProfitActionability.READY, ServiceProfitOpportunityStatus.DETECTED,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED, ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH, "Brake inspection",
                "Brake evidence requires internal follow-up");
    }
}