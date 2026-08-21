package com.autovision.platform.aftersales;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerAuthorizationCoverageServiceTests {

    private final CustomerAuthorizationRepository authorizationRepository =
            mock(CustomerAuthorizationRepository.class);
    private final AfterSalesCaseRepository caseRepository =
            mock(AfterSalesCaseRepository.class);
    private final ServiceQuoteRepository quoteRepository =
            mock(ServiceQuoteRepository.class);
    private final ServiceQuoteLineRepository quoteLineRepository =
            mock(ServiceQuoteLineRepository.class);
    private final AuthorizationService authorizationService =
            mock(AuthorizationService.class);

    private final CustomerAuthorizationCoverageService service =
            new CustomerAuthorizationCoverageService(
                    authorizationRepository,
                    caseRepository,
                    quoteRepository,
                    quoteLineRepository,
                    authorizationService
            );

    private final UUID tenantId = UUID.fromString(
            "2cf85fea-bc61-4405-be50-00a0ca45df3b"
    );
    private final UUID principalId = UUID.fromString(
            "761b3ab6-bd03-48c0-a107-44fa1403b0f3"
    );
    private final AuthenticatedTenantContext context =
            new AuthenticatedTenantContext(
                    principalId,
                    tenantId,
                    "svc-advisor-01"
            );

    @Test
    void resolvesLinkedAuthorizationCoverageInPersistedOrder() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID lineOneId = UUID.randomUUID();
        UUID lineTwoId = UUID.randomUUID();
        UUID lineThreeId = UUID.randomUUID();
        UUID serviceLineOneId = UUID.randomUUID();
        UUID serviceLineTwoId = UUID.randomUUID();
        UUID serviceLineThreeId = UUID.randomUUID();
        UUID jobOneId = UUID.randomUUID();
        UUID jobTwoId = UUID.randomUUID();

        prepareCase(caseId);
        prepareAuthorization(authorizationId, caseId, quoteId);
        prepareQuote(quoteId, caseId, serviceOrderId);
        when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quoteId))
                .thenReturn(List.of(
                        quoteLine(lineOneId, quoteId, serviceLineOneId, jobOneId, 2),
                        quoteLine(lineTwoId, quoteId, serviceLineTwoId, jobOneId, 5),
                        quoteLine(lineThreeId, quoteId, serviceLineThreeId, jobTwoId, 8)
                ));

        CustomerAuthorizationCoverage result = service.resolve(
                context,
                caseId,
                authorizationId
        );

        assertEquals(authorizationId, result.authorizationId());
        assertEquals(quoteId, result.serviceQuoteId());
        assertEquals(serviceOrderId, result.serviceOrderId());
        assertEquals(
                List.of(serviceLineOneId, serviceLineTwoId, serviceLineThreeId),
                result.serviceLineIds()
        );
        assertEquals(List.of(jobOneId, jobTwoId), result.serviceJobIds());
        verifyPermission(caseId);
        verify(quoteLineRepository)
                .findByServiceQuoteIdOrderBySequenceAsc(quoteId);
    }

    @Test
    void preservesLineCoverageWhenQuoteLinesHaveNoJobs() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceLineId = UUID.randomUUID();

        prepareCase(caseId);
        prepareAuthorization(authorizationId, caseId, quoteId);
        prepareQuote(quoteId, caseId, serviceOrderId);
        when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quoteId))
                .thenReturn(List.of(
                        quoteLine(
                                UUID.randomUUID(),
                                quoteId,
                                serviceLineId,
                                null,
                                1
                        )
                ));

        CustomerAuthorizationCoverage result = service.resolve(
                context,
                caseId,
                authorizationId
        );

        assertEquals(List.of(serviceLineId), result.serviceLineIds());
        assertEquals(List.of(), result.serviceJobIds());
    }

    @Test
    void returnsEmptyCoverageForQuoteWithoutLines() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();

        prepareCase(caseId);
        prepareAuthorization(authorizationId, caseId, quoteId);
        prepareQuote(quoteId, caseId, UUID.randomUUID());
        when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(quoteId))
                .thenReturn(List.of());

        CustomerAuthorizationCoverage result = service.resolve(
                context,
                caseId,
                authorizationId
        );

        assertEquals(List.of(), result.serviceLineIds());
        assertEquals(List.of(), result.serviceJobIds());
    }

    @Test
    void resolvesCoverageForEveryAuthorizationStatus() {
        for (CustomerAuthorizationStatus status :
                CustomerAuthorizationStatus.values()) {
            UUID caseId = UUID.randomUUID();
            UUID authorizationId = UUID.randomUUID();
            UUID quoteId = UUID.randomUUID();

            prepareCase(caseId);
            CustomerAuthorization authorization =
                    authorization(authorizationId, caseId, quoteId);
            applyStatus(authorization, status);
            when(authorizationRepository.findByIdAndTenantId(
                    authorizationId,
                    tenantId
            )).thenReturn(Optional.of(authorization));
            prepareQuote(quoteId, caseId, UUID.randomUUID());
            when(quoteLineRepository.findByServiceQuoteIdOrderBySequenceAsc(
                    quoteId
            )).thenReturn(List.of());

            assertEquals(
                    authorizationId,
                    service.resolve(context, caseId, authorizationId)
                            .authorizationId()
            );
        }
    }

    @Test
    void requiresReadPermissionAgainstParentCase() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        AuthorizationRequest permission = new AuthorizationRequest(
                context,
                AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                AuthorizationResourceType.AFTERSALES_CASE,
                caseId
        );
        doThrow(new AccessDeniedException("Access is denied"))
                .when(authorizationService)
                .requirePermission(permission);

        assertThrows(
                AccessDeniedException.class,
                () -> service.resolve(context, caseId, authorizationId)
        );

        verifyNoInteractions(caseRepository, authorizationRepository);
    }

    @Test
    void rejectsNullRequiredInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve(null, UUID.randomUUID(), UUID.randomUUID())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve(context, null, UUID.randomUUID())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve(context, UUID.randomUUID(), null)
        );
    }

    @Test
    void rejectsUnknownCaseBeforeAuthorizationLookup() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resolve(context, caseId, authorizationId)
        );

        assertNotFound(exception, "AfterSales case not found");
        verifyNoInteractions(authorizationRepository, quoteRepository);
    }

    @Test
    void rejectsUnknownAuthorizationAndCrossTenantAuthorizationUniformly() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        prepareCase(caseId);
        when(authorizationRepository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resolve(context, caseId, authorizationId)
        );

        assertNotFound(exception, "Customer authorization not found");
        verifyNoInteractions(quoteRepository);
    }

    @Test
    void rejectsAuthorizationFromAnotherCase() {
        UUID caseId = UUID.randomUUID();
        UUID otherCaseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        prepareCase(caseId);
        when(authorizationRepository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.of(
                authorization(authorizationId, otherCaseId, UUID.randomUUID())
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resolve(context, caseId, authorizationId)
        );

        assertNotFound(exception, "Customer authorization not found");
        verifyNoInteractions(quoteRepository);
    }

    @Test
    void rejectsAuthorizationWithoutQuoteLinkage() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        prepareCase(caseId);
        when(authorizationRepository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.of(
                authorization(authorizationId, caseId, null)
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.resolve(context, caseId, authorizationId)
        );

        assertNotFound(exception, "Customer authorization quote not found");
        verifyNoInteractions(quoteRepository, quoteLineRepository);
    }

    @Test
    void rejectsUnknownCrossTenantDifferentCaseAndNullCaseQuotesUniformly() {
        UUID caseId = UUID.randomUUID();
        UUID authorizationId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        prepareCase(caseId);
        prepareAuthorization(authorizationId, caseId, quoteId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.empty());
        assertQuoteNotFound(service, caseId, authorizationId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(
                        quote(quoteId, tenantId, UUID.randomUUID(), UUID.randomUUID())
                ));
        assertQuoteNotFound(service, caseId, authorizationId);

        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(
                        quote(quoteId, tenantId, null, UUID.randomUUID())
                ));
        assertQuoteNotFound(service, caseId, authorizationId);
    }

    @Test
    void coverageListsAreDefensiveCopies() {
        UUID lineId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        CustomerAuthorizationCoverage coverage =
                new CustomerAuthorizationCoverage(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(lineId),
                        List.of(jobId)
                );

        assertNotSame(List.of(lineId), coverage.serviceLineIds());
        assertNotSame(List.of(jobId), coverage.serviceJobIds());
        assertThrows(
                UnsupportedOperationException.class,
                () -> coverage.serviceLineIds().add(UUID.randomUUID())
        );
    }

    private void prepareCase(UUID caseId) {
        when(caseRepository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(
                        AfterSalesCase.open(
                                caseId,
                                tenantId,
                                null,
                                null,
                                "CASE-1",
                                AfterSalesCaseSourceChannel.SERVICE_ADVISOR,
                                principalId,
                                OffsetDateTime.now()
                        )
                ));
    }

    private void prepareAuthorization(
            UUID authorizationId,
            UUID caseId,
            UUID quoteId
    ) {
        when(authorizationRepository.findByIdAndTenantId(
                authorizationId,
                tenantId
        )).thenReturn(Optional.of(
                authorization(authorizationId, caseId, quoteId)
        ));
    }

    private void prepareQuote(
            UUID quoteId,
            UUID caseId,
            UUID serviceOrderId
    ) {
        when(quoteRepository.findByIdAndTenantId(quoteId, tenantId))
                .thenReturn(Optional.of(
                        quote(quoteId, tenantId, caseId, serviceOrderId)
                ));
    }

    private CustomerAuthorization authorization(
            UUID authorizationId,
            UUID caseId,
            UUID quoteId
    ) {
        return CustomerAuthorization.request(
                authorizationId,
                tenantId,
                null,
                null,
                caseId,
                quoteId,
                "AUTH-" + authorizationId,
                null,
                null,
                "Authorization scope",
                null,
                null,
                null,
                null,
                principalId,
                OffsetDateTime.now()
        );
    }

    private ServiceQuote quote(
            UUID quoteId,
            UUID quoteTenantId,
            UUID caseId,
            UUID serviceOrderId
    ) {
        return ServiceQuote.create(
                quoteId,
                quoteTenantId,
                null,
                null,
                caseId,
                serviceOrderId,
                "QUOTE-" + quoteId,
                "EUR",
                null,
                null,
                null,
                principalId,
                OffsetDateTime.now()
        );
    }

    private ServiceQuoteLine quoteLine(
            UUID lineId,
            UUID quoteId,
            UUID serviceLineId,
            UUID serviceJobId,
            int sequence
    ) {
        return ServiceQuoteLine.create(
                lineId,
                quoteId,
                serviceLineId,
                serviceJobId,
                "Line " + sequence,
                BigDecimal.ONE,
                BigDecimal.TEN,
                "EUR",
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.valueOf(11),
                sequence,
                principalId,
                OffsetDateTime.now()
        );
    }

    private void applyStatus(
            CustomerAuthorization authorization,
            CustomerAuthorizationStatus status
    ) {
        switch (status) {
            case REQUESTED -> { }
            case AUTHORIZED -> authorization.authorize(
                    "EMAIL", "DECISION", principalId, OffsetDateTime.now()
            );
            case DECLINED -> authorization.decline(
                    "EMAIL", "DECISION", principalId, OffsetDateTime.now()
            );
            case DEFERRED -> authorization.defer(
                    "EMAIL", "DECISION", principalId, OffsetDateTime.now()
            );
            case CANCELLED -> authorization.cancel(
                    "EMAIL", "DECISION", principalId, OffsetDateTime.now()
            );
        }
    }

    private void verifyPermission(UUID caseId) {
        verify(authorizationService).requirePermission(
                new AuthorizationRequest(
                        context,
                        AfterSalesPermissions.CUSTOMER_AUTHORIZATION_READ,
                        AuthorizationResourceType.AFTERSALES_CASE,
                        caseId
                )
        );
    }

    private void assertQuoteNotFound(
            CustomerAuthorizationCoverageService coverageService,
            UUID caseId,
            UUID authorizationId
    ) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> coverageService.resolve(context, caseId, authorizationId)
        );
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Service quote not found", exception.getReason());
    }

    private void assertNotFound(
            ResponseStatusException exception,
            String reason
    ) {
        assertEquals(404, exception.getStatusCode().value());
        assertEquals(reason, exception.getReason());
    }
}
