package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceProfitOpportunityAccessService {

    private final ServiceProfitOpportunityRepository repository;
    private final AuthorizationService authorizationService;

    public ServiceProfitOpportunityAccessService(
            ServiceProfitOpportunityRepository repository,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public ServiceProfitOpportunity requireOpportunity(
            AuthenticatedTenantContext tenantContext,
            UUID opportunityId
    ) {
        requireTenantContext(tenantContext);

        authorizationService.requirePermission(
                new AuthorizationRequest(
                        tenantContext,
                        ServiceProfitPermissions.OPPORTUNITY_READ,
                        AuthorizationResourceType.SERVICE_PROFIT_OPPORTUNITY,
                        opportunityId
                )
        );

        return repository.findByIdAndTenantId(
                opportunityId,
                tenantContext.tenantId()
        ).orElseThrow(() -> new ResponseStatusException(
                NOT_FOUND,
                "Service Profit Opportunity not found"
        ));
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
