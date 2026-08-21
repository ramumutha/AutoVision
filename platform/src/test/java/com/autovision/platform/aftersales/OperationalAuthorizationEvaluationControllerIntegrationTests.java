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
class OperationalAuthorizationEvaluationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private OperationalAuthorizationEvaluationService evaluationService;

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "operator"
            );

    @Test
    void returnsFullyAuthorizedResponseWithExactContract() throws Exception {
        assertResponse(
                new OperationalAuthorizationEvaluation(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OperationalAuthorizationStatus.FULLY_AUTHORIZED,
                        3,
                        3,
                        0,
                        0
                )
        );
    }

    @Test
    void mapsAllOperationalStatusesAndCounts() throws Exception {
        assertResponse(evaluation(OperationalAuthorizationStatus.PARTIALLY_AUTHORIZED, 4, 2, 1, 1));
        assertResponse(evaluation(OperationalAuthorizationStatus.PENDING, 2, 0, 2, 0));
        assertResponse(evaluation(OperationalAuthorizationStatus.NOT_AUTHORIZED, 2, 0, 0, 2));
        assertResponse(evaluation(OperationalAuthorizationStatus.NOT_REQUIRED, 5, 0, 0, 0));
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
    }

    @Test
    void rejectsMalformedOrderIdBeforeEvaluatorCall() throws Exception {
        UUID jobId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/aftersales/service-orders/not-a-uuid/service-jobs/{jobId}/authorization-evaluation", jobId)
                                .with(authenticatedJwt())
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluationService);
    }

    @Test
    void rejectsMalformedJobIdBeforeEvaluatorCall() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/aftersales/service-orders/{orderId}/service-jobs/not-a-uuid/authorization-evaluation", orderId)
                                .with(authenticatedJwt())
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(evaluationService);
    }

    @Test
    void delegatesTenantContextAndBothResourceIdsExactly() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        OperationalAuthorizationEvaluation evaluation =
                evaluation(OperationalAuthorizationStatus.PENDING, 1, 0, 1, 0);
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId))
                .thenReturn(evaluation);

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isOk());

        verify(tenantContextResolver).resolve(any());
        verify(evaluationService).evaluate(context, orderId, jobId);
    }

    private void assertResponse(
            OperationalAuthorizationEvaluation evaluation
    ) throws Exception {
        UUID orderId = evaluation.serviceOrderId();
        UUID jobId = evaluation.serviceJobId();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(evaluationService.evaluate(context, orderId, jobId))
                .thenReturn(evaluation);

        mockMvc.perform(request(orderId, jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceJobId").value(jobId.toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value(evaluation.status().name()))
                .andExpect(jsonPath("$.totalLineCount").value(evaluation.totalLineCount()))
                .andExpect(jsonPath("$.authorizedLineCount").value(evaluation.authorizedLineCount()))
                .andExpect(jsonPath("$.pendingLineCount").value(evaluation.pendingLineCount()))
                .andExpect(jsonPath("$.notAuthorizedLineCount").value(evaluation.notAuthorizedLineCount()))
                .andExpect(jsonPath("$.quoteId").doesNotExist())
                .andExpect(jsonPath("$.authorizationId").doesNotExist())
                .andExpect(jsonPath("$.serviceLineIds").doesNotExist())
                .andExpect(jsonPath("$.explanation").doesNotExist());
    }

    private OperationalAuthorizationEvaluation evaluation(
            OperationalAuthorizationStatus status,
            int total,
            int authorized,
            int pending,
            int notAuthorized
    ) {
        return new OperationalAuthorizationEvaluation(
                UUID.randomUUID(),
                UUID.randomUUID(),
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
                "/api/v1/aftersales/service-orders/{orderId}/service-jobs/{jobId}/authorization-evaluation",
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
