package com.autovision.platform.serviceprofit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitOpportunityContextServiceTests {

    @Test
    void refreshesExistingProjectionInsteadOfCreatingDuplicate() {
        ServiceProfitOpportunityContextRepository repository =
                mock(ServiceProfitOpportunityContextRepository.class);
        ServiceProfitOpportunity opportunity = opportunity();
        ServiceProfitOpportunityContext existing =
                ServiceProfitOpportunityContext.capture(
                        UUID.randomUUID(), opportunity.getTenantId(),
                        opportunity, snapshot("Original"),
                        OffsetDateTime.parse("2026-08-22T10:00:00Z")
                );

        when(repository.findByOpportunityIdAndTenantId(
                opportunity.getId(), opportunity.getTenantId()
        )).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0));

        ServiceProfitOpportunityContext result =
                new ServiceProfitOpportunityContextService(repository)
                        .capture(
                                opportunity,
                                snapshot("Updated"),
                                OffsetDateTime.parse(
                                        "2026-08-23T10:00:00Z"
                                )
                        );

        assertSame(existing, result);
        assertEquals("Updated", result.getCustomerDisplayName());
        verify(repository).save(existing);
    }

    private ServiceProfitOpportunityContextSnapshot snapshot(String name) {
        return new ServiceProfitOpportunityContextSnapshot(
                name, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null
        );
    }

    private ServiceProfitOpportunity opportunity() {
        return ServiceProfitOpportunity.detect(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                null, null, "CONTEXT-TEST",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined work", null, new BigDecimal("100"), "INR",
                "DEMO", "SERVICE_JOB", "JOB-1", null, null, null,
                null, "R1", null, OffsetDateTime.now()
        );
    }
}