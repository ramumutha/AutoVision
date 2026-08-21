package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerAuthorizationServiceTests {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private final CustomerAuthorizationRepository repository =
            mock(CustomerAuthorizationRepository.class);

    private final AfterSalesCaseRepository caseRepository =
            mock(AfterSalesCaseRepository.class);

    private final ServiceQuoteRepository quoteRepository =
            mock(ServiceQuoteRepository.class);

    private final ServiceQuoteLineRepository quoteLineRepository =
            mock(ServiceQuoteLineRepository.class);

    private final ServiceQuoteAuthorizationSnapshotFactory snapshotFactory =
            new ServiceQuoteAuthorizationSnapshotFactory(OBJECT_MAPPER);

    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final CustomerAuthorizationService service =
            new CustomerAuthorizationService(
                    repository,
                    caseRepository,
                    quoteRepository,
                    quoteLineRepository,
                    snapshotFactory,
                    authorizationService
            );

    private final UUID tenantId =
            UUID.fromString(
                    "2cf85fea-bc61-4405-be50-00a0ca45df3b"
            );

    private final UUID principalId =
            UUID.fromString(
                    "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
            );

    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    principalId,
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void requestsAuthorizationUsingCaseContainmentAndAuditContext()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        AfterSalesCase afterSalesCase =
                caseRecord(
                        caseId,
                        dealerId,
                        branchId
                );

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.of(afterSalesCase));

        when(repository.save(any(CustomerAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAuthorization result =
                service.request(
                        context,
                        caseId,
                        "AUTH-2001",
                        "CUSTOMER-1",
                        "Test Customer",
                        "Authorize brake work",
                        json("""
                                {
                                  "scopeVersion": 1,
                                  "items": []
                                }
                                """),
                        json("""
                                {
                                  "currency": "INR",
                                  "total": 10030.00
                                }
                                """),
                        "Applicable commercial terms",
                        "Applicable dealer disclaimer"
                );

        assertNotNull(result.getId());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(caseId, result.getAftersalesCaseId());
        assertEquals(dealerId, result.getDealerId());
        assertEquals(branchId, result.getBranchId());
        assertEquals(
                CustomerAuthorizationStatus.REQUESTED,
                result.getAuthorizationStatus()
        );
        assertEquals(
                principalId,
                result.getCreatedByPrincipalId()
        );
        assertNull(result.getServiceQuoteId());

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_CREATE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    @Test
    void deniesRequestBeforeCaseOrAuthorizationRepositoryAccess()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        AuthorizationRequest request =
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_CREATE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                );

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(request);

        assertThrows(
                AccessDeniedException.class,
                () -> service.request(
                        context,
                        caseId,
                        "AUTH-DENIED",
                        null,
                        null,
                        "Denied request",
                        json("""
                                {
                                  "scopeVersion": 1,
                                  "items": []
                                }
                                """),
                        null,
                        null,
                        null
                )
        );

        verifyNoInteractions(caseRepository);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsDuplicateAuthorizationNumberWithinTenant()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.of(
                caseRecord(caseId, null, null)
        ));

        when(repository.existsByTenantIdAndAuthorizationNumber(
                tenantId,
                "AUTH-DUPLICATE"
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.request(
                                context,
                                caseId,
                                "AUTH-DUPLICATE",
                                null,
                                null,
                                "Duplicate authorization",
                                json("""
                                        {
                                          "scopeVersion": 1,
                                          "items": []
                                        }
                                        """),
                                null,
                                null,
                                null
                        )
                );

        assertEquals(409, exception.getStatusCode().value());

        verify(repository, never())
                .save(any(CustomerAuthorization.class));
    }

    @Test
    void authorizesRequestedAuthorization() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        CustomerAuthorization result =
                service.authorize(
                        context,
                        caseId,
                        authorizationId,
                        "CUSTOMER_PORTAL",
                        "decision-1001"
                );

        assertSame(authorization, result);
        assertEquals(
                CustomerAuthorizationStatus.AUTHORIZED,
                result.getAuthorizationStatus()
        );
        assertNotNull(result.getDecidedAt());
        assertEquals(
                "CUSTOMER_PORTAL",
                result.getDecisionChannel()
        );
        assertEquals(
                "decision-1001",
                result.getDecisionReference()
        );
        assertEquals(
                principalId,
                result.getUpdatedByPrincipalId()
        );

        verifyDecisionPermission(caseId);
    }

    @Test
    void declinesRequestedAuthorization() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        CustomerAuthorization result =
                service.decline(
                        context,
                        caseId,
                        authorizationId,
                        "EMAIL",
                        "reply-2001"
                );

        assertEquals(
                CustomerAuthorizationStatus.DECLINED,
                result.getAuthorizationStatus()
        );
        assertNotNull(result.getDecidedAt());
    }

    @Test
    void defersRequestedAuthorization() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        CustomerAuthorization result =
                service.defer(
                        context,
                        caseId,
                        authorizationId,
                        "PHONE",
                        "call-3001"
                );

        assertEquals(
                CustomerAuthorizationStatus.DEFERRED,
                result.getAuthorizationStatus()
        );
        assertNotNull(result.getDecidedAt());
    }

    @Test
    void cancelsRequestedAuthorization() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        CustomerAuthorization result =
                service.cancel(
                        context,
                        caseId,
                        authorizationId,
                        "SERVICE_ADVISOR",
                        "cancel-4001"
                );

        assertEquals(
                CustomerAuthorizationStatus.CANCELLED,
                result.getAuthorizationStatus()
        );
        assertNotNull(result.getDecidedAt());
    }

    @Test
    void rejectsSecondDecisionWithoutOverwritingOriginalDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-original"
        );

        OffsetDateTime originalDecidedAt =
                authorization.getDecidedAt();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.decline(
                                context,
                                caseId,
                                authorizationId,
                                "EMAIL",
                                "decision-overwrite"
                        )
                );

        assertEquals(409, exception.getStatusCode().value());

        assertEquals(
                CustomerAuthorizationStatus.AUTHORIZED,
                authorization.getAuthorizationStatus()
        );
        assertEquals(
                "CUSTOMER_PORTAL",
                authorization.getDecisionChannel()
        );
        assertEquals(
                "decision-original",
                authorization.getDecisionReference()
        );
        assertEquals(
                originalDecidedAt,
                authorization.getDecidedAt()
        );
    }

    @Test
    void returnsNotFoundWhenAuthorizationDoesNotBelongToRequestedCase()
            throws Exception {

        UUID requestedCaseId = UUID.randomUUID();
        UUID actualCaseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                requestedCaseId,
                tenantId
        )).thenReturn(Optional.of(
                caseRecord(requestedCaseId, null, null)
        ));

        when(repository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.of(
                authorizationRecord(
                        authorizationId,
                        actualCaseId
                )
        ));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.authorize(
                                context,
                                requestedCaseId,
                                authorizationId,
                                "CUSTOMER_PORTAL",
                                "decision-5001"
                        )
                );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void decisionAuthorizationOccursBeforeRepositoryAccess() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        AuthorizationRequest request =
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_DECIDE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                );

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(request);

        assertThrows(
                AccessDeniedException.class,
                () -> service.authorize(
                        context,
                        caseId,
                        authorizationId,
                        "CUSTOMER_PORTAL",
                        "decision-denied"
                )
        );

        verifyNoInteractions(caseRepository);
        verifyNoInteractions(repository);
    }

    @Test
    void deniesReadBeforeAnyPersistenceAccess() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        AuthorizationRequest request =
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                );

        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(request);

        assertThrows(
                AccessDeniedException.class,
                () -> service.findById(
                        context,
                        caseId,
                        authorizationId
                )
        );

        verifyNoInteractions(caseRepository);
        verifyNoInteractions(repository);
    }

    @Test
    void returnsNotFoundWhenParentCaseIsMissingBeforeAuthorizationLookup() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(
                                context,
                                caseId,
                                authorizationId
                        )
                );

        assertEquals(404, exception.getStatusCode().value());

        verify(repository, never())
                .findByIdAndTenantId(
                        any(UUID.class),
                        any(UUID.class)
                );
    }

    @Test
    void requestsAuthorizationWithSameTenantAndCaseQuote() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(caseRecord(caseId, null, null)));
        when(quoteRepository.findByIdAndTenantId(serviceQuoteId, tenantId))
                .thenReturn(Optional.of(quoteRecord(
                        serviceQuoteId,
                        tenantId,
                        caseId
                )));
        when(repository.save(any(CustomerAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAuthorization result = service.request(
                context,
                caseId,
                serviceQuoteId,
                "AUTH-QUOTE-1",
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
                "Terms",
                "Disclaimer"
        );

        assertEquals(serviceQuoteId, result.getServiceQuoteId());
        verify(repository).save(any(CustomerAuthorization.class));
    }

    @Test
    void rejectsUnknownServiceQuoteWithoutLeakingTenantInformation()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(serviceQuoteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestWithQuote(caseId, serviceQuoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
        verify(repository, never()).save(any(CustomerAuthorization.class));
    }

    @Test
    void rejectsServiceQuoteFromAnotherCaseWithoutLeakingItsIdentity()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();
        UUID otherCaseId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(serviceQuoteId, tenantId))
                .thenReturn(Optional.of(quoteRecord(
                        serviceQuoteId,
                        tenantId,
                        otherCaseId
                )));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestWithQuote(caseId, serviceQuoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
        verify(repository, never()).save(any(CustomerAuthorization.class));
    }

    @Test
    void rejectsCrossTenantServiceQuoteWithoutLeakingTenantInformation()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(serviceQuoteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestWithQuote(caseId, serviceQuoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
        verify(repository, never()).save(any(CustomerAuthorization.class));
    }

    @Test
    void rejectsServiceQuoteWithoutAfterSalesCase() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID serviceQuoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(serviceQuoteId, tenantId))
                .thenReturn(Optional.of(quoteRecord(
                        serviceQuoteId,
                        tenantId,
                        null
                )));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestWithQuote(caseId, serviceQuoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
        verify(repository, never()).save(any(CustomerAuthorization.class));
    }

    @Test
    void issuedQuoteCreatesAuthorizationWithDeterministicEvidence()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID firstLineId = UUID.randomUUID();
        UUID secondLineId = UUID.randomUUID();
        UUID firstServiceLineId = UUID.randomUUID();
        UUID secondServiceLineId = UUID.randomUUID();
        UUID serviceJobId = UUID.randomUUID();

        ServiceQuote quote = quoteRecord(
                quoteId,
                tenantId,
                caseId,
                serviceOrderId,
                "QUOTE-ISSUED-1"
        );
        quote.issue(principalId, OffsetDateTime.now());

        ServiceQuoteLine firstLine = ServiceQuoteLine.create(
                firstLineId,
                quoteId,
                firstServiceLineId,
                null,
                "Front brake pad",
                new BigDecimal("2.0000"),
                new BigDecimal("125.1250"),
                "INR",
                new BigDecimal("250.2500"),
                new BigDecimal("45.0450"),
                new BigDecimal("295.2950"),
                1,
                principalId,
                OffsetDateTime.now()
        );
        ServiceQuoteLine secondLine = ServiceQuoteLine.create(
                secondLineId,
                quoteId,
                secondServiceLineId,
                serviceJobId,
                "Brake inspection",
                new BigDecimal("1.0000"),
                new BigDecimal("99.9999"),
                "INR",
                new BigDecimal("99.9999"),
                new BigDecimal("18.0000"),
                new BigDecimal("117.9999"),
                2,
                principalId,
                OffsetDateTime.now()
        );

        prepareRequestCase(caseId);
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quote));
        when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quoteId))
                .thenReturn(List.of(firstLine, secondLine));
        when(repository.save(any(CustomerAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAuthorization result = service.requestFromServiceQuote(
                context,
                caseId,
                quoteId,
                "AUTH-FROM-QUOTE-1",
                "CUSTOMER-1",
                "Test Customer",
                "Authorize quoted work"
        );

        assertEquals(CustomerAuthorizationStatus.REQUESTED,
                result.getAuthorizationStatus());
        assertEquals(quoteId, result.getServiceQuoteId());
        assertEquals(quote.getTermsSnapshot(), result.getTermsSnapshot());
        assertEquals(quote.getDisclaimerSnapshot(), result.getDisclaimerSnapshot());
        assertEquals("\"SERVICE_QUOTE\"",
                result.getAuthorizationScopeSnapshot().get("sourceType").toString());
        assertEquals(List.of(
                        "\"" + firstLineId + "\"",
                        "\"" + secondLineId + "\""
                ),
                result.getAuthorizationScopeSnapshot().get("quoteLineIds")
                        .valueStream()
                        .map(node -> node.toString())
                        .toList());

        JsonNode commercial = result.getCommercialSnapshot();
        assertEquals("\"INR\"", commercial.get("currencyCode").toString());
        assertEquals("99.9999",
                commercial.get("lines").get(1).get("unitPrice").toString());
        assertEquals("117.9999",
                commercial.get("lines").get(1).get("grossAmount").toString());
        assertEquals("\"" + serviceJobId + "\"",
                commercial.get("lines").get(1).get("serviceJobId").toString());
        assertEquals(ServiceQuoteStatus.ISSUED, quote.getStatus());
    }

    @Test
    void rejectsEveryNonIssuedQuoteStatusWithoutMutatingQuote()
            throws Exception {
        for (ServiceQuoteStatus status : List.of(
                ServiceQuoteStatus.DRAFT,
                ServiceQuoteStatus.ACCEPTED,
                ServiceQuoteStatus.DECLINED,
                ServiceQuoteStatus.CANCELLED,
                ServiceQuoteStatus.EXPIRED,
                ServiceQuoteStatus.SUPERSEDED
        )) {
            UUID caseId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();
            ServiceQuote quote = quoteRecord(quoteId, tenantId, caseId);
            if (status != ServiceQuoteStatus.DRAFT) {
                quote.changeStatus(status, principalId, OffsetDateTime.now());
            }

            prepareRequestCase(caseId);
            when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                    .thenReturn(Optional.of(quote));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.requestFromServiceQuote(
                            context,
                            caseId,
                            quoteId,
                            "AUTH-STATUS-" + status,
                            null,
                            null,
                            "Authorize quoted work"
                    )
            );

            assertEquals(409, exception.getStatusCode().value());
            assertEquals(status, quote.getStatus());
            verify(repository, never()).save(any(CustomerAuthorization.class));
        }
    }

    @Test
    void existingRequestedAuthorizationBlocksOnlyOutstandingQuoteRequest()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        ServiceQuote quote = quoteRecord(quoteId, tenantId, caseId);
        quote.issue(principalId, OffsetDateTime.now());
        prepareRequestCase(caseId);
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quote));
        when(repository.existsByTenantIdAndServiceQuoteIdAndAuthorizationStatus(
                tenantId,
                quoteId,
                CustomerAuthorizationStatus.REQUESTED
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requestFromServiceQuote(
                        context,
                        caseId,
                        quoteId,
                        "AUTH-DUPLICATE-QUOTE",
                        null,
                        null,
                        "Authorize quoted work"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(ServiceQuoteStatus.ISSUED, quote.getStatus());
        verify(quoteLineRepository, never())
                .findByServiceQuoteIdOrderBySequenceAsc(quoteId);
        verify(repository, never()).save(any(CustomerAuthorization.class));
    }

    @Test
    void historicalNonRequestedAuthorizationDoesNotBlockQuoteRequest()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        ServiceQuote quote = quoteRecord(quoteId, tenantId, caseId);
        quote.issue(principalId, OffsetDateTime.now());
        prepareRequestCase(caseId);
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quote));
        when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quoteId))
                .thenReturn(List.of());
        when(repository.save(any(CustomerAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAuthorization result = service.requestFromServiceQuote(
                context,
                caseId,
                quoteId,
                "AUTH-HISTORICAL-1",
                null,
                null,
                "Authorize quoted work"
        );

        assertEquals(CustomerAuthorizationStatus.REQUESTED,
                result.getAuthorizationStatus());
    }

    @Test
    void findsCompleteQuoteAuthorizationHistoryNewestFirst()
            throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        ServiceQuote quote = quoteRecord(quoteId, tenantId, caseId);
        CustomerAuthorization newest = authorizationRecord(
                UUID.randomUUID(),
                caseId
        );
        CustomerAuthorization historical = authorizationRecord(
                UUID.randomUUID(),
                caseId
        );
        historical.decline("EMAIL", "declined-1", principalId,
                OffsetDateTime.now());

        prepareRequestCase(caseId);
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quote));
        when(repository
                .findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
                        tenantId,
                        quoteId
                )).thenReturn(List.of(newest, historical));

        List<CustomerAuthorization> result = service.findAllForServiceQuote(
                context,
                caseId,
                quoteId
        );

        assertEquals(List.of(newest, historical), result);
        assertEquals(CustomerAuthorizationStatus.REQUESTED,
                result.getFirst().getAuthorizationStatus());
        assertEquals(CustomerAuthorizationStatus.DECLINED,
                result.get(1).getAuthorizationStatus());
        verify(repository)
                .findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
                        tenantId,
                        quoteId
                );
        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    @Test
    void returnsEmptyQuoteAuthorizationHistory() {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareRequestCase(caseId);
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quoteRecord(quoteId, tenantId, caseId)));
        when(repository
                .findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
                        tenantId,
                        quoteId
                )).thenReturn(List.of());

        assertEquals(List.of(), service.findAllForServiceQuote(
                context,
                caseId,
                quoteId
        ));
    }

    @Test
    void rejectsMissingCaseBeforeQuoteHistoryLookup() {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findAllForServiceQuote(context, caseId, quoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("AfterSales case not found", exception.getReason());
        verifyNoInteractions(quoteRepository);
        verify(repository, never())
                .findAllByTenantIdAndServiceQuoteIdOrderByRequestedAtDescIdDesc(
                        any(UUID.class),
                        any(UUID.class)
                );
    }

    @Test
    void rejectsUnknownOrMiscontainedQuoteWithUniformNotFound() {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findAllForServiceQuote(context, caseId, quoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
    }

    @Test
    void rejectsCrossTenantQuoteWithUniformNotFound() {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findAllForServiceQuote(context, caseId, quoteId)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
        verify(quoteRepository)
                .findByIdAndTenantId(quoteId, tenantId);
    }

    @Test
    void rejectsDifferentCaseAndNullCaseQuoteWithUniformNotFound() {
        UUID caseId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareRequestCase(caseId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(
                        quoteRecord(quoteId, tenantId, UUID.randomUUID())
                ));

        ResponseStatusException differentCase = assertThrows(
                ResponseStatusException.class,
                () -> service.findAllForServiceQuote(context, caseId, quoteId)
        );
        assertEquals(404, differentCase.getStatusCode().value());
        assertEquals("Service quote not found", differentCase.getReason());

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(quoteRecord(quoteId, tenantId, null)));

        ResponseStatusException nullCase = assertThrows(
                ResponseStatusException.class,
                () -> service.findAllForServiceQuote(context, caseId, quoteId)
        );
        assertEquals(404, nullCase.getStatusCode().value());
        assertEquals("Service quote not found", nullCase.getReason());
    }

    @Test
    void parentCaseLookupIsScopedToAuthenticatedTenant() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.authorize(
                                context,
                                caseId,
                                authorizationId,
                                "CUSTOMER_PORTAL",
                                "decision-cross-tenant"
                        )
                );

        assertEquals(404, exception.getStatusCode().value());

        verify(caseRepository).findByIdAndTenantId(
                caseId,
                tenantId
        );

        verify(repository, never())
                .findByIdAndTenantId(
                        any(UUID.class),
                        any(UUID.class)
                );
    }

    @Test
    void decisionPreservesAuthorizationAndCommercialSnapshots()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        JsonNode scopeSnapshot = json("""
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

        JsonNode commercialSnapshot = json("""
                {
                  "currency": "INR",
                  "subtotal": 8500.00,
                  "tax": 1530.00,
                  "total": 10030.00
                }
                """);

        String termsSnapshot =
                "Applicable commercial terms at authorization time";

        String disclaimerSnapshot =
                "Applicable dealer disclaimer at authorization time";

        CustomerAuthorization authorization =
                CustomerAuthorization.request(
                        authorizationId,
                        tenantId,
                        null,
                        null,
                        caseId,
                        "AUTH-SNAPSHOT-1",
                        "CUSTOMER-TEST",
                        "Test Customer",
                        "Authorize brake work",
                        scopeSnapshot,
                        commercialSnapshot,
                        termsSnapshot,
                        disclaimerSnapshot,
                        principalId,
                        OffsetDateTime.now()
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-snapshot"
        );

        assertEquals(
                scopeSnapshot,
                authorization.getAuthorizationScopeSnapshot()
        );
        assertEquals(
                commercialSnapshot,
                authorization.getCommercialSnapshot()
        );
        assertEquals(
                termsSnapshot,
                authorization.getTermsSnapshot()
        );
        assertEquals(
                disclaimerSnapshot,
                authorization.getDisclaimerSnapshot()
        );
    }

    @Test
    void decisionRecordsConsistentAuditMetadata()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        service.decline(
                context,
                caseId,
                authorizationId,
                "EMAIL",
                "decision-audit"
        );

        assertNotNull(authorization.getDecidedAt());
        assertNotNull(authorization.getUpdatedAt());

        assertEquals(
                authorization.getDecidedAt(),
                authorization.getUpdatedAt()
        );

        assertEquals(
                principalId,
                authorization.getUpdatedByPrincipalId()
        );
    }

    @Test
    void findAllForCaseAuthorizesReadAgainstParentCase() {
        UUID caseId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.of(
                caseRecord(caseId, null, null)
        ));

        when(repository.findAllByTenantIdAndAftersalesCaseId(
                tenantId,
                caseId
        )).thenReturn(java.util.List.of());

        service.findAllForCase(
                context,
                caseId
        );

        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );

        verify(repository)
                .findAllByTenantIdAndAftersalesCaseId(
                        tenantId,
                        caseId
                );
    }
    @Test
    void requestRemainsSuccessfulWithLifecycleObservabilityEnabled()
            throws Exception {

        UUID caseId = UUID.randomUUID();

        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.of(
                caseRecord(caseId, null, null)
        ));

        when(repository.save(any(CustomerAuthorization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAuthorization result =
                service.request(
                        context,
                        caseId,
                        "AUTH-OBS-1",
                        "CUSTOMER-OBS",
                        "Observability Customer",
                        "Authorize observability test",
                        json("""
                                {
                                  "scopeVersion": 1,
                                  "items": []
                                }
                                """),
                        null,
                        "Observability terms",
                        "Observability disclaimer"
                );

        assertEquals(
                CustomerAuthorizationStatus.REQUESTED,
                result.getAuthorizationStatus()
        );
        assertEquals(
                principalId,
                result.getCreatedByPrincipalId()
        );
    }

    @Test
    void failedRepeatedDecisionDoesNotOverwriteSuccessfulDecision()
            throws Exception {

        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();

        CustomerAuthorization authorization =
                authorizationRecord(
                        authorizationId,
                        caseId
                );

        prepareDecisionLookup(
                caseId,
                authorizationId,
                authorization
        );

        service.authorize(
                context,
                caseId,
                authorizationId,
                "CUSTOMER_PORTAL",
                "decision-observability-original"
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.cancel(
                                context,
                                caseId,
                                authorizationId,
                                "SERVICE_ADVISOR",
                                "decision-observability-repeat"
                        )
                );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(
                CustomerAuthorizationStatus.AUTHORIZED,
                authorization.getAuthorizationStatus()
        );
        assertEquals(
                "CUSTOMER_PORTAL",
                authorization.getDecisionChannel()
        );
        assertEquals(
                "decision-observability-original",
                authorization.getDecisionReference()
        );
    }
    private void prepareDecisionLookup(
            UUID caseId,
            UUID authorizationId,
            CustomerAuthorization authorization
    ) {
        when(caseRepository.findByIdAndTenantId(
                caseId,
                tenantId
        )).thenReturn(Optional.of(
                caseRecord(caseId, null, null)
        ));

        when(repository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.of(authorization));
    }

    private void prepareRequestCase(UUID caseId) {
        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(caseRecord(caseId, null, null)));
    }

    private CustomerAuthorization requestWithQuote(
            UUID caseId,
            UUID serviceQuoteId
    ) throws Exception {
        return service.requestFromServiceQuote(
                context,
                caseId,
                serviceQuoteId,
                "AUTH-QUOTE-REJECTED",
                null,
                null,
                "Authorize quoted work"
        );
    }

    private void verifyDecisionPermission(UUID caseId) {
        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_DECIDE,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    private AfterSalesCase caseRecord(
            UUID caseId,
            UUID dealerId,
            UUID branchId
    ) {
        return AfterSalesCase.open(
                caseId,
                tenantId,
                dealerId,
                branchId,
                "ASC-S522",
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR,
                principalId,
                OffsetDateTime.now()
        );
    }

    private ServiceQuote quoteRecord(
            UUID quoteId,
            UUID quoteTenantId,
            UUID caseId
    ) {
        return quoteRecord(
                quoteId,
                quoteTenantId,
                caseId,
                UUID.randomUUID(),
                "QUOTE-" + quoteId
        );
    }

    private ServiceQuote quoteRecord(
            UUID quoteId,
            UUID quoteTenantId,
            UUID caseId,
            UUID serviceOrderId,
            String quoteNumber
    ) {
        return ServiceQuote.create(
                quoteId,
                quoteTenantId,
                null,
                null,
                caseId,
                serviceOrderId,
                quoteNumber,
                "INR",
                null,
                null,
                null,
                principalId,
                OffsetDateTime.now()
        );
        }

    private CustomerAuthorization authorizationRecord(
            UUID authorizationId,
            UUID caseId
    ) throws Exception {
        return CustomerAuthorization.request(
                authorizationId,
                tenantId,
                null,
                null,
                caseId,
                "AUTH-S522-" + authorizationId,
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
                principalId,
                OffsetDateTime.now()
        );
    }

    private JsonNode json(String value) throws Exception {
        return OBJECT_MAPPER.readTree(value);
    }
}
