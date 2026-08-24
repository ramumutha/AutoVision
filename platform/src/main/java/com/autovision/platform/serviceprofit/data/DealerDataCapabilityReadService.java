package com.autovision.platform.serviceprofit.data;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.serviceprofit.ServiceProfitPermissions;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Read-side projection for Service Profit commercial data capabilities.
 *
 * Tenant authority is derived exclusively from the authenticated context.
 * Only the latest COMPLETED assessment is eligible for presentation.
 */
@Service
@Transactional(readOnly = true)
public class DealerDataCapabilityReadService {

    private static final Set<DealerDataCapability> COMMERCIAL_CAPABILITIES =
            EnumSet.of(
                    DealerDataCapability.REVENUE_ATTRIBUTION,
                    DealerDataCapability.GROSS_PROFIT_ATTRIBUTION
            );

    private final DealerDataAssessmentRepository assessmentRepository;
    private final DealerDataCapabilityAssessmentRepository capabilityRepository;
    private final AuthorizationService authorizationService;

    public DealerDataCapabilityReadService(
            DealerDataAssessmentRepository assessmentRepository,
            DealerDataCapabilityAssessmentRepository capabilityRepository,
            AuthorizationService authorizationService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.capabilityRepository = capabilityRepository;
        this.authorizationService = authorizationService;
    }

    public DealerDataCapabilityResponse findCurrent(
            AuthenticatedTenantContext tenantContext
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        ServiceProfitPermissions.OPPORTUNITY_READ,
                        AuthorizationResourceType.TENANT,
                        tenantContext.tenantId()
                )
        );

        return assessmentRepository
                .findFirstByTenantIdAndStatusOrderByCreatedAtDesc(
                        tenantContext.tenantId(),
                        DealerDataAssessmentStatus.COMPLETED
                )
                .map(this::toResponse)
                .orElseGet(DealerDataCapabilityResponse::notAssessed);
    }

    private DealerDataCapabilityResponse toResponse(
            DealerDataAssessment assessment
    ) {
        List<DealerDataCapabilityItemResponse> capabilities =
                capabilityRepository
                        .findByAssessmentIdOrderByCapabilityAsc(
                                assessment.getId()
                        )
                        .stream()
                        .filter(candidate ->
                                COMMERCIAL_CAPABILITIES.contains(
                                        candidate.getCapability()
                                )
                        )
                        .map(DealerDataCapabilityItemResponse::from)
                        .toList();

        return DealerDataCapabilityResponse.assessed(
                assessment,
                capabilities
        );
    }

    private static void requireTenantContext(
            AuthenticatedTenantContext tenantContext
    ) {
        if (tenantContext == null || tenantContext.tenantId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated tenant context is required"
            );
        }
    }
}
