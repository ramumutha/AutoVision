package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitWorkQueueQueryServiceTests {
    @Test
    void readsWithFollowUpReadPermissionWithoutRequiringManage() {
        AuthorizationRepository authorization = mock(AuthorizationRepository.class);
        ServiceProfitWorkQueueQueryRepository repository = mock(ServiceProfitWorkQueueQueryRepository.class);
        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        AuthenticatedTenantContext context = new AuthenticatedTenantContext(principalId, tenantId, "user");
        ServiceProfitWorkQueueQuery query = new ServiceProfitWorkQueueQuery(null, null, null, null, 0, 25);
        when(authorization.findActivePermissionGrants(principalId, tenantId, ServiceProfitPermissions.FOLLOW_UP_READ))
                .thenReturn(List.of(new AuthorizationGrant(com.autovision.platform.authorization.AuthorizationScopeType.TENANT, tenantId)));
        when(repository.findAuthorizedPage(eq(tenantId), eq(principalId), any(), eq(query)))
                .thenReturn(ServiceProfitWorkQueuePage.of(List.of(), 0, 25, 0));

        new ServiceProfitWorkQueueQueryService(authorization, repository).findPage(context, query);

        verify(authorization).findActivePermissionGrants(principalId, tenantId, ServiceProfitPermissions.FOLLOW_UP_READ);
        verify(repository).findAuthorizedPage(eq(tenantId), eq(principalId), any(), eq(query));
    }

    @Test
    void deniesWithoutFollowUpReadPermission() {
        AuthorizationRepository authorization = mock(AuthorizationRepository.class);
        ServiceProfitWorkQueueQueryRepository repository = mock(ServiceProfitWorkQueueQueryRepository.class);
        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        when(authorization.findActivePermissionGrants(principalId, tenantId, ServiceProfitPermissions.FOLLOW_UP_READ))
                .thenReturn(List.of());

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                new ServiceProfitWorkQueueQueryService(authorization, repository).findPage(
                        new AuthenticatedTenantContext(principalId, tenantId, "user"),
                        new ServiceProfitWorkQueueQuery(null, null, null, null, 0, 25)));
    }
}