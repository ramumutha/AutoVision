package com.autovision.platform.aftersales;

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
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrderMutationControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private ServiceOrderAccessService accessService;

    @MockitoBean
    private ServiceOrderCommandService commandService;

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
    void startRejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        UUID.randomUUID()
                )
                .with(csrf())
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void startReturnsUpdatedServiceOrder()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        ServiceOrder order = order(orderId);

        order.start(
                new DefaultServiceLifecyclePolicy(),
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId
        )).thenReturn(order);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(orderId.toString())
        )
        .andExpect(
                jsonPath("$.tenantId")
                        .value(tenantId.toString())
        )
        .andExpect(
                jsonPath("$.status")
                        .value("IN_PROGRESS")
        )
        .andExpect(
                jsonPath("$.updatedByPrincipalId")
                        .value(userRefId.toString())
        );

        verify(commandService)
                .start(
                        context,
                        orderId
                );
    }

    @Test
    void cancelReturnsCancelledServiceOrder()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        ServiceOrder order = order(orderId);

        order.cancel(
                new DefaultServiceLifecyclePolicy(),
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.cancel(
                context,
                orderId
        )).thenReturn(order);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/cancel",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("CANCELLED")
        );
    }

    @Test
    void completeWorkReturnsCompletedServiceOrder()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        ServiceOrder order = order(orderId);

        order.completeWork(
                new DefaultServiceLifecyclePolicy(),
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.completeWork(
                context,
                orderId
        )).thenReturn(order);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/complete-work",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("WORK_COMPLETED")
        );
    }

    @Test
    void closeReturnsClosedServiceOrder()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        ServiceOrder order = order(orderId);
        ServiceLifecyclePolicy policy =
                new DefaultServiceLifecyclePolicy();

        order.completeWork(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        order.close(
                policy,
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.close(
                context,
                orderId
        )).thenReturn(order);

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/close",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.status")
                        .value("CLOSED")
        );
    }

    @Test
    void mutationRejectsAuthenticatedRequestWithoutAuthorizedScope()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId
        )).thenThrow(
                new AccessDeniedException(
                        "Access is denied"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mutationPropagatesNotFound()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId
        )).thenThrow(
                new ResponseStatusException(
                        NOT_FOUND,
                        "Service order not found"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void mutationRejectsMalformedOrderId()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void invalidLifecycleTransitionReturnsConflict()
            throws Exception {

        UUID orderId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(commandService.start(
                context,
                orderId
        )).thenThrow(
                new IllegalStateException(
                        "Service order transition is not allowed"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/service-orders/{orderId}/start",
                        orderId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isConflict());
    }

    private ServiceOrder order(
            UUID orderId
    ) {
        return ServiceOrder.open(
                orderId,
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SO-" + orderId,
                UUID.randomUUID(),
                userRefId,
                OffsetDateTime.now()
        );
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