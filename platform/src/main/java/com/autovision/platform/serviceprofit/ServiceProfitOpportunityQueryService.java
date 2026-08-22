package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceProfitOpportunityQueryService {

    private final AuthorizationRepository authorizationRepository;
    private final ServiceProfitOpportunityQueryRepository queryRepository;

    public ServiceProfitOpportunityQueryService(
            AuthorizationRepository authorizationRepository,
            ServiceProfitOpportunityQueryRepository queryRepository
    ) {
        this.authorizationRepository =
                authorizationRepository;

        this.queryRepository =
                queryRepository;
    }

    public ServiceProfitOpportunityPage findPage(
            AuthenticatedTenantContext context,
            ServiceProfitOpportunityQuery query
    ) {
        List<AuthorizationGrant> grants =
                requireReadGrants(context);

        return queryRepository.findAuthorizedPage(
                context.tenantId(),
                grants,
                query
        );
    }

    public ServiceProfitOpportunitySummary findSummary(
            AuthenticatedTenantContext context,
            ServiceProfitOpportunityQuery query
    ) {
        List<AuthorizationGrant> grants =
                requireReadGrants(context);

        return queryRepository.findAuthorizedSummary(
                context.tenantId(),
                grants,
                query
        );
    }

    private List<AuthorizationGrant> requireReadGrants(
            AuthenticatedTenantContext context
    ) {
        List<AuthorizationGrant> grants =
                authorizationRepository
                        .findActivePermissionGrants(
                                context.userRefId(),
                                context.tenantId(),
                                ServiceProfitPermissions.OPPORTUNITY_READ
                        );

        if (grants.isEmpty()) {
            throw new AccessDeniedException(
                    "Access is denied"
            );
        }

        return grants;
    }
}