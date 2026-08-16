package com.autovision.platform.organization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class BranchControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private BranchService branchService;

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
    void branchListRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/branches")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void branchListReturnsAuthenticatedTenantData()
            throws Exception {

        UUID branchId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                branchService.findAll(context)
        ).thenReturn(
                List.of(
                        new BranchResponse(
                                branchId,
                                dealerId,
                                locationId,
                                "B001",
                                "Demo Branch",
                                OrganizationStatus.ACTIVE,
                                OffsetDateTime.now(),
                                OffsetDateTime.now()
                        )
                )
        );

        mockMvc.perform(
                get("/api/v1/branches")
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
                        .value(branchId.toString())
        )
        .andExpect(
                jsonPath("$[0].dealerId")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$[0].locationId")
                        .value(locationId.toString())
        )
        .andExpect(
                jsonPath("$[0].code")
                        .value("B001")
        )
        .andExpect(
                jsonPath("$[0].name")
                        .value("Demo Branch")
        );
    }

    @Test
    void branchDetailReturnsAuthenticatedTenantData()
            throws Exception {

        UUID branchId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                branchService.findById(
                        context,
                        branchId
                )
        ).thenReturn(
                new BranchResponse(
                        branchId,
                        dealerId,
                        null,
                        "B001",
                        "Demo Branch",
                        OrganizationStatus.ACTIVE,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        mockMvc.perform(
                get("/api/v1/branches/{branchId}", branchId)
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
                        .value(branchId.toString())
        )
        .andExpect(
                jsonPath("$.dealerId")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$.code")
                        .value("B001")
        );
    }
}