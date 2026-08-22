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
class ServiceProfitOpportunityControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceProfitOpportunityCommandService commandService;

    @MockitoBean
    private ServiceProfitOpportunityAccessService accessService;

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
                    "service-profit-user"
            );

    @Test
    void readRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/{id}",
                        UUID.randomUUID()
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void readReturnsAuthorizedOpportunity()
            throws Exception {

        UUID opportunityId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                opportunity(opportunityId);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireOpportunity(
                context,
                opportunityId
        )).thenReturn(opportunity);

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/{id}",
                        opportunityId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(opportunityId.toString())
        )
        .andExpect(
                jsonPath("$.tenantId")
                        .value(tenantId.toString())
        )
        .andExpect(
                jsonPath("$.opportunityType")
                        .value("DECLINED_WORK")
        )
        .andExpect(
                jsonPath("$.status")
                        .value("DETECTED")
        )
        .andExpect(
                jsonPath("$.evidenceClass")
                        .value("SOURCE_CONFIRMED")
        )
        .andExpect(
                jsonPath("$.priority")
                        .value("HIGH")
        );
    }

    @Test
    void readRejectsUnauthorizedScope()
            throws Exception {

        UUID opportunityId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(accessService.requireOpportunity(
                context,
                opportunityId
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/{id}",
                        opportunityId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void readRejectsMalformedOpportunityId()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/opportunities/{id}",
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsCreatedOpportunity()
            throws Exception {

        UUID opportunityId = UUID.randomUUID();

        ServiceProfitOpportunity opportunity =
                opportunity(opportunityId);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.create(
                any(),
                any()
        )).thenReturn(opportunity);

        String body = """
                {
                  "opportunityKey": "OPP-CREATE-1",
                  "opportunityType": "DECLINED_WORK",
                  "evidenceClass": "SOURCE_CONFIRMED",
                  "evidenceStrength": "STRONG",
                  "priority": "HIGH",
                  "actionability": "READY",
                  "title": "Declined brake work",
                  "sourceSystem": "DEALER_IMPORT",
                  "sourceEntityType": "RO_LINE",
                  "sourceEntityId": "SRC-1",
                  "policyVersion": "R1-POLICY-1"
                }
                """;

        mockMvc.perform(
                post(
                        "/api/v1/service-profit/opportunities"
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(opportunityId.toString())
        )
        .andExpect(
                jsonPath("$.currencyCode")
                        .value("INR")
        );
    }

    private ServiceProfitOpportunity opportunity(
            UUID opportunityId
    ) {
        return ServiceProfitOpportunity.detect(
                opportunityId,
                tenantId,
                null,
                null,
                null,
                null,
                null,
                "OPP-CREATE-1",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined brake work",
                null,
                new BigDecimal("12400.0000"),
                "INR",
                "DEALER_IMPORT",
                "RO_LINE",
                "SRC-1",
                null,
                null,
                null,
                null,
                "R1-POLICY-1",
                userRefId,
                OffsetDateTime.parse(
                        "2026-08-22T10:00:00+00:00"
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
