package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AfterSalesCaseServiceTests {

    private final AfterSalesCaseRepository repository =
            mock(AfterSalesCaseRepository.class);

    private final AfterSalesCaseService service =
            new AfterSalesCaseService(repository);

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
    void findsCaseOnlyWithinAuthenticatedTenant() {
        UUID caseId = UUID.randomUUID();

        AfterSalesCase afterSalesCase = AfterSalesCase.open(
                caseId,
                tenantId,
                null,
                null,
                "ASC-2001",
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR,
                principalId,
                java.time.OffsetDateTime.now()
        );

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(afterSalesCase));

        AfterSalesCase result =
                service.findById(context, caseId);

        assertSame(afterSalesCase, result);

        verify(repository)
                .findByIdAndTenantId(caseId, tenantId);
    }

    @Test
    void returnsNotFoundWhenCaseIsOutsideTenantOrAbsent() {
        UUID caseId = UUID.randomUUID();

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.findById(context, caseId)
                );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void rejectsDuplicateCaseNumberWithinTenant() {
        when(repository.existsByTenantIdAndCaseNumber(
                tenantId,
                "ASC-2002"
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.open(
                                context,
                                "ASC-2002",
                                null,
                                null,
                                AfterSalesCaseSourceChannel.SERVICE_ADVISOR
                        )
                );

        assertEquals(409, exception.getStatusCode().value());

        verify(repository, never())
                .save(any(AfterSalesCase.class));
    }

    @Test
    void opensCaseWithTenantAndAuditContext() {
        when(repository.existsByTenantIdAndCaseNumber(
                tenantId,
                "ASC-2003"
        )).thenReturn(false);

        when(repository.save(any(AfterSalesCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AfterSalesCase result =
                service.open(
                        context,
                        "ASC-2003",
                        null,
                        null,
                        AfterSalesCaseSourceChannel.CUSTOMER_PORTAL
                );

        assertNotNull(result.getId());
        assertEquals(tenantId, result.getTenantId());
        assertEquals("ASC-2003", result.getCaseNumber());
        assertEquals(
                AfterSalesCaseStatus.OPEN,
                result.getLifecycleStatus()
        );
        assertEquals(
                AfterSalesCaseSourceChannel.CUSTOMER_PORTAL,
                result.getSourceChannel()
        );
        assertEquals(principalId, result.getCreatedByPrincipalId());
        assertEquals(principalId, result.getUpdatedByPrincipalId());
        assertNotNull(result.getOpenedAt());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertNull(result.getClosedAt());
    }

    @Test
    void closesExistingCaseWithAuditContext() {
        UUID caseId = UUID.randomUUID();

        AfterSalesCase afterSalesCase = AfterSalesCase.open(
                caseId,
                tenantId,
                null,
                null,
                "ASC-2004",
                AfterSalesCaseSourceChannel.SERVICE_ADVISOR,
                principalId,
                java.time.OffsetDateTime.now()
        );

        when(repository.findByIdAndTenantId(caseId, tenantId))
                .thenReturn(Optional.of(afterSalesCase));

        AfterSalesCase result =
                service.close(context, caseId);

        assertEquals(
                AfterSalesCaseStatus.CLOSED,
                result.getLifecycleStatus()
        );
        assertNotNull(result.getClosedAt());
        assertEquals(principalId, result.getUpdatedByPrincipalId());

        verify(repository)
                .findByIdAndTenantId(caseId, tenantId);
    }
}