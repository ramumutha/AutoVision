package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.data.DealerDataAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentStatus;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessmentRepository;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.data.R1DealerDataReadinessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitDemoLoaderTests {

    private DealerDataAssessmentRepository assessmentRepository;
    private DealerDataCapabilityAssessmentRepository capabilityRepository;
    private ServiceProfitDemoTenantRepository tenantRepository;

    private ServiceProfitDemoLoader loader;

    @BeforeEach
    void setUp() {

        assessmentRepository =
                mock(DealerDataAssessmentRepository.class);

        capabilityRepository =
                mock(DealerDataCapabilityAssessmentRepository.class);

        tenantRepository =
                mock(ServiceProfitDemoTenantRepository.class);

        when(assessmentRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(capabilityRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        loader = new ServiceProfitDemoLoader(
                new ObjectMapper(),
                tenantRepository,
                assessmentRepository,
                capabilityRepository,
                new R1DealerDataReadinessPolicy(),
                new R1DealerDataCapabilityPolicy()
        );
    }

    @Test
    void loadsValidatedSyntheticDataset() {

        UUID tenantId = UUID.fromString(
                "10000000-0000-4000-8000-000000000001"
        );

        when(tenantRepository.existsById(tenantId))
                .thenReturn(true);

        when(
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                tenantId,
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0"
                        )
        ).thenReturn(Optional.empty());

        List<DealerDataCapabilityAssessment> capabilities =
                new ArrayList<>();

        when(capabilityRepository.save(any()))
                .thenAnswer(invocation -> {
                    DealerDataCapabilityAssessment assessment =
                            invocation.getArgument(0);

                    capabilities.add(assessment);

                    return assessment;
                });

        ServiceProfitDemoLoadResult result =
                loader.load(
                        datasetRoot(),
                        UUID.randomUUID(),
                        OffsetDateTime.parse(
                                "2026-08-22T08:00:00Z"
                        )
                );

        assertTrue(result.loaded());
        assertFalse(result.alreadyPresent());

        assertEquals(
                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                result.datasetId()
        );

        assertEquals(
                "1.0.0",
                result.datasetVersion()
        );

        assertEquals(7, capabilities.size());

        verify(capabilityRepository).flush();

        verify(
                assessmentRepository,
                times(2)
        ).saveAndFlush(any());
    }

    @Test
    void existingDatasetVersionIsIdempotent() {

        UUID tenantId = UUID.fromString(
                "10000000-0000-4000-8000-000000000001"
        );

        UUID assessmentId = UUID.randomUUID();

        when(tenantRepository.existsById(tenantId))
                .thenReturn(true);

        DealerDataAssessment existing =
                mock(DealerDataAssessment.class);

        when(existing.getId())
                .thenReturn(assessmentId);

        when(
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                tenantId,
                                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                                "1.0.0"
                        )
        ).thenReturn(Optional.of(existing));

        ServiceProfitDemoLoadResult result =
                loader.load(
                        datasetRoot(),
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                );

        assertFalse(result.loaded());
        assertTrue(result.alreadyPresent());
        assertEquals(
                assessmentId,
                result.assessmentId()
        );
    }

    @Test
    void rejectsUnknownTenant() {

        UUID tenantId = UUID.fromString(
                "10000000-0000-4000-8000-000000000001"
        );

        when(tenantRepository.existsById(tenantId))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> loader.load(
                        datasetRoot(),
                        UUID.randomUUID(),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void persistedAssessmentIsCompleted() {

        UUID tenantId = UUID.fromString(
                "10000000-0000-4000-8000-000000000001"
        );

        when(tenantRepository.existsById(tenantId))
                .thenReturn(true);

        when(
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(Optional.empty());

        List<DealerDataAssessment> saves =
                new ArrayList<>();

        when(assessmentRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    DealerDataAssessment assessment =
                            invocation.getArgument(0);

                    saves.add(assessment);

                    return assessment;
                });

        loader.load(
                datasetRoot(),
                UUID.randomUUID(),
                OffsetDateTime.parse(
                        "2026-08-22T08:00:00Z"
                )
        );

        DealerDataAssessment completed =
                saves.getLast();

        assertEquals(
                DealerDataAssessmentStatus.COMPLETED,
                completed.getStatus()
        );

        assertEquals(
                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                completed.getSourceDatasetId()
        );

        assertEquals(
                "1.0.0",
                completed.getSourceDatasetVersion()
        );

        assertEquals(
                new java.math.BigDecimal("82.20"),
                completed.getOverallScore()
        );
    }

    private Path datasetRoot() {
        return Path.of(
                "..",
                "demo-data",
                "service-profit",
                "r1"
        )
        .toAbsolutePath()
        .normalize();
    }
}

