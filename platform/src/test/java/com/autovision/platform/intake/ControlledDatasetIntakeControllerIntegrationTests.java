package com.autovision.platform.intake;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ControlledDatasetIntakeControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ControlledDatasetIntakeCommandService commandService;

    @Test
    void unauthenticatedIntakeRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/controlled-data-intake")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"packageReference\":\"partner-r1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedIntakeReturnsSafeOperationalSummary() throws Exception {
        UUID userRefId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AuthenticatedTenantContext context = new AuthenticatedTenantContext(
                userRefId, tenantId, "operator");
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        UUID processingId = UUID.randomUUID();
        when(commandService.process(eq(context), eq("partner-r1"))).thenReturn(
                new ControlledDatasetProcessingResult(processingId, "dataset-1", "1",
                        DatasetProcessingStatus.STAGED, 4, 3, 1, 0, 1, 0, 0, false));

        mockMvc.perform(post("/api/v1/controlled-data-intake")
                        .with(jwt())
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"packageReference\":\"partner-r1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetProcessingId").value(processingId.toString()))
                .andExpect(jsonPath("$.status").value("STAGED"))
                .andExpect(jsonPath("$.recordsReceived").value(4))
                .andExpect(jsonPath("$.recordsQuarantined").value(1))
                .andExpect(jsonPath("$.packageReference").doesNotExist());
    }
}