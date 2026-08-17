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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AfterSalesCaseControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private AfterSalesCaseService service;

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
    void listRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                get("/api/v1/aftersales-cases")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void listRejectsAuthenticatedRequestWithoutPermission()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findAll(context))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(
                get("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void listReturnsAuthenticatedTenantCases()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findAll(context))
                .thenReturn(List.of(caseRecord(caseId, "ASC-3001")));

        mockMvc.perform(
                get("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$[0].id")
                        .value(caseId.toString())
        )
        .andExpect(
                jsonPath("$[0].tenantId")
                        .value(tenantId.toString())
        )
        .andExpect(
                jsonPath("$[0].caseNumber")
                        .value("ASC-3001")
        )
        .andExpect(
                jsonPath("$[0].lifecycleStatus")
                        .value("OPEN")
        );
    }

    @Test
    void detailReturnsAuthenticatedCase()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findById(context, caseId))
                .thenReturn(caseRecord(caseId, "ASC-3002"));

        mockMvc.perform(
                get("/api/v1/aftersales-cases/{caseId}", caseId)
                        .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(caseId.toString())
        )
        .andExpect(
                jsonPath("$.caseNumber")
                        .value("ASC-3002")
        );
    }

    @Test
    void openCreatesAfterSalesCase()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.open(
                context,
                "ASC-3003",
                null,
                null,
                AfterSalesCaseSourceChannel.CUSTOMER_PORTAL
        )).thenReturn(caseRecord(caseId, "ASC-3003"));

        mockMvc.perform(
                post("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "caseNumber": "ASC-3003",
                                  "sourceChannel": "CUSTOMER_PORTAL"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.id")
                        .value(caseId.toString())
        )
        .andExpect(
                jsonPath("$.caseNumber")
                        .value("ASC-3003")
        );
    }

    @Test
    void closeReturnsClosedCase()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        AfterSalesCase afterSalesCase =
                caseRecord(caseId, "ASC-3004");

        afterSalesCase.close(
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.close(context, caseId))
                .thenReturn(afterSalesCase);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/close",
                        caseId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.lifecycleStatus")
                        .value("CLOSED")
        );
    }


    @Test
    void openRejectsBlankCaseNumber()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "caseNumber": " ",
                                  "sourceChannel": "CUSTOMER_PORTAL"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void openRejectsMissingSourceChannel()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "caseNumber": "ASC-INVALID"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void openRejectsBranchWithoutDealer()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post("/api/v1/aftersales-cases")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content("""
                                {
                                  "caseNumber": "ASC-INVALID",
                                  "branchId": "11111111-1111-1111-1111-111111111111",
                                  "sourceChannel": "SERVICE_ADVISOR"
                                }
                                """)
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

    private AfterSalesCase caseRecord(
            UUID caseId,
            String caseNumber
    ) {
        return AfterSalesCase.open(
                caseId,
                tenantId,
                null,
                null,
                caseNumber,
                AfterSalesCaseSourceChannel.CUSTOMER_PORTAL,
                userRefId,
                OffsetDateTime.now()
        );
    }
}