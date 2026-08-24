package com.autovision.platform.serviceprofit.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DealerDataAssessmentRepositoryTests {

    @Autowired
    private DealerDataAssessmentRepository assessmentRepository;

    @Autowired
    private DealerDataCapabilityAssessmentRepository capabilityRepository;

    @AfterEach
    void cleanup() {
        capabilityRepository.deleteAll();
        assessmentRepository.deleteAll();
    }

    @Test
    void persistsAssessmentAndCapabilityResults() {

        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime now =
                OffsetDateTime.parse(
                        "2026-08-22T08:00:00Z"
                );

        DealerDataAssessment assessment =
                assessmentRepository.saveAndFlush(
                        newAssessment(
                                tenantId,
                                principalId,
                                now,
                                null,
                                null
                        )
                );

        DealerDataCapabilityAssessment capability =
                DealerDataCapabilityAssessment.create(
                        UUID.randomUUID(),
                        assessment.getId(),
                        new DealerDataCapabilityResult(
                                DealerDataCapability.REVENUE_ATTRIBUTION,
                                DealerDataCapabilityStatus.AVAILABLE,
                                "Invoice linkage coverage is sufficient."
                        ),
                        principalId,
                        now
                );

        capabilityRepository.saveAndFlush(capability);

        DealerDataAssessment reloaded =
                assessmentRepository.findByIdAndTenantId(
                        assessment.getId(),
                        tenantId
                ).orElseThrow();

        assertEquals(
                DealerDataAssessmentStatus.DRAFT,
                reloaded.getStatus()
        );

        List<DealerDataCapabilityAssessment> results =
                capabilityRepository
                        .findByAssessmentIdOrderByCapabilityAsc(
                                assessment.getId()
                        );

        assertEquals(1, results.size());

        assertEquals(
                DealerDataCapability.REVENUE_ATTRIBUTION,
                results.getFirst().getCapability()
        );
    }

    @Test
    void persistsAndFindsDatasetIdentity() {

        UUID tenantId = UUID.randomUUID();

        DealerDataAssessment assessment =
                assessmentRepository.saveAndFlush(
                        newAssessment(
                                tenantId,
                                UUID.randomUUID(),
                                OffsetDateTime.now(),
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0"
                        )
                );

        DealerDataAssessment found =
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                tenantId,
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0"
                        )
                        .orElseThrow();

        assertEquals(
                assessment.getId(),
                found.getId()
        );
    }

    @Test
    void sameDatasetVersionCannotBeStoredTwiceForTenant() {

        UUID tenantId = UUID.randomUUID();

        assessmentRepository.saveAndFlush(
                newAssessment(
                        tenantId,
                        UUID.randomUUID(),
                        OffsetDateTime.now(),
                        "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                        "1.0.0"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> assessmentRepository.saveAndFlush(
                        newAssessment(
                                tenantId,
                                UUID.randomUUID(),
                                OffsetDateTime.now(),
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0"
                        )
                )
        );
    }

    @Test
    void sameCapabilityCannotBeStoredTwiceForAssessment() {

        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        DealerDataAssessment assessment =
                assessmentRepository.saveAndFlush(
                        newAssessment(
                                tenantId,
                                principalId,
                                now,
                                null,
                                null
                        )
                );

        DealerDataCapabilityResult result =
                new DealerDataCapabilityResult(
                        DealerDataCapability.DEFERRED_WORK,
                        DealerDataCapabilityStatus.AVAILABLE,
                        "Deferred-work evidence is available."
                );

        capabilityRepository.saveAndFlush(
                DealerDataCapabilityAssessment.create(
                        UUID.randomUUID(),
                        assessment.getId(),
                        result,
                        principalId,
                        now
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> capabilityRepository.saveAndFlush(
                        DealerDataCapabilityAssessment.create(
                                UUID.randomUUID(),
                                assessment.getId(),
                                result,
                                principalId,
                                now
                        )
                )
        );
    }

    @Test
    void tenantScopedLookupDoesNotCrossTenantBoundary() {

        UUID tenantId = UUID.randomUUID();

        DealerDataAssessment assessment =
                assessmentRepository.saveAndFlush(
                        newAssessment(
                                tenantId,
                                UUID.randomUUID(),
                                OffsetDateTime.now(),
                                null,
                                null
                        )
                );

        assertTrue(
                assessmentRepository.findByIdAndTenantId(
                        assessment.getId(),
                        tenantId
                ).isPresent()
        );

        assertTrue(
                assessmentRepository.findByIdAndTenantId(
                        assessment.getId(),
                        UUID.randomUUID()
                ).isEmpty()
        );
    }


    @Test
    void findsLatestCompletedAssessmentAndIgnoresNewerDraft() {

        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime olderTime =
                OffsetDateTime.parse(
                        "2026-08-22T08:00:00Z"
                );

        OffsetDateTime newerTime =
                OffsetDateTime.parse(
                        "2026-08-22T09:00:00Z"
                );

        DealerDataAssessment completed =
                newAssessment(
                        tenantId,
                        principalId,
                        olderTime,
                        "COMPLETED-DATASET",
                        "1.0.0"
                );

        completed.complete(
                principalId,
                olderTime.plusMinutes(5)
        );

        assessmentRepository.saveAndFlush(completed);

        assessmentRepository.saveAndFlush(
                newAssessment(
                        tenantId,
                        principalId,
                        newerTime,
                        "DRAFT-DATASET",
                        "2.0.0"
                )
        );

        DealerDataAssessment found =
                assessmentRepository
                        .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
                                tenantId,
                                DealerDataAssessmentStatus.COMPLETED
                        )
                        .orElseThrow();

        assertEquals(
                completed.getId(),
                found.getId()
        );

        assertEquals(
                DealerDataAssessmentStatus.COMPLETED,
                found.getStatus()
        );
    }
    private DealerDataAssessment newAssessment(
            UUID tenantId,
            UUID principalId,
            OffsetDateTime now,
            String sourceDatasetId,
            String sourceDatasetVersion
    ) {
        DealerDataCoverage coverage =
                new DealerDataCoverage(
                        new BigDecimal("95.00"),
                        new BigDecimal("94.00"),
                        new BigDecimal("98.00"),
                        new BigDecimal("82.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("71.00"),
                        new BigDecimal("93.00"),
                        new BigDecimal("35.00")
                );

        R1DealerDataReadinessPolicy policy =
                new R1DealerDataReadinessPolicy();

        return DealerDataAssessment.createDraft(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                "Dealer extract",
                "LEGACY_DMS",
                sourceDatasetId,
                sourceDatasetVersion,
                coverage,
                policy.calculateOverallScore(coverage),
                policy.policyVersion(),
                principalId,
                now
        );
    }
}
