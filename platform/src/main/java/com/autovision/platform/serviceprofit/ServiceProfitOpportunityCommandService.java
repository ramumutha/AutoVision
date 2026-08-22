package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class ServiceProfitOpportunityCommandService {

    private final ServiceProfitOpportunityRepository repository;
    private final AuthorizationService authorizationService;

    public ServiceProfitOpportunityCommandService(
            ServiceProfitOpportunityRepository repository,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ServiceProfitOpportunity create(
            AuthenticatedTenantContext tenantContext,
            CreateServiceProfitOpportunityRequest request
    ) {
        requireTenantContext(tenantContext);

        if (request == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Service Profit Opportunity request is required"
            );
        }

        validateOrganizationHierarchy(request);

        requireCreatePermission(
                tenantContext,
                request.dealerId(),
                request.branchId(),
                request.locationId()
        );

        if (request.opportunityKey() != null
                && repository.existsByTenantIdAndOpportunityKey(
                        tenantContext.tenantId(),
                        request.opportunityKey().trim()
                )) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Service Profit Opportunity already exists"
            );
        }

        OffsetDateTime detectedAt =
                request.detectedAt() != null
                        ? request.detectedAt()
                        : OffsetDateTime.now();

        try {
            ServiceProfitOpportunity opportunity =
                    ServiceProfitOpportunity.detect(
                            UUID.randomUUID(),
                            tenantContext.tenantId(),
                            request.dealerId(),
                            request.branchId(),
                            request.locationId(),
                            request.customerId(),
                            request.vehicleId(),
                            request.opportunityKey(),
                            request.opportunityType(),
                            request.evidenceClass(),
                            request.evidenceStrength(),
                            request.priority(),
                            request.actionability(),
                            request.title(),
                            request.summary(),
                            request.potentialAmount(),
                            request.currencyCode(),
                            request.sourceSystem(),
                            request.sourceEntityType(),
                            request.sourceEntityId(),
                            request.sourceServiceOrderId(),
                            request.sourceServiceJobId(),
                            request.sourceServiceLineId(),
                            request.sourceQuoteId(),
                            request.policyVersion(),
                            tenantContext.userRefId(),
                            detectedAt
                    );

            return repository.save(opportunity);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private void validateOrganizationHierarchy(
            CreateServiceProfitOpportunityRequest request
    ) {
        if (request.locationId() != null
                && request.branchId() == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "branchId is required when locationId is provided"
            );
        }

        if (request.branchId() != null
                && request.dealerId() == null) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "dealerId is required when branchId is provided"
            );
        }
    }

    private void requireCreatePermission(
            AuthenticatedTenantContext tenantContext,
            UUID dealerId,
            UUID branchId,
            UUID locationId
    ) {
        AuthorizationResourceType resourceType;
        UUID resourceId;

        if (locationId != null) {
            resourceType = AuthorizationResourceType.LOCATION;
            resourceId = locationId;
        } else if (branchId != null) {
            resourceType = AuthorizationResourceType.BRANCH;
            resourceId = branchId;
        } else if (dealerId != null) {
            resourceType = AuthorizationResourceType.DEALER;
            resourceId = dealerId;
        } else {
            resourceType = AuthorizationResourceType.TENANT;
            resourceId = tenantContext.tenantId();
        }

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        ServiceProfitPermissions.OPPORTUNITY_CREATE,
                        resourceType,
                        resourceId
                )
        );
    }

    private void requireTenantContext(
            AuthenticatedTenantContext tenantContext
    ) {
        if (tenantContext == null
                || tenantContext.tenantId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated tenant context is required"
            );
        }
    }
}
