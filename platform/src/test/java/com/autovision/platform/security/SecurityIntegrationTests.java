package com.autovision.platform.security;

import java.util.UUID;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void meRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
void meAcceptsAuthenticatedMappedJwt() throws Exception {
    UUID userRefId =
            UUID.fromString("761b3ab6-bd03-48c0-a107-44fa1403b0f3");

    UUID tenantId =
            UUID.fromString("2cf85fea-bc61-4405-be50-00a0ca45df3b");

    when(tenantContextResolver.resolve(any()))
            .thenReturn(new AuthenticatedTenantContext(
                    userRefId,
                    tenantId,
                    "svc-advisor-01"
            ));

    mockMvc.perform(get("/api/v1/me")
                    .with(jwt().jwt(token -> token
                            .subject("test-subject")
                            .issuer(
                                "http://localhost:8081/realms/autovision"
                            )
                            .claim(
                                "autovision_user_ref_id",
                                userRefId.toString()
                            ))))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.subject")
                    .value("test-subject")
            )
            .andExpect(
                jsonPath("$.externalUserId")
                    .value("svc-advisor-01")
            )
            .andExpect(
                jsonPath("$.userRefId")
                    .value(userRefId.toString())
            )
            .andExpect(
                jsonPath("$.tenantId")
                    .value(tenantId.toString())
            );
}
}
