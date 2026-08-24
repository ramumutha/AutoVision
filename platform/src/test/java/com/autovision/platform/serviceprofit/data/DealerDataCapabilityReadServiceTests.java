package com.autovision.platform.serviceprofit.data;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DealerDataCapabilityReadServiceTests {

    private DealerDataAssessmentRepository assessmentRepository;
    private DealerDataCapabilityAssessmentRepository capabilityRepository;
    private AuthorizationService authorizationService;
    private DealerDataCapabilityReadService service;

    @BeforeEach
    void setUp() {
        assessmentRepository =
                mock(DealerDataAssessmentRepository.class);

        capabilityRepository =
                mock(DealerDataCapabilityAssessmentRepository.class);

        authorizationService =
                mock(AuthorizationService.class);

        service = new DealerDataCapabilityReadService(
                assessmentRepository,
                capabilityRepository,
                authorizationService
        );
    }

    @Test
    void returnsNotAssessedWhenNoCompletedAssessmentExists() {

        AuthenticatedTenantContext context =
                context();

        when(
                assessmentRepository
                        .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
                                context.tenantId(),
                                DealerDataAssessmentStatus.COMPLETED
                        )
        ).thenReturn(Optional.empty());

        DealerDataCapabilityResponse response =
                service.findCurrent(context);

        assertEquals(
                DealerDataCapabilityResponse.AssessmentState.NOT_ASSESSED,
                response.assessmentState()
        );

        assertEquals(
                List.of(),
                response.capabilities()
        );
    }

    @Test
    void returnsOnlyCommercialCapabilitiesFromLatestCompletedAssessment() {

        AuthenticatedTenantContext context =
                context();

        UUID principalId = UUID.randomUUID();

        OffsetDateTime createdAt =
                OffsetDateTime.parse(
                        "2026-08-22T08:00:00Z"
                );

        DealerDataAssessment assessment =
                completedAssessment(
                        context.tenantId(),
                        principalId,
                        createdAt
                );

        DealerDataCapabilityAssessment revenue =
                DealerDataCapabilityAssessment.create(
                        UUID.randomUUID(),
                        assessment.getId(),
                        new DealerDataCapabilityResult(
                                DealerDataCapability.REVENUE_ATTRIBUTION,
                                DealerDataCapabilityStatus.AVAILABLE,
                                "Invoice linkage is consistently available."
                        ),
                        principalId,
                        createdAt
                );

        DealerDataCapabilityAssessment grossProfit =
                DealerDataCapabilityAssessment.create(
                        UUID.randomUUID(),
                        assessment.getId(),
                        new DealerDataCapabilityResult(
                                DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                                DealerDataCapabilityStatus.PARTIAL,
                                "Gross-profit attribution is constrained by incomplete cost data."
                        ),
                        principalId,
                        createdAt
                );

        DealerDataCapabilityAssessment declinedWork =
                DealerDataCapabilityAssessment.create(
                        UUID.randomUUID(),
                        assessment.getId(),
                        new DealerDataCapabilityResult(
                                DealerDataCapability.DECLINED_WORK_EXPLICIT,
                                DealerDataCapabilityStatus.AVAILABLE,
                                "Explicit declined-work evidence is available."
                        ),
                        principalId,
                        createdAt
                );

        when(
                assessmentRepository
                        .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
                                context.tenantId(),
                                DealerDataAssessmentStatus.COMPLETED
                        )
        ).thenReturn(Optional.of(assessment));

        when(
                capabilityRepository
                        .findByAssessmentIdOrderByCapabilityAsc(
                                assessment.getId()
                        )
        ).thenReturn(
                List.of(
                        declinedWork,
                        grossProfit,
                        revenue
                )
        );

        DealerDataCapabilityResponse response =
                service.findCurrent(context);

        assertEquals(
                DealerDataCapabilityResponse.AssessmentState.ASSESSED,
                response.assessmentState()
        );

        assertEquals(
                "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                response.sourceDatasetId()
        );

        assertEquals(
                "1.0.0",
                response.sourceDatasetVersion()
        );

        assertEquals(
                2,
                response.capabilities().size()
        );

        assertEquals(
                DealerDataCapability.GROSS_PROFIT_ATTRIBUTION,
                response.capabilities().get(0).capability()
        );

        assertEquals(
                DealerDataCapability.REVENUE_ATTRIBUTION,
                response.capabilities().get(1).capability()
        );
    }

    @Test
    void authorizesReadAgainstAuthenticatedTenantResource() {

        AuthenticatedTenantContext context =
                context();

        when(
                assessmentRepository
                        .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
                                context.tenantId(),
                                DealerDataAssessmentStatus.COMPLETED
                        )
        ).thenReturn(Optional.empty());

        service.findCurrent(context);

        ArgumentCaptor<AuthorizationRequest> captor =
                ArgumentCaptor.forClass(
                        AuthorizationRequest.class
                );

        verify(authorizationService)
                .requirePermission(captor.capture());

        AuthorizationRequest request =
                captor.getValue();

        assertEquals(
                context,
                request.context()
        );

        assertEquals(
                ServiceProfitPermissions.OPPORTUNITY_READ,
                request.permissionCode()
        );

        assertEquals(
                AuthorizationResourceType.TENANT,
                request.resourceType()
        );

        assertEquals(
                context.tenantId(),
                request.resourceId()
        );
    }

    @Test
    void rejectsMissingAuthenticatedTenantContext() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findCurrent(null)
        );
    }

    private AuthenticatedTenantContext context() {
        return new AuthenticatedTenantContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "service-profit-test-user"
        );
    }

    private DealerDataAssessment completedAssessment(
            UUID tenantId,
            UUID principalId,
            OffsetDateTime createdAt
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

        DealerDataAssessment assessment =
                DealerDataAssessment.createDraft(
                        UUID.randomUUID(),
                        tenantId,
                        null,
                        null,
                        "Dealer extract",
                        "LEGACY_DMS",
                        "AUTOVISION-SERVICE-PROFIT-R1-DEMO",
                        "1.0.0",
                        coverage,
                        policy.calculateOverallScore(coverage),
                        policy.policyVersion(),
                        principalId,
                        createdAt
                );

        assessment.complete(
                principalId,
                createdAt.plusMinutes(5)
        );

        return assessment;
    }
}