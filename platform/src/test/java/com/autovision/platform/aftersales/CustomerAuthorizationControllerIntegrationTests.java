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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerAuthorizationControllerIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantContextResolver tenantContextResolver;

    @MockitoBean
    private CustomerAuthorizationService service;

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
    void listRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        UUID.randomUUID()
                )
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void listRejectsAuthenticatedRequestWithoutPermission()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findAllForCase(context, caseId))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        caseId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void listReturnsCustomerAuthorizationsForCase()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findAllForCase(context, caseId))
                .thenReturn(List.of(
                        authorizationRecord(
                                authorizationId,
                                caseId,
                                "AUTH-4001"
                        )
                ));

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        caseId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$[0].id")
                        .value(authorizationId.toString())
        )
        .andExpect(
                jsonPath("$[0].aftersalesCaseId")
                        .value(caseId.toString())
        )
        .andExpect(
                jsonPath("$[0].authorizationNumber")
                        .value("AUTH-4001")
        )
        .andExpect(
                jsonPath("$[0].authorizationStatus")
                        .value("REQUESTED")
        );
    }

    @Test
    void detailReturnsCustomerAuthorization()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findById(
                context,
                caseId,
                authorizationId
        )).thenReturn(
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4002"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.id")
                        .value(authorizationId.toString())
        )
        .andExpect(
                jsonPath("$.authorizationNumber")
                        .value("AUTH-4002")
        );
    }

    @Test
    void requestCreatesCustomerAuthorization()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        JsonNode scopeSnapshot =
                json("""
                        {
                          "scopeVersion": 1,
                          "items": []
                        }
                        """);

        JsonNode commercialSnapshot =
                json("""
                        {
                          "currency": "INR",
                          "total": 10030.00
                        }
                        """);

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.request(
                context,
                caseId,
                null,
                "AUTH-4003",
                "CUSTOMER-1",
                "Test Customer",
                "Authorize brake work",
                scopeSnapshot,
                commercialSnapshot,
                "Applicable terms",
                "Applicable disclaimer"
        )).thenReturn(
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4003"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        caseId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "AUTH-4003",
                          "customerReference": "CUSTOMER-1",
                          "customerDisplayNameSnapshot": "Test Customer",
                          "authorizationSummary": "Authorize brake work",
                          "authorizationScopeSnapshot": {
                            "scopeVersion": 1,
                            "items": []
                          },
                          "commercialSnapshot": {
                            "currency": "INR",
                            "total": 10030.00
                          },
                          "termsSnapshot": "Applicable terms",
                          "disclaimerSnapshot": "Applicable disclaimer"
                        }
                        """)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.authorizationNumber")
                        .value("AUTH-4003")
        )
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("REQUESTED")
        );
    }

    @Test
    void requestReturnsLinkedServiceQuoteId() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.request(
                context,
                caseId,
                serviceQuoteId,
                "AUTH-4003-QUOTE",
                null,
                null,
                "Authorize quoted work",
                json("""
                        {
                          "scopeVersion": 1,
                          "items": []
                        }
                        """),
                null,
                null,
                null
        )).thenReturn(
                CustomerAuthorization.request(
                        authorizationId,
                        tenantId,
                        null,
                        null,
                        caseId,
                        serviceQuoteId,
                        "AUTH-4003-QUOTE",
                        null,
                        null,
                        "Authorize quoted work",
                        json("""
                                {
                                  "scopeVersion": 1,
                                  "items": []
                                }
                                """),
                        null,
                        null,
                        null,
                        userRefId,
                        OffsetDateTime.now()
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        caseId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "AUTH-4003-QUOTE",
                          "authorizationSummary": "Authorize quoted work",
                          "authorizationScopeSnapshot": {
                            "scopeVersion": 1,
                            "items": []
                          },
                          "serviceQuoteId": "%s"
                        }
                        """.formatted(serviceQuoteId))
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.serviceQuoteId")
                        .value(serviceQuoteId.toString())
        );
    }

    @Test
    void quoteAuthorizationHistoryUsesExactQuoteScopedRoute()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(service.findAllForServiceQuote(context, caseId, quoteId))
                .thenReturn(List.of(
                        authorizationRecord(
                                authorizationId,
                                caseId,
                                "AUTH-HISTORY-1"
                        )
                ));

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/service-quotes/{quoteId}/customer-authorizations",
                        caseId,
                        quoteId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id")
                .value(authorizationId.toString()))
        .andExpect(jsonPath("$[0].authorizationNumber")
                .value("AUTH-HISTORY-1"));
    }

    @Test
    void quoteAuthorizationHistoryReturnsEmptyList() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(service.findAllForServiceQuote(context, caseId, quoteId))
                .thenReturn(List.of());

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/service-quotes/{quoteId}/customer-authorizations",
                        caseId,
                        quoteId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void quoteRequestEndpointDoesNotAcceptServerGeneratedEvidenceFields()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);
        when(service.requestFromServiceQuote(
                context,
                caseId,
                quoteId,
                "AUTH-QUOTE-VALIDATION",
                null,
                null,
                "Authorize quoted work"
        )).thenReturn(
                authorizationRecord(
                        UUID.randomUUID(),
                        caseId,
                        "AUTH-QUOTE-VALIDATION"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/service-quotes/{quoteId}/customer-authorization",
                        caseId,
                        quoteId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "AUTH-QUOTE-VALIDATION",
                          "authorizationSummary": "Authorize quoted work",
                          "serviceQuoteId": "00000000-0000-0000-0000-000000000001",
                          "authorizationScopeSnapshot": {},
                          "commercialSnapshot": {},
                          "termsSnapshot": "client supplied terms",
                          "disclaimerSnapshot": "client supplied disclaimer"
                        }
                        """)
        )
        .andExpect(status().isCreated());
    }

    @Test
    void requestRejectsBlankAuthorizationNumber()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": " ",
                          "authorizationSummary": "Authorize work",
                          "authorizationScopeSnapshot": {
                            "scopeVersion": 1,
                            "items": []
                          }
                        }
                        """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsMissingAuthorizationSummary()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "AUTH-INVALID",
                          "authorizationScopeSnapshot": {
                            "scopeVersion": 1,
                            "items": []
                          }
                        }
                        """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsMissingAuthorizationScopeSnapshot()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "AUTH-INVALID",
                          "authorizationSummary": "Authorize work"
                        }
                        """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void authorizeReturnsAuthorizedDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4004"
                );

        authorization.authorize(
                "CUSTOMER_PORTAL",
                "decision-4004",
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-4004"
        )).thenReturn(authorization);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/authorize",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "CUSTOMER_PORTAL",
                          "decisionReference": "decision-4004"
                        }
                        """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("AUTHORIZED")
        )
        .andExpect(
                jsonPath("$.decisionChannel")
                        .value("CUSTOMER_PORTAL")
        )
        .andExpect(
                jsonPath("$.decisionReference")
                        .value("decision-4004")
        );
    }

    @Test
    void declineReturnsDeclinedDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4005"
                );

        authorization.decline(
                "EMAIL",
                "decision-4005",
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.decline(
                context,
                caseId,
                authorizationId,
                "EMAIL",
                "decision-4005"
        )).thenReturn(authorization);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/decline",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "EMAIL",
                          "decisionReference": "decision-4005"
                        }
                        """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("DECLINED")
        );
    }

    @Test
    void deferReturnsDeferredDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4006"
                );

        authorization.defer(
                "PHONE",
                "decision-4006",
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.defer(
                context,
                caseId,
                authorizationId,
                "PHONE",
                "decision-4006"
        )).thenReturn(authorization);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/defer",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "PHONE",
                          "decisionReference": "decision-4006"
                        }
                        """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("DEFERRED")
        );
    }

    @Test
    void cancelReturnsCancelledDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId,
                        "AUTH-4007"
                );

        authorization.cancel(
                "SERVICE_ADVISOR",
                "decision-4007",
                userRefId,
                OffsetDateTime.now()
        );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.cancel(
                context,
                caseId,
                authorizationId,
                "SERVICE_ADVISOR",
                "decision-4007"
        )).thenReturn(authorization);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/cancel",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "SERVICE_ADVISOR",
                          "decisionReference": "decision-4007"
                        }
                        """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("CANCELLED")
        );
    }

    @Test
    void detailRejectsMalformedCaseId()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}",
                        "not-a-uuid",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void detailRejectsMalformedAuthorizationId()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}",
                        UUID.randomUUID(),
                        "not-a-uuid"
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void requestRejectsOversizedAuthorizationNumber()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        String oversizedAuthorizationNumber =
                "A".repeat(81);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations",
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "authorizationNumber": "%s",
                          "authorizationSummary": "Authorize work",
                          "authorizationScopeSnapshot": {
                            "scopeVersion": 1,
                            "items": []
                          }
                        }
                        """.formatted(oversizedAuthorizationNumber))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void decisionRejectsOversizedDecisionChannel()
            throws Exception {

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        String oversizedDecisionChannel =
                "C".repeat(33);

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/authorize",
                        UUID.randomUUID(),
                        UUID.randomUUID()
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "%s",
                          "decisionReference": "decision-invalid"
                        }
                        """.formatted(oversizedDecisionChannel))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void authorizeRejectsAuthenticatedRequestWithoutPermission()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-denied"
        )).thenThrow(
                new AccessDeniedException("Access is denied")
        );

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/authorize",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "CUSTOMER_PORTAL",
                          "decisionReference": "decision-denied"
                        }
                        """)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void detailPropagatesNotFoundFromService()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findById(
                context,
                caseId,
                authorizationId
        )).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Customer authorization not found"
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void authorizePropagatesConflictFromService()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-repeat"
        )).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Customer authorization has already been decided"
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}/authorize",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
                .contentType("application/json")
                .content("""
                        {
                          "decisionChannel": "CUSTOMER_PORTAL",
                          "decisionReference": "decision-repeat"
                        }
                        """)
        )
        .andExpect(status().isConflict());
    }

    @Test
    void detailReturnsSnapshotsAndServerOwnedAuditFields()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        JsonNode scopeSnapshot =
                json("""
                        {
                          "scopeVersion": 1,
                          "items": [
                            {
                              "reference": "BRAKE-1",
                              "description": "Front brake service"
                            }
                          ]
                        }
                        """);

        JsonNode commercialSnapshot =
                json("""
                        {
                          "currency": "INR",
                          "subtotal": 8500.00,
                          "tax": 1530.00,
                          "total": 10030.00
                        }
                        """);

        CustomerAuthorization authorization =
                CustomerAuthorization.request(
                        authorizationId,
                        tenantId,
                        dealerId,
                        branchId,
                        caseId,
                        "AUTH-4008",
                        "CUSTOMER-TEST",
                        "Test Customer",
                        "Authorize brake work",
                        scopeSnapshot,
                        commercialSnapshot,
                        "Applicable terms snapshot",
                        "Applicable disclaimer snapshot",
                        userRefId,
                        OffsetDateTime.now()
                );

        when(tenantContextResolver.resolve(any()))
                .thenReturn(context);

        when(service.findById(
                context,
                caseId,
                authorizationId
        )).thenReturn(authorization);

        mockMvc.perform(
                get(
                        "/api/v1/aftersales-cases/{caseId}/customer-authorizations/{authorizationId}",
                        caseId,
                        authorizationId
                )
                .with(authenticatedJwt())
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.tenantId")
                        .value(tenantId.toString())
        )
        .andExpect(
                jsonPath("$.dealerId")
                        .value(dealerId.toString())
        )
        .andExpect(
                jsonPath("$.branchId")
                        .value(branchId.toString())
        )
        .andExpect(
                jsonPath("$.authorizationStatus")
                        .value("REQUESTED")
        )
        .andExpect(
                jsonPath("$.createdByPrincipalId")
                        .value(userRefId.toString())
        )
        .andExpect(
                jsonPath("$.updatedByPrincipalId")
                        .value(userRefId.toString())
        )
        .andExpect(
                jsonPath("$.authorizationScopeSnapshot.scopeVersion")
                        .value(1)
        )
        .andExpect(
                jsonPath("$.authorizationScopeSnapshot.items[0].reference")
                        .value("BRAKE-1")
        )
        .andExpect(
                jsonPath("$.commercialSnapshot.currency")
                        .value("INR")
        )
        .andExpect(
                jsonPath("$.commercialSnapshot.total")
                        .value(10030.00)
        )
        .andExpect(
                jsonPath("$.termsSnapshot")
                        .value("Applicable terms snapshot")
        )
        .andExpect(
                jsonPath("$.disclaimerSnapshot")
                        .value("Applicable disclaimer snapshot")
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

    private CustomerAuthorization authorizationRecord(
            UUID authorizationId,
            UUID caseId,
            String authorizationNumber
    ) throws Exception {

        return CustomerAuthorization.request(
                authorizationId,
                tenantId,
                null,
                null,
                caseId,
                authorizationNumber,
                "CUSTOMER-TEST",
                "Test Customer",
                "Test authorization",
                json("""
                        {
                          "scopeVersion": 1,
                          "items": []
                        }
                        """),
                null,
                null,
                null,
                userRefId,
                OffsetDateTime.now()
        );
    }

    private JsonNode json(String value) throws Exception {
        return OBJECT_MAPPER.readTree(value);
    }
}
