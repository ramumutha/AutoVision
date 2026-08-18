package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import tools.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceOrderCreateControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

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
    void createsServiceOrderAndReturnsMutationResponse()
            throws Exception {

        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        String orderNumber = "SO-REST-001";
        ServiceOrder order = order(
                dealerId,
                branchId,
                vehicleId,
                orderNumber
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderNumber,
                dealerId,
                branchId,
                vehicleId
        )).thenReturn(order);

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content(json(
                                orderNumber,
                                dealerId,
                                branchId,
                                vehicleId
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.getId().toString()))
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.dealerId").value(dealerId.toString()))
        .andExpect(jsonPath("$.branchId").value(branchId.toString()))
        .andExpect(jsonPath("$.orderNumber").value(orderNumber))
        .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
        .andExpect(jsonPath("$.status").value("OPEN"));

        verify(tenantContextResolver).resolve(any());
        verify(commandService).create(
                context,
                orderNumber,
                dealerId,
                branchId,
                vehicleId
        );
    }

    @Test
    void createsTenantScopedServiceOrderWithNullOrganizationIds()
            throws Exception {

        UUID vehicleId = UUID.randomUUID();
        String orderNumber = "SO-REST-TENANT-001";

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderNumber,
                null,
                null,
                vehicleId
        )).thenReturn(order(
                null,
                null,
                vehicleId,
                orderNumber
        ));

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content(json(
                                orderNumber,
                                null,
                                null,
                                vehicleId
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.dealerId").doesNotExist())
        .andExpect(jsonPath("$.branchId").doesNotExist());

        verify(commandService).create(
                context,
                orderNumber,
                null,
                null,
                vehicleId
        );
    }

    @Test
    void propagatesBadRequestFromCommandService()
            throws Exception {

        String orderNumber = "SO-REST-BAD-001";
        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        UUID branchId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(commandService.create(
                context,
                orderNumber,
                null,
                branchId,
                vehicleId
        )).thenThrow(new ResponseStatusException(
                BAD_REQUEST,
                "dealerId is required when branchId is provided"
        ));

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content(json(
                                orderNumber,
                                null,
                                branchId,
                                vehicleId
                        ))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void propagatesAuthorizationDenialAsForbidden()
            throws Exception {

        String orderNumber = "SO-REST-FORBIDDEN-001";
        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderNumber,
                null,
                null,
                null
        )).thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content(json(orderNumber, null, null, null))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void propagatesDuplicateOrderConflict()
            throws Exception {

        String orderNumber = "SO-REST-DUPLICATE-001";
        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(commandService.create(
                context,
                orderNumber,
                null,
                null,
                null
        )).thenThrow(new ResponseStatusException(
                CONFLICT,
                "Service order number already exists"
        ));

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(authenticatedJwt())
                        .contentType("application/json")
                        .content(json(orderNumber, null, null, null))
        )
        .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/service-orders")
                        .with(csrf())
                        .contentType("application/json")
                        .content(json(
                                "SO-REST-UNAUTH-001",
                                null,
                                null,
                                UUID.randomUUID()
                        ))
        )
        .andExpect(status().isUnauthorized());

        verify(commandService, never()).create(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private ServiceOrder order(
            UUID dealerId,
            UUID branchId,
            UUID vehicleId,
            String orderNumber
    ) {
        return ServiceOrder.open(
                UUID.randomUUID(),
                tenantId,
                dealerId,
                branchId,
                orderNumber,
                vehicleId,
                userRefId,
                OffsetDateTime.now()
        );
    }

    private String json(
            String orderNumber,
            UUID dealerId,
            UUID branchId,
            UUID vehicleId
    ) throws Exception {
        return objectMapper.writeValueAsString(
                new CreateServiceOrderRequest(
                        orderNumber,
                        dealerId,
                        branchId,
                        vehicleId
                )
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