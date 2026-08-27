package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationRequest;
import com.autovision.platform.authorization.AuthorizationResourceType;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ServiceProfitFollowUpReadService {
    private final AuthorizationService authorizationService;
    private final ServiceProfitOpportunityRepository opportunityRepository;
    private final ServiceProfitFollowUpRepository followUpRepository;
    private final ServiceProfitFollowUpHistoryRepository historyRepository;

    public ServiceProfitFollowUpReadService(AuthorizationService authorizationService,
                                            ServiceProfitOpportunityRepository opportunityRepository,
                                            ServiceProfitFollowUpRepository followUpRepository,
                                            ServiceProfitFollowUpHistoryRepository historyRepository) {
        this.authorizationService = authorizationService;
        this.opportunityRepository = opportunityRepository;
        this.followUpRepository = followUpRepository;
        this.historyRepository = historyRepository;
    }

    public ServiceProfitFollowUpResponse readCurrent(AuthenticatedTenantContext context, UUID opportunityId) {
        authorize(context, opportunityId);
        ServiceProfitFollowUp followUp = findFollowUp(context, opportunityId);
        return ServiceProfitFollowUpResponse.from(followUp, context.userRefId(), OffsetDateTime.now());
    }

    public List<ServiceProfitFollowUpHistoryResponse> readHistory(AuthenticatedTenantContext context, UUID opportunityId) {
        authorize(context, opportunityId);
        ServiceProfitFollowUp followUp = findFollowUp(context, opportunityId);
        return historyRepository.findAllByTenantIdAndFollowUpIdOrderByOccurredAtAscIdAsc(context.tenantId(), followUp.getId())
                .stream().map(history -> ServiceProfitFollowUpHistoryResponse.from(history, context.userRefId())).toList();
    }

    private ServiceProfitFollowUp findFollowUp(AuthenticatedTenantContext context, UUID opportunityId) {
        return followUpRepository.findByTenantIdAndOpportunityId(context.tenantId(), opportunityId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Service Profit follow-up not found"));
    }

    private void authorize(AuthenticatedTenantContext context, UUID opportunityId) {
        if (context == null || context.tenantId() == null || context.userRefId() == null || opportunityId == null) {
            throw new ResponseStatusException(NOT_FOUND, "Service Profit opportunity not found");
        }
        authorizationService.requirePermission(new AuthorizationRequest(context, ServiceProfitPermissions.FOLLOW_UP_READ,
                AuthorizationResourceType.SERVICE_PROFIT_OPPORTUNITY, opportunityId));
        opportunityRepository.findByIdAndTenantId(opportunityId, context.tenantId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Service Profit opportunity not found"));
    }
}