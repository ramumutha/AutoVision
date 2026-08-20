package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import com.autovision.platform.tenant.TenantContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceQuoteControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TenantContextResolver tenantContextResolver;
    @MockitoBean private ServiceQuoteCommandService commandService;
    @MockitoBean private ServiceQuoteReadService readService;

    private final AuthenticatedTenantContext context = new AuthenticatedTenantContext(
            UUID.randomUUID(), UUID.randomUUID(), "operator");

    @Test
    void postCreatesQuoteAndReturnsSummaryWithoutSecondaryReadAuthorization()
            throws Exception {
        UUID orderId = UUID.randomUUID();
        ServiceQuote quote = quote(orderId, "Q-REST");
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.create(context, orderId, "Q-REST", "EUR", null,
                "Terms", "Disclaimer")).thenReturn(quote);

        mockMvc.perform(post("/api/v1/aftersales/service-orders/{orderId}/quotes", orderId)
                        .with(authenticatedJwt()).with(csrf())
                        .contentType("application/json")
                        .content(json("Q-REST", "EUR", "Terms", "Disclaimer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(quote.getId().toString()))
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.quoteNumber").value("Q-REST"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currencyCode").value("EUR"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(commandService).create(context, orderId, "Q-REST", "EUR", null,
                "Terms", "Disclaimer");
        verifyNoInteractions(readService);
    }

    @Test
    void postRejectsBlankRequiredFields() throws Exception {
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        mockMvc.perform(post("/api/v1/aftersales/service-orders/{orderId}/quotes", UUID.randomUUID())
                        .with(authenticatedJwt()).with(csrf())
                        .contentType("application/json")
                        .content(json(" ", " ", null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAndDetailMapResponses() throws Exception {
        UUID orderId = UUID.randomUUID();
        ServiceQuote quote = quote(orderId, "Q-LIST");
        ServiceQuoteLine line = line(quote);
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(readService.listForServiceOrder(context, orderId)).thenReturn(List.of(quote));
        when(readService.getForServiceOrder(context, orderId, quote.getId())).thenReturn(quote);
        when(readService.linesFor(context, orderId, quote.getId())).thenReturn(List.of(line));

        mockMvc.perform(get("/api/v1/aftersales/service-orders/{orderId}/quotes", orderId)
                        .with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quoteNumber").value("Q-LIST"));

        mockMvc.perform(get("/api/v1/aftersales/service-orders/{orderId}/quotes/{quoteId}",
                        orderId, quote.getId()).with(authenticatedJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteNumber").value("Q-LIST"))
                .andExpect(jsonPath("$.lines[0].sequence").value(0));
    }

    @Test
    void commandConflictIsReturnedAsConflict() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(tenantContextResolver.resolve(any())).thenReturn(context);
        when(commandService.create(context, orderId, "Q-DUP", "EUR", null, null, null))
                .thenThrow(new ResponseStatusException(CONFLICT, "duplicate"));

        mockMvc.perform(post("/api/v1/aftersales/service-orders/{orderId}/quotes", orderId)
                        .with(authenticatedJwt()).with(csrf())
                        .contentType("application/json")
                        .content(json("Q-DUP", "EUR", null, null)))
                .andExpect(status().isConflict());
    }

    private String json(String quoteNumber, String currency, String terms, String disclaimer)
            throws Exception {
        return objectMapper.writeValueAsString(new CreateServiceQuoteRequest(
                quoteNumber, currency, null, terms, disclaimer));
    }

    private ServiceQuote quote(UUID orderId, String number) {
        return ServiceQuote.create(UUID.randomUUID(), context.tenantId(), null, null, null,
                orderId, number, "EUR", null, "Terms", "Disclaimer",
                context.userRefId(), OffsetDateTime.now());
    }

    private ServiceQuoteLine line(ServiceQuote quote) {
        return ServiceQuoteLine.create(UUID.randomUUID(), quote.getId(), UUID.randomUUID(), null,
                "Labor", BigDecimal.ONE, BigDecimal.TEN, "EUR", BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.TEN, 0, context.userRefId(), OffsetDateTime.now());
    }

        private org.springframework.test.web.servlet.request.RequestPostProcessor
                        authenticatedJwt() {
                return jwt().jwt(token -> token
                                .subject("test-subject")
                                .claim("autovision_user_ref_id", context.userRefId().toString()));
    }
}