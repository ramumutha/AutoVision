package com.autovision.platform.serviceprofit.data;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealerDataCapabilityControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private DealerDataCapabilityReadService readService;

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
                    "service-profit-manager"
            );

    @Test
    void capabilityReadRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/data-capability"
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void capabilityReadReturnsCommercialCapabilityProjection()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(readService.findCurrent(context))
                .thenReturn(
                        new DealerDataCapabilityResponse(
                                DealerDataCapabilityResponse
                                        .AssessmentState.ASSESSED,
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0",
                                "R1-DATA-READINESS-1",
                                OffsetDateTime.parse(
                                        "2026-08-22T08:05:00Z"
                                ),
                                List.of(
                                        new DealerDataCapabilityItemResponse(
                                                DealerDataCapability
                                                        .REVENUE_ATTRIBUTION,
                                                DealerDataCapabilityStatus
                                                        .AVAILABLE,
                                                "Invoice linkage is consistently available."
                                        ),
                                        new DealerDataCapabilityItemResponse(
                                                DealerDataCapability
                                                        .GROSS_PROFIT_ATTRIBUTION,
                                                DealerDataCapabilityStatus
                                                        .PARTIAL,
                                                "Gross-profit attribution is constrained by incomplete cost data."
                                        )
                                )
                        )
                );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/data-capability"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.assessmentState")
                        .value("ASSESSED")
        )
        .andExpect(
                jsonPath("$.sourceDatasetId")
                        .value(
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO"
                        )
        )
        .andExpect(
                jsonPath("$.sourceDatasetVersion")
                        .value("1.0.0")
        )
        .andExpect(
                jsonPath("$.assessmentPolicyVersion")
                        .value("R1-DATA-READINESS-1")
        )
        .andExpect(
                jsonPath("$.assessedAt")
                        .value("2026-08-22T08:05:00Z")
        )
        .andExpect(
                jsonPath("$.capabilities.length()")
                        .value(2)
        )
        .andExpect(
                jsonPath("$.capabilities[0].capability")
                        .value("REVENUE_ATTRIBUTION")
        )
        .andExpect(
                jsonPath("$.capabilities[0].status")
                        .value("AVAILABLE")
        )
        .andExpect(
                jsonPath("$.capabilities[1].capability")
                        .value("GROSS_PROFIT_ATTRIBUTION")
        )
        .andExpect(
                jsonPath("$.capabilities[1].status")
                        .value("PARTIAL")
        )

        // Raw assessment and identity data must remain internal.
        .andExpect(
                jsonPath("$.tenantId")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.dealerId")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.branchId")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.overallScore")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.identityCoverage")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.invoiceLinkageCoverage")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.costCoverage")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.createdByPrincipalId")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.updatedByPrincipalId")
                        .doesNotExist()
        );
    }

    @Test
    void capabilityReadReturnsExplicitNotAssessedState()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(readService.findCurrent(context))
                .thenReturn(
                        DealerDataCapabilityResponse.notAssessed()
                );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/data-capability"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.assessmentState")
                        .value("NOT_ASSESSED")
        )
        .andExpect(
                jsonPath("$.capabilities.length()")
                        .value(0)
        )
        .andExpect(
                jsonPath("$.overallScore")
                        .doesNotExist()
        );
    }

    @Test
    void capabilityReadPropagatesAuthorizationDenial()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(readService.findCurrent(context))
                .thenThrow(
                        new AccessDeniedException(
                                "Access is denied"
                        )
                );

        mockMvc.perform(
                get(
                        "/api/v1/service-profit/data-capability"
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