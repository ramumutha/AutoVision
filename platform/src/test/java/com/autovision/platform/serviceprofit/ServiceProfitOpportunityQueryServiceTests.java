package com.autovision.platform.serviceprofit;

import com.autovision.platform.authorization.AuthorizationGrant;
import com.autovision.platform.authorization.AuthorizationRepository;
import com.autovision.platform.authorization.AuthorizationScopeType;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceProfitOpportunityQueryServiceTests {

    private AuthorizationRepository authorizationRepository;
    private ServiceProfitOpportunityQueryRepository queryRepository;
    private ServiceProfitOpportunityQueryService service;

    private AuthenticatedTenantContext context;

    @BeforeEach
    void setUp() {
        authorizationRepository =
                mock(AuthorizationRepository.class);

        queryRepository =
                mock(ServiceProfitOpportunityQueryRepository.class);

        service =
                new ServiceProfitOpportunityQueryService(
                        authorizationRepository,
                        queryRepository
                );

        context = new AuthenticatedTenantContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "service-profit-manager"
        );
    }

    @Test
    void deniesQueueWhenNoReadGrantExists() {

        when(
                authorizationRepository
                        .findActivePermissionGrants(
                                context.userRefId(),
                                context.tenantId(),
                                ServiceProfitPermissions.OPPORTUNITY_READ
                        )
        ).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.findPage(
                        context,
                        query()
                )
        );
    }

    @Test
    void delegatesAuthorizedQueueToDatabaseRepository() {

        AuthorizationGrant grant =
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        context.tenantId()
                );

        List<AuthorizationGrant> grants =
                List.of(grant);

        ServiceProfitOpportunityQuery query =
                query();

        ServiceProfitOpportunityPage expected =
                ServiceProfitOpportunityPage.of(
                        List.of(),
                        0,
                        25,
                        0
                );

        when(
                authorizationRepository
                        .findActivePermissionGrants(
                                context.userRefId(),
                                context.tenantId(),
                                ServiceProfitPermissions.OPPORTUNITY_READ
                        )
        ).thenReturn(grants);

        when(
                queryRepository.findAuthorizedPage(
                        context.tenantId(),
                        grants,
                        query
                )
        ).thenReturn(expected);

        ServiceProfitOpportunityPage actual =
                service.findPage(
                        context,
                        query
                );

        assertSame(expected, actual);

        verify(queryRepository)
                .findAuthorizedPage(
                        context.tenantId(),
                        grants,
                        query
                );
    }

    @Test
    void deniesSummaryWhenNoReadGrantExists() {

        when(
                authorizationRepository
                        .findActivePermissionGrants(
                                context.userRefId(),
                                context.tenantId(),
                                ServiceProfitPermissions.OPPORTUNITY_READ
                        )
        ).thenReturn(List.of());

        assertThrows(
                AccessDeniedException.class,
                () -> service.findSummary(
                        context,
                        query()
                )
        );
    }

    @Test
    void delegatesAuthorizedSummaryToDatabaseRepository() {

        AuthorizationGrant grant =
                new AuthorizationGrant(
                        AuthorizationScopeType.TENANT,
                        context.tenantId()
                );

        List<AuthorizationGrant> grants =
                List.of(grant);

        ServiceProfitOpportunityQuery query =
                query();

        ServiceProfitOpportunitySummary expected =
                new ServiceProfitOpportunitySummary(
                        0,
                        0,
                        0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(
                authorizationRepository
                        .findActivePermissionGrants(
                                context.userRefId(),
                                context.tenantId(),
                                ServiceProfitPermissions.OPPORTUNITY_READ
                        )
        ).thenReturn(grants);

        when(
                queryRepository.findAuthorizedSummary(
                        context.tenantId(),
                        grants,
                        query
                )
        ).thenReturn(expected);

        ServiceProfitOpportunitySummary actual =
                service.findSummary(
                        context,
                        query
                );

        assertSame(expected, actual);

        verify(queryRepository)
                .findAuthorizedSummary(
                        context.tenantId(),
                        grants,
                        query
                );
    }

    private ServiceProfitOpportunityQuery query() {
        return new ServiceProfitOpportunityQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                25,
                ServiceProfitOpportunitySort.DETECTED_DESC
        );
    }
}
