package com.autovision.platform.organization;

import java.math.BigDecimal;
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
class LocationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private LocationService locationService;

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
    void locationListRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/locations")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void locationListReturnsAuthenticatedTenantData()
            throws Exception {

        UUID locationId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                locationService.findAll(context)
        ).thenReturn(
                List.of(
                        location(
                                locationId,
                                "L001",
                                "Demo Location"
                        )
                )
        );

        mockMvc.perform(
                get("/api/v1/locations")
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
                        .value(locationId.toString())
        )
        .andExpect(
                jsonPath("$[0].code")
                        .value("L001")
        )
        .andExpect(
                jsonPath("$[0].name")
                        .value("Demo Location")
        )
        .andExpect(
                jsonPath("$[0].city")
                        .value("Bengaluru")
        )
        .andExpect(
                jsonPath("$[0].countryCode")
                        .value("IN")
        );
    }

    @Test
    void locationDetailReturnsAuthenticatedTenantData()
            throws Exception {

        UUID locationId = UUID.randomUUID();

        when(
                tenantContextResolver.resolve(any())
        ).thenReturn(context);

        when(
                locationService.findById(
                        context,
                        locationId
                )
        ).thenReturn(
                location(
                        locationId,
                        "L001",
                        "Demo Location"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/locations/{locationId}",
                        locationId
                )
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
                        .value(locationId.toString())
        )
        .andExpect(
                jsonPath("$.code")
                        .value("L001")
        )
        .andExpect(
                jsonPath("$.timezone")
                        .value("Asia/Kolkata")
        );
    }

    private LocationResponse location(
            UUID id,
            String code,
            String name
    ) {
        return new LocationResponse(
                id,
                code,
                name,
                "100 Demo Road",
                null,
                "Bengaluru",
                "Karnataka",
                "560001",
                "IN",
                "Asia/Kolkata",
                new BigDecimal("12.971599"),
                new BigDecimal("77.594566"),
                OrganizationStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}