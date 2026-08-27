package com.autovision.platform.serviceprofit.detection;

import com.autovision.platform.serviceprofit.ServiceProfitActionability;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceClass;
import com.autovision.platform.serviceprofit.ServiceProfitEvidenceStrength;
import com.autovision.platform.serviceprofit.ServiceProfitFollowUpActivationService;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunity;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityRepository;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityStatus;
import com.autovision.platform.serviceprofit.ServiceProfitOpportunityType;
import com.autovision.platform.serviceprofit.ServiceProfitPriority;
import com.autovision.platform.serviceprofit.ServiceProfitSuppressionReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitDetectionPersistenceServiceTests {

    private static final UUID TENANT_ID =
            UUID.fromString(
                    "10000000-0000-4000-8000-000000000001"
            );

    private static final UUID PRINCIPAL_ID =
            UUID.fromString(
                    "20000000-0000-4000-8000-000000000001"
            );

    private static final OffsetDateTime DETECTED_AT =
            OffsetDateTime.parse(
                    "2026-08-22T10:00:00+05:30"
            );

    @Test
    void noMatchDoesNotWriteAnything() {

        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(
                        repository
                );

        ServiceProfitPersistenceResult result =
                service.persist(
                        ServiceProfitDetectionResult.noMatch(),
                        PRINCIPAL_ID,
                        DETECTED_AT
                );

        assertEquals(
                ServiceProfitPersistenceOutcome.NO_MATCH,
                result.outcome()
        );

        assertNull(result.opportunity());

        verify(repository, never())
                .findByTenantIdAndOpportunityKey(
                        any(),
                        any()
                );

        verify(repository, never())
                .save(any());
    }

    @Test
    void firstDetectionPersistsExactlyOnce() {

        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        when(
                repository.findByTenantIdAndOpportunityKey(
                        TENANT_ID,
                        "SP:DECLINED_WORK:TEST:001"
                )
        ).thenReturn(Optional.empty());

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(
                        repository
                );

        ServiceProfitPersistenceResult result =
                service.persist(
                        detectionResult(false),
                        PRINCIPAL_ID,
                        DETECTED_AT
                );

        assertEquals(
                ServiceProfitPersistenceOutcome.CREATED,
                result.outcome()
        );

        assertEquals(
                ServiceProfitOpportunityStatus.DETECTED,
                result.opportunity().getStatus()
        );

        verify(repository).save(any());
    }

    @Test
    void firstDetectionActivatesOnlyThePersistedOpportunity() {
        ServiceProfitOpportunityRepository repository = mock(ServiceProfitOpportunityRepository.class);
        ServiceProfitFollowUpActivationService activation = mock(ServiceProfitFollowUpActivationService.class);
        when(repository.findByTenantIdAndOpportunityKey(
                TENANT_ID, "SP:DECLINED_WORK:TEST:001")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(repository, null, activation);

        ServiceProfitPersistenceResult result = service.persist(
                detectionResult(false), PRINCIPAL_ID, DETECTED_AT);

        verify(activation).activate(eq(result.opportunity()), eq(DETECTED_AT));
    }

    @Test
    void sameTenantAndOpportunityKeyReturnsExistingWithoutInsert() {

        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        ServiceProfitOpportunity existing =
                existingOpportunity();

        when(
                repository.findByTenantIdAndOpportunityKey(
                        TENANT_ID,
                        "SP:DECLINED_WORK:TEST:001"
                )
        ).thenReturn(
                Optional.of(existing)
        );

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(
                        repository
                );

        ServiceProfitPersistenceResult result =
                service.persist(
                        detectionResult(false),
                        PRINCIPAL_ID,
                        DETECTED_AT
                );

        assertEquals(
                ServiceProfitPersistenceOutcome.EXISTING,
                result.outcome()
        );

        assertEquals(
                existing,
                result.opportunity()
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void sameOpportunityKeyInDifferentTenantDoesNotCollide() {

        UUID otherTenantId =
                UUID.fromString(
                        "10000000-0000-4000-8000-000000000002"
                );

        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        when(
                repository.findByTenantIdAndOpportunityKey(
                        TENANT_ID,
                        "SP:DECLINED_WORK:TEST:001"
                )
        ).thenReturn(Optional.empty());

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(
                        repository
                );

        service.persist(
                detectionResult(false),
                PRINCIPAL_ID,
                DETECTED_AT
        );

        verify(repository).findByTenantIdAndOpportunityKey(
                TENANT_ID,
                "SP:DECLINED_WORK:TEST:001"
        );

        verify(repository, never())
                .findByTenantIdAndOpportunityKey(
                        otherTenantId,
                        "SP:DECLINED_WORK:TEST:001"
                );
    }

    @Test
    void suppressedDetectionPersistsSuppressionAuditState() {

        ServiceProfitOpportunityRepository repository =
                mock(ServiceProfitOpportunityRepository.class);

        when(
                repository.findByTenantIdAndOpportunityKey(
                        TENANT_ID,
                        "SP:DECLINED_WORK:TEST:001"
                )
        ).thenReturn(Optional.empty());

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceProfitDetectionPersistenceService service =
                new ServiceProfitDetectionPersistenceService(
                        repository
                );

        ServiceProfitPersistenceResult result =
                service.persist(
                        detectionResult(true),
                        PRINCIPAL_ID,
                        DETECTED_AT
                );

        assertEquals(
                ServiceProfitPersistenceOutcome.CREATED_SUPPRESSED,
                result.outcome()
        );

        assertEquals(
                ServiceProfitOpportunityStatus.SUPPRESSED,
                result.opportunity().getStatus()
        );

        assertEquals(
                ServiceProfitActionability.SUPPRESSED,
                result.opportunity().getActionability()
        );

        assertEquals(
                ServiceProfitSuppressionReason.AUTHORITATIVE_COMPLETION_EVIDENCE,
                result.opportunity().getSuppressionReason()
        );

        assertEquals(
                DETECTED_AT,
                result.opportunity().getSuppressedAt()
        );

        verify(repository).save(any());
    }

    private ServiceProfitDetectionResult detectionResult(
            boolean suppressed
    ) {
        return new ServiceProfitDetectionResult(
                true,
                suppressed,

                "SP:DECLINED_WORK:TEST:001",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                suppressed
                        ? ServiceProfitActionability.SUPPRESSED
                        : ServiceProfitActionability.READY,

                "Declined brake work",
                "Customer declined recommended brake replacement.",

                new BigDecimal("12500.00"),
                "INR",

                TENANT_ID,
                null,
                null,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),

                "TEST-DMS",
                "RECOMMENDATION",
                "REC-001",

                null,
                null,
                null,
                null,

                "R1-TEST"
        );
    }

    private ServiceProfitOpportunity existingOpportunity() {
        return ServiceProfitOpportunity.detect(
                UUID.fromString(
                        "40000000-0000-4000-8000-000000000001"
                ),
                TENANT_ID,
                null,
                null,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SP:DECLINED_WORK:TEST:001",
                ServiceProfitOpportunityType.DECLINED_WORK,
                ServiceProfitEvidenceClass.SOURCE_CONFIRMED,
                ServiceProfitEvidenceStrength.STRONG,
                ServiceProfitPriority.HIGH,
                ServiceProfitActionability.READY,
                "Declined brake work",
                null,
                null,
                null,
                "TEST-DMS",
                "RECOMMENDATION",
                "REC-001",
                null,
                null,
                null,
                null,
                "R1-TEST",
                PRINCIPAL_ID,
                DETECTED_AT
        );
    }
}