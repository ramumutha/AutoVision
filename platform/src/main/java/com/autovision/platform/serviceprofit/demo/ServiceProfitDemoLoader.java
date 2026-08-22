package com.autovision.platform.serviceprofit.demo;

import com.autovision.platform.serviceprofit.data.DealerDataAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapability;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessment;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityAssessmentRepository;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityResult;
import com.autovision.platform.serviceprofit.data.DealerDataCapabilityStatus;
import com.autovision.platform.serviceprofit.data.DealerDataCoverage;
import com.autovision.platform.serviceprofit.data.DealerDataFeatureProfile;
import com.autovision.platform.serviceprofit.data.R1DealerDataCapabilityPolicy;
import com.autovision.platform.serviceprofit.data.R1DealerDataReadinessPolicy;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ServiceProfitDemoLoader {

    public static final String EXPECTED_CLASSIFICATION =
            "SYNTHETIC_DEMO_ONLY";

    private final ObjectMapper objectMapper;
    private final ServiceProfitDemoTenantRepository tenantRepository;
    private final DealerDataAssessmentRepository assessmentRepository;
    private final DealerDataCapabilityAssessmentRepository capabilityRepository;
    private final R1DealerDataReadinessPolicy readinessPolicy;
    private final R1DealerDataCapabilityPolicy capabilityPolicy;

    public ServiceProfitDemoLoader(
            ObjectMapper objectMapper,
            ServiceProfitDemoTenantRepository tenantRepository,
            DealerDataAssessmentRepository assessmentRepository,
            DealerDataCapabilityAssessmentRepository capabilityRepository,
            R1DealerDataReadinessPolicy readinessPolicy,
            R1DealerDataCapabilityPolicy capabilityPolicy
    ) {
        this.objectMapper = objectMapper;
        this.tenantRepository = tenantRepository;
        this.assessmentRepository = assessmentRepository;
        this.capabilityRepository = capabilityRepository;
        this.readinessPolicy = readinessPolicy;
        this.capabilityPolicy = capabilityPolicy;
    }

    @Transactional
    public ServiceProfitDemoLoadResult load(
            Path datasetRoot,
            UUID principalId,
            OffsetDateTime now
    ) {
        requirePath(datasetRoot);
        requireId(principalId, "Principal ID is required");
        requireTime(now);

        ServiceProfitDemoManifest manifest =
                readManifest(datasetRoot);

        validateManifest(manifest);

        requireTenant(manifest.tenantId());

        var existing =
                assessmentRepository
                        .findByTenantIdAndSourceDatasetIdAndSourceDatasetVersion(
                                manifest.tenantId(),
                                manifest.datasetId(),
                                manifest.version()
                        );

        if (existing.isPresent()) {
            return new ServiceProfitDemoLoadResult(
                    false,
                    true,
                    existing.get().getId(),
                    manifest.datasetId(),
                    manifest.version()
            );
        }

        DealerDataCoverage coverage =
                toCoverage(manifest.expectedCoverage());

        BigDecimal calculatedScore =
                readinessPolicy.calculateOverallScore(
                        coverage
                );

        if (!readinessPolicy.policyVersion().equals(
                manifest.assessmentPolicyVersion()
        )) {
            throw new IllegalStateException(
                    "Manifest readiness policy version does not match runtime policy"
            );
        }

        DealerDataFeatureProfile featureProfile =
                toFeatureProfile(
                        manifest.featureProfile()
                );

        List<DealerDataCapabilityResult> capabilityResults =
                capabilityPolicy.evaluate(
                        featureProfile
                );

        validateExpectedCapabilities(
                manifest.expectedCapabilities(),
                capabilityResults
        );

        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        manifest.tenantId(),
                        null,
                        null,
                        manifest.datasetId(),
                        "SYNTHETIC_DEMO",
                        manifest.datasetId(),
                        manifest.version(),
                        coverage,
                        calculatedScore,
                        readinessPolicy.policyVersion(),
                        principalId,
                        now
                );

        assessmentRepository.saveAndFlush(
                assessment
        );

        for (DealerDataCapabilityResult result
                : capabilityResults) {

            capabilityRepository.save(
                    DealerDataCapabilityAssessment.create(
                            UUID.randomUUID(),
                            assessment.getId(),
                            result,
                            principalId,
                            now
                    )
            );
        }

        capabilityRepository.flush();

        assessment.complete(
                principalId,
                now
        );

        assessmentRepository.saveAndFlush(
                assessment
        );

        return new ServiceProfitDemoLoadResult(
                true,
                false,
                assessment.getId(),
                manifest.datasetId(),
                manifest.version()
        );
    }

    private ServiceProfitDemoManifest readManifest(
            Path datasetRoot
    ) {
        Path manifestPath =
                datasetRoot.resolve("manifest.json");

        if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException(
                    "Service Profit demo manifest does not exist: "
                            + manifestPath
            );
        }

        try {
            return objectMapper.readValue(
                    manifestPath.toFile(),
                    ServiceProfitDemoManifest.class
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to read Service Profit demo manifest",
                    exception
            );
        }
    }

    private void validateManifest(
            ServiceProfitDemoManifest manifest
    ) {
        if (manifest == null) {
            throw new IllegalArgumentException(
                    "Service Profit demo manifest is required"
            );
        }

        requireText(
                manifest.datasetId(),
                "Demo dataset ID is required"
        );

        requireText(
                manifest.version(),
                "Demo dataset version is required"
        );

        requireText(
                manifest.classification(),
                "Demo dataset classification is required"
        );

        if (!EXPECTED_CLASSIFICATION.equals(
                manifest.classification()
        )) {
            throw new IllegalStateException(
                    "Only SYNTHETIC_DEMO_ONLY datasets may be loaded by this loader"
            );
        }

        requireId(
                manifest.tenantId(),
                "Demo tenant ID is required"
        );

        if (manifest.expectedCoverage() == null) {
            throw new IllegalArgumentException(
                    "Demo expectedCoverage is required"
            );
        }

        if (manifest.featureProfile() == null) {
            throw new IllegalArgumentException(
                    "Demo featureProfile is required"
            );
        }

        if (manifest.expectedCapabilities() == null) {
            throw new IllegalArgumentException(
                    "Demo expectedCapabilities is required"
            );
        }
    }

    private void requireTenant(
            UUID tenantId
    ) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new IllegalStateException(
                    "Demo tenant does not exist: "
                            + tenantId
            );
        }
    }

    private DealerDataCoverage toCoverage(
            ServiceProfitDemoManifestCoverage coverage
    ) {
        return new DealerDataCoverage(
                coverage.identity(),
                coverage.vehicleLinkage(),
                coverage.serviceTransaction(),
                coverage.recommendationEvidence(),
                coverage.disposition(),
                coverage.mileage(),
                coverage.invoiceLinkage(),
                coverage.cost()
        );
    }

    private DealerDataFeatureProfile toFeatureProfile(
            ServiceProfitDemoManifestFeatureProfile profile
    ) {
        return new DealerDataFeatureProfile(
                profile.structuredDisposition(),
                profile.technicianAdvisorNotes(),
                profile.recommendationHistory(),
                profile.serviceHistory(),
                profile.mileageHistory(),
                profile.customerActivityHistory(),
                profile.invoiceLinkage(),
                profile.costData()
        );
    }

    private void validateExpectedCapabilities(
            ServiceProfitDemoManifestCapabilities expected,
            List<DealerDataCapabilityResult> actualResults
    ) {
        Map<DealerDataCapability, DealerDataCapabilityStatus> actual =
                new EnumMap<>(DealerDataCapability.class);

        for (DealerDataCapabilityResult result
                : actualResults) {
            actual.put(
                    result.capability(),
                    result.status()
            );
        }

        verifyCapability(
                actual,
                DealerDataCapability.DECLINED_WORK_EXPLICIT,
                expected.DECLINED_WORK_EXPLICIT()
        );

        verifyCapability(
                actual,
                DealerDataCapability.DECLINED_WORK_RECONSTRUCTION,
                expected.DECLINED_WORK_RECONSTRUCTION()
        );

        verifyCapability(
                actual,
                DealerDataCapability.DEFERRED_WORK,
                expected.DEFERRED_WORK()
        );

        verifyCapability(
                actual,
                DealerDataCapability.DUE_OVERDUE_SERVICE,
                expected.DUE_OVERDUE_SERVICE()
        );

        verifyCapability(
                actual,
                DealerDataCapability.INACTIVE_CUSTOMER,
                expected.INACTIVE_CUSTOMER()
        );

        verifyCapability(
                actual,
                DealerDataCapability.REVENUE_ATTRIBUTION,
                expected.REVENUE_ATTRIBUTION()
        );

        verifyCapability(
                actual,
                DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                expected.GROSS_PROFIT_ATTRIBUTION()
        );
    }

    private void verifyCapability(
            Map<DealerDataCapability, DealerDataCapabilityStatus> actual,
            DealerDataCapability capability,
            DealerDataCapabilityStatus expected
    ) {
        DealerDataCapabilityStatus actualStatus =
                actual.get(capability);

        if (actualStatus != expected) {
            throw new IllegalStateException(
                    "Manifest capability mismatch for "
                            + capability
                            + ": expected "
                            + expected
                            + ", calculated "
                            + actualStatus
            );
        }
    }

    private static void requirePath(
            Path datasetRoot
    ) {
        if (datasetRoot == null) {
            throw new IllegalArgumentException(
                    "Demo dataset root is required"
            );
        }
    }

    private static void requireId(
            UUID value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireTime(
            OffsetDateTime value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Demo load time is required"
            );
        }
    }

    private static void requireText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}


