package com.autovision.platform.aftersales;

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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceJobAuthorizationReadinessControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private OperationalAuthorizationEvaluationService evaluationService;

    @MockitoBean
    private ServiceJobAuthorizationReadinessPolicy readinessPolicy;

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "operator"
            );

    @Test
    void mapsEveryReadinessStatusToTheExactFiveFieldResponse() throws Exception {
        assertResponse(
                OperationalAuthorizationStatus.NOT_REQUIRED,
                true,
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_NOT_REQUIRED
        );
        assertResponse(
                OperationalAuthorizationStatus.FULLY_AUTHORIZED,
                true,
                ServiceJobAuthorizationReadinessReason.FULLY_AUTHORIZED
        );
        assertResponse(
                OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED,
                false,
                ServiceJobAuthorizationReadinessReason.PARTIAL_AUTHORIZATION
        );
        assertResponse(
                OperationalAuthorizationStatus.PENDING,
                false,
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_PENDING
        );
        assertResponse(
                OperationalAuthorizationStatus.NOT_AUTHORIZED,
                false,
                ServiceJobAuthorizationReadinessReason.AUTHORIZATION_MISSING
        );
    }

    @Test
    void propagatesForbiddenEvaluationFailure() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isForbidden());
        verifyNoInteractions(readinessPolicy);
    }

    @Test
    void propagatesNotFoundEvaluationFailure() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Service job not found"
                ));

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isNotFound());
        verifyNoInteractions(readinessPolicy);
    }

    @Test
    void rejectsMalformedOrderIdBeforeEvaluatorInvocation() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/aftersales/service-orders/not-a-uuid/service-jobs/{jobId}/authorization-readiness", jobId)
                                .with(authenticatedJwt())
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluationService, readinessPolicy);
    }

    @Test
    void rejectsMalformedJobIdBeforeEvaluatorInvocation() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/aftersales/service-orders/{orderId}/service-jobs/not-a-uuid/authorization-readiness", orderId)
                                .with(authenticatedJwt())
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluationService, readinessPolicy);
    }

    @Test
    void passesExactEvaluationFromEvaluatorToPolicy() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        OperationalAuthorizationEvaluation evaluation = evaluation(
                jobId,
                orderId,
                OperationalAuthorizationStatus.FULLY_AUTHORIZED
        );
        ServiceJobAuthorizationReadiness readiness = new ServiceJobAuthorizationReadiness(
                jobId,
                orderId,
                true,
                OperationalAuthorizationStatus.FULLY_AUTHORIZED,
                ServiceJobAuthorizationReadinessReason.FULLY_AUTHORIZED
        );
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId))
                .thenReturn(evaluation);
        when(readinessPolicy.evaluate(evaluation)).thenReturn(readiness);

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isOk());

        verify(evaluationService).evaluate(context, orderId, jobId);
        verify(readinessPolicy).evaluate(evaluation);
    }

    private void assertResponse(
            OperationalAuthorizationStatus status,
            boolean ready,
            ServiceJobAuthorizationReadinessReason reason
    ) throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        OperationalAuthorizationEvaluation evaluation = evaluation(jobId, orderId, status);
        ServiceJobAuthorizationReadiness readiness = new ServiceJobAuthorizationReadiness(
                jobId,
                orderId,
                ready,
                status,
                reason
        );
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId)).thenReturn(evaluation);
        when(readinessPolicy.evaluate(evaluation)).thenReturn(readiness);

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceJobId").value(jobId.toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.ready").value(ready))
                .andExpect(jsonPath("$.operationalAuthorizationStatus").value(status.name()))
                .andExpect(jsonPath("$.reason").value(reason.name()))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.quoteId").doesNotExist())
                .andExpect(jsonPath("$.authorizationId").doesNotExist())
                .andExpect(jsonPath("$.counts").doesNotExist());
    }

    private OperationalAuthorizationEvaluation evaluation(
            UUID jobId,
            UUID orderId,
            OperationalAuthorizationStatus status
    ) {
        int total = status == OperationalAuthorizationStatus.NOT_REQUIRED ? 3 : 2;
        int authorized = status == OperationalAuthorizationStatus.FULLY_AUTHORIZED ? 2 : 0;
        int pending = status == OperationalAuthorizationStatus.PENDING ? total : 0;
        int notAuthorized = status == OperationalAuthorizationStatus.NOT_AUTHORIZED ? total : 0;
        if (status == OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED) {
            authorized = 1;
            pending = 1;
        }
        return new OperationalAuthorizationEvaluation(
                jobId,
                orderId,
                status,
                total,
                authorized,
                pending,
                notAuthorized
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            request(UUID orderId, UUID jobId) {
        return get(
                "/api/v1/aftersales/service-orders/{orderId}/service-jobs/{jobId}/authorization-readiness",
                orderId,
                jobId
        ).with(authenticatedJwt());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor
            authenticatedJwt() {
        return jwt().jwt(token -> token
                .subject("test-subject")
                .claim("autovision_user_ref_id", context.userRefId().toString()));
    }
}
