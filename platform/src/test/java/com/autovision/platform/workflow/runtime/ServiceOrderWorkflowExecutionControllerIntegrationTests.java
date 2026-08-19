package com.autovision.platform.workflow.runtime;

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
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrderWorkflowExecutionControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantContextResolver tenantContextResolver;
    @MockitoBean private ServiceOrderWorkflowExecutionCommandService commandService;
    @MockitoBean private ServiceOrderWorkflowExecutionReadService readService;

    private final UUID userRefId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final UUID statusId = UUID.randomUUID();
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(userRefId, tenantId, "user");

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get(path()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(path()).contentType("application/json")
                        .with(csrf()).content(validJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postAssignsExactIdsAndDerivesTenantAndPrincipalFromJwt() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.assign(context, orderId, definitionId, versionId,
                stageId, statusId)).thenReturn(execution());

        mockMvc.perform(post(path()).with(authenticatedJwt())
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.workflowVersionId").value(versionId.toString()));

        verify(commandService).assign(context, orderId, definitionId, versionId,
                stageId, statusId);
    }

    @Test
    void postRejectsMissingRequiredFields() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        String[] jsons = {
                "{\"workflowVersionId\":\"" + versionId
                        + "\",\"initialStageId\":\"" + stageId
                        + "\",\"initialStatusId\":\"" + statusId + "\"}",
                "{\"workflowDefinitionId\":\"" + definitionId
                        + "\",\"initialStageId\":\"" + stageId
                        + "\",\"initialStatusId\":\"" + statusId + "\"}",
                "{\"workflowDefinitionId\":\"" + definitionId
                        + "\",\"workflowVersionId\":\"" + versionId
                        + "\",\"initialStatusId\":\"" + statusId + "\"}",
                "{\"workflowDefinitionId\":\"" + definitionId
                        + "\",\"workflowVersionId\":\"" + versionId
                        + "\",\"initialStageId\":\"" + stageId + "\"}"
        };
        for (String json : jsons) {
            mockMvc.perform(post(path()).with(authenticatedJwt())
                            .contentType("application/json").content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void postRejectsMalformedOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/aftersales/service-orders/not-a-uuid/workflow")
                        .with(authenticatedJwt()).contentType("application/json")
                        .content(validJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPropagatesAuthorizationDuplicateAndNotFoundFailures() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(commandService).assign(context, orderId, definitionId,
                        versionId, stageId, statusId);
        mockMvc.perform(post(path()).with(authenticatedJwt())
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.doThrow(new ResponseStatusException(CONFLICT, "duplicate"))
                .when(commandService).assign(context, orderId, definitionId,
                        versionId, stageId, statusId);
        mockMvc.perform(post(path()).with(authenticatedJwt())
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isConflict());

        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND, "missing"))
                .when(commandService).assign(context, orderId, definitionId,
                        versionId, stageId, statusId);
        mockMvc.perform(post(path()).with(authenticatedJwt())
                        .contentType("application/json").content(validJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReturnsPinnedRuntimeExecution() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.getForServiceOrder(context, orderId)).thenReturn(execution());

        mockMvc.perform(get(path()).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(executionId().toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.workflowDefinitionId").value(definitionId.toString()))
                .andExpect(jsonPath("$.workflowVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.currentStageId").value(stageId.toString()))
                .andExpect(jsonPath("$.currentStatusId").value(statusId.toString()));
    }

    @Test
    void getPropagatesAuthorizationAndMissingAssignmentFailures() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(readService).getForServiceOrder(context, orderId);
        mockMvc.perform(get(path()).with(authenticatedJwt()))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND, "missing"))
                .when(readService).getForServiceOrder(context, orderId);
        mockMvc.perform(get(path()).with(authenticatedJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRejectsMalformedOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/aftersales/service-orders/not-a-uuid/workflow")
                        .with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransitionExecutesAndReturnsOk() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.transition(context, orderId, transitionId))
                .thenReturn(movedExecution());

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.workflowVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.currentStageId").value(stageId.toString()));

        verify(commandService).transition(context, orderId, transitionId);
    }

    @Test
    void postTransitionForwardsExactOrderAndTransitionIds() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.transition(context, orderId, transitionId))
                .thenReturn(movedExecution());

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isOk());

        verify(commandService).transition(context, orderId, transitionId);
    }

    @Test
    void postTransitionResolvesTenantContextFromJwt() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.transition(context, orderId, transitionId))
                .thenReturn(movedExecution());

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isOk());

        verify(tenantContextResolver).resolve(any());
    }

    @Test
    void postTransitionResponseMapsUpdatedCurrentStageAndStatus() throws Exception {
        UUID transitionId = UUID.randomUUID();
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.transition(context, orderId, transitionId))
                .thenReturn(ServiceOrderWorkflowExecution.start(
                        executionId(), tenantId, orderId, definitionId, versionId,
                        stageId, statusId, userRefId,
                        OffsetDateTime.parse("2026-08-19T10:00:00Z")));

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStageId").value(stageId.toString()))
                .andExpect(jsonPath("$.currentStatusId").value(statusId.toString()));
    }

    @Test
    void postTransitionResponsePreservesPinnedWorkflowVersionAndServiceOrderId() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.transition(context, orderId, transitionId))
                .thenReturn(movedExecution());

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()));
    }

    @Test
    void postTransitionRejectsMalformedOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/aftersales/service-orders/not-a-uuid/workflow/transitions/"
                        + UUID.randomUUID())
                        .with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransitionRejectsMalformedTransitionId() throws Exception {
        mockMvc.perform(post("/api/v1/aftersales/service-orders/" + orderId
                        + "/workflow/transitions/not-a-uuid")
                        .with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransitionAuthorizationDenialReturnsForbidden() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postTransitionMissingServiceOrderReturnsNotFound() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND, "missing"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postTransitionMissingWorkflowExecutionReturnsNotFound() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND,
                        "workflow execution not found"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postTransitionMissingTransitionReturnsNotFound() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND,
                        "transition not found"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postTransitionInactiveTransitionOrTargetReturnsBadRequest() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "workflow transition is not active"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransitionFromStateMismatchReturnsConflict() throws Exception {
        UUID transitionId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        org.mockito.Mockito.doThrow(new ResponseStatusException(CONFLICT,
                        "workflow transition does not match the current execution state"))
                .when(commandService).transition(context, orderId, transitionId);

        mockMvc.perform(post(transitionPath(transitionId)).with(authenticatedJwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void postTransitionUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post(transitionPath(UUID.randomUUID())).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private String transitionPath(UUID transitionId) {
        return "/api/v1/aftersales/service-orders/" + orderId
                + "/workflow/transitions/" + transitionId;
    }

    private ServiceOrderWorkflowExecution movedExecution() {
        return ServiceOrderWorkflowExecution.start(
                executionId(), tenantId, orderId, definitionId, versionId,
                stageId, statusId, userRefId, OffsetDateTime.parse(
                        "2026-08-19T10:00:00Z"));
    }

    private String path() {
        return "/api/v1/aftersales/service-orders/" + orderId + "/workflow";
    }

    private RequestPostProcessor authenticatedJwt() {
        return jwt().jwt(token -> token.subject("test-subject")
                .claim("autovision_user_ref_id", userRefId.toString()));
    }

    private String validJson() {
        return "{\"workflowDefinitionId\":\"" + definitionId
                + "\",\"workflowVersionId\":\"" + versionId
                + "\",\"initialStageId\":\"" + stageId
                + "\",\"initialStatusId\":\"" + statusId + "\"}";
    }

    private UUID executionId() {
        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    private ServiceOrderWorkflowExecution execution() {
        return ServiceOrderWorkflowExecution.start(
                executionId(), tenantId, orderId, definitionId, versionId,
                stageId, statusId, userRefId, OffsetDateTime.parse(
                        "2026-08-19T10:00:00Z"));
    }
}