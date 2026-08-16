package com.autovision.platform.organization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealerControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private DealerService dealerService;

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
    void dealerListRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/dealers")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void dealerListRejectsAuthenticatedRequestWithoutPermission()
            throws Exception {

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                dealerService.findAll(context)
        ).thenThrow(
                new AccessDeniedException("Access is denied")
        );

        mockMvc.perform(
                get("/api/v1/dealers")
                        .with(jwt().jwt(token -> token
                                .subject("test-subject")
                                .claim(
                                        "autovision_user_ref_id",
                                        userRefId.toString()
                                )))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void dealerListReturnsAuthenticatedTenantData()
            throws Exception {

        UUID dealerId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                dealerService.findAll(context)
        ).thenReturn(
                List.of(
                        new DealerResponse(
                                dealerId,
                                "D001",
                                "Demo Dealer",
                                "Demo Dealer Legal",
                                null,
                                OrganizationStatus.ACTIVE,
                                OffsetDateTime.now(),
                                OffsetDateTime.now()
                        )
                )
        );

        mockMvc.perform(
                get("/api/v1/dealers")
                        .with(jwt().jwt(token -> token
                                .subject("test-subject")
                                .claim(
                                        "autovision_user_ref_id",
                                        userRefId.toString()
                                )))
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$[0].id")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$[0].code")
                        .value("D001")
        )
        .andExpect(
                jsonPath("$[0].name")
                        .value("Demo Dealer")
        );
    }

    @Test
    void dealerDetailReturnsAuthenticatedTenantData()
            throws Exception {

        UUID dealerId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                dealerService.findById(
                        context,
                        dealerId
                )
        ).thenReturn(
                new DealerResponse(
                        dealerId,
                        "D001",
                        "Demo Dealer",
                        null,
                        null,
                        OrganizationStatus.ACTIVE,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        mockMvc.perform(
                get("/api/v1/dealers/{dealerId}", dealerId)
                        .with(jwt().jwt(token -> token
                                .subject("test-subject")
                                .claim(
                                        "autovision_user_ref_id",
                                        userRefId.toString()
                                )))
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$.code")
                        .value("D001")
        );
    }
}