package com.autovision.platform.workflow;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceWorkflowConfigurationControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantContextResolver tenantContextResolver;
    @MockitoBean private ServiceWorkflowConfigurationReadService readService;
    @MockitoBean private ServiceWorkflowConfigurationCommandService commandService;

    private final UUID userRefId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(userRefId, tenantId, "user");

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/service-workflows"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listDefinitionsReturnsSafeResponses() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.findDefinitions(context)).thenReturn(List.of(definition()));

        mockMvc.perform(get("/api/v1/admin/service-workflows")
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(definitionId.toString()))
                .andExpect(jsonPath("$[0].tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$[0].code").value("STANDARD"));
    }

    @Test
    void definitionReadMapsResponse() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.requireDefinition(context, definitionId)).thenReturn(definition());

        mockMvc.perform(get("/api/v1/admin/service-workflows/{id}", definitionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Standard"));
    }

    @Test
    void versionsStagesAndStatusesMapResponses() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.findVersions(context, definitionId)).thenReturn(List.of(version()));
        when(readService.requireVersion(context, definitionId, versionId)).thenReturn(version());
        when(readService.findStages(context, definitionId, versionId)).thenReturn(List.of(stage()));
        when(readService.findStatuses(context, definitionId, versionId, stageId))
                .thenReturn(List.of(workflowStatus()));

        mockMvc.perform(get("/api/v1/admin/service-workflows/{d}/versions", definitionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(1));
        mockMvc.perform(get("/api/v1/admin/service-workflows/{d}/versions/{v}", definitionId, versionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(get("/api/v1/admin/service-workflows/{d}/versions/{v}/stages", definitionId, versionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("INTAKE"));
        mockMvc.perform(get("/api/v1/admin/service-workflows/{d}/versions/{v}/stages/{s}/statuses",
                        definitionId, versionId, stageId).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("OPEN"));
    }

    @Test
    void commandEndpointsForwardRequestsAndMapResponses() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.createDefinition(context, null, null, "STANDARD", "Standard"))
                .thenReturn(definition());
        when(commandService.createDraftVersion(context, definitionId, 1)).thenReturn(version());
        when(commandService.addStage(context, versionId, "INTAKE", "Intake", 1)).thenReturn(stage());
        when(commandService.addStatus(context, stageId, "OPEN", "Open", 1)).thenReturn(workflowStatus());
        when(commandService.publish(context, versionId)).thenReturn(version());
        when(commandService.retire(context, versionId)).thenReturn(version());

        mockMvc.perform(post("/api/v1/admin/service-workflows")
                        .with(authenticatedJwt()).contentType("application/json")
                        .content("{\"code\":\"STANDARD\",\"displayName\":\"Standard\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/service-workflows/{d}/versions", definitionId)
                        .with(authenticatedJwt()).contentType("application/json")
                        .content("{\"versionNumber\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/service-workflows/{d}/versions/{v}/stages",
                        definitionId, versionId).with(authenticatedJwt())
                        .contentType("application/json")
                        .content("{\"code\":\"INTAKE\",\"displayName\":\"Intake\",\"sequence\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/service-workflows/{d}/versions/{v}/stages/{s}/statuses",
                        definitionId, versionId, stageId).with(authenticatedJwt())
                        .contentType("application/json")
                        .content("{\"code\":\"OPEN\",\"displayName\":\"Open\",\"sequence\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/service-workflows/{d}/versions/{v}/publish",
                        definitionId, versionId).with(authenticatedJwt()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/service-workflows/{d}/versions/{v}/retire",
                        definitionId, versionId).with(authenticatedJwt()))
                .andExpect(status().isOk());

        verify(commandService).createDefinition(context, null, null, "STANDARD", "Standard");
        verify(commandService).createDraftVersion(context, definitionId, 1);
        verify(commandService).addStage(context, versionId, "INTAKE", "Intake", 1);
        verify(commandService).addStatus(context, stageId, "OPEN", "Open", 1);
        verify(commandService).publish(context, versionId);
        verify(commandService).retire(context, versionId);
    }

    @Test
    void readAuthorizationDenialReturnsForbidden() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.requireDefinition(context, definitionId))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(get("/api/v1/admin/service-workflows/{id}", definitionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void commandAuthorizationDenialReturnsForbidden() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.createDefinition(context, null, null, "A", "A"))
                .thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(post("/api/v1/admin/service-workflows")
                        .with(authenticatedJwt()).contentType("application/json")
                        .content("{\"code\":\"A\",\"displayName\":\"A\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void notFoundAndMalformedUuidUsePlatformHttpSemantics() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.requireDefinition(context, definitionId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "missing"));

        mockMvc.perform(get("/api/v1/admin/service-workflows/{id}", definitionId)
                        .with(authenticatedJwt()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/service-workflows/not-a-uuid")
                        .with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
    }

    private RequestPostProcessor authenticatedJwt() {
        return jwt().jwt(token -> token.subject("test-subject")
                .claim("autovision_user_ref_id", userRefId.toString()));
    }

    private ServiceWorkflowDefinition definition() {
        return ServiceWorkflowDefinition.create(definitionId, tenantId, null, null,
                "STANDARD", "Standard", userRefId, OffsetDateTime.now());
    }

    private ServiceWorkflowVersion version() {
        return ServiceWorkflowVersion.draft(versionId, definitionId, 1,
                userRefId, OffsetDateTime.now());
    }

    private ServiceWorkflowStage stage() {
        return ServiceWorkflowStage.create(stageId, versionId, "INTAKE", "Intake", 1);
    }

        private ServiceWorkflowStatus workflowStatus() {
        return ServiceWorkflowStatus.create(UUID.randomUUID(), stageId, "OPEN", "Open", 1);
    }
}