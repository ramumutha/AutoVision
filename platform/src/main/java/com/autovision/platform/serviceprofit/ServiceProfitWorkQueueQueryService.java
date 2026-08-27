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
public class ServiceProfitWorkQueueQueryService {
    private final AuthorizationRepository authorizationRepository;
    private final ServiceProfitWorkQueueQueryRepository queryRepository;

    public ServiceProfitWorkQueueQueryService(AuthorizationRepository authorizationRepository,
                                              ServiceProfitWorkQueueQueryRepository queryRepository) {
        this.authorizationRepository = authorizationRepository;
        this.queryRepository = queryRepository;
    }

    public ServiceProfitWorkQueuePage findPage(AuthenticatedTenantContext context,
                                               ServiceProfitWorkQueueQuery query) {
        List<AuthorizationGrant> grants = authorizationRepository.findActivePermissionGrants(
                context.userRefId(), context.tenantId(), ServiceProfitPermissions.FOLLOW_UP_READ);
        if (grants.isEmpty()) throw new AccessDeniedException("Access is denied");
        return queryRepository.findAuthorizedPage(context.tenantId(), context.userRefId(), grants, query);
    }
}