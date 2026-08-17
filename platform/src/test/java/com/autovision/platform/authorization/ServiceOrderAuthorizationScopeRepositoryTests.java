package com.autovision.platform.authorization;

import com.autovision.platform.aftersales.ServiceOrder;
import com.autovision.platform.aftersales.ServiceOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ServiceOrderAuthorizationScopeRepositoryTests {

    @Autowired
    private AuthorizationScopeRepository scopeRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID tenantId;
    private UUID otherTenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        otherTenantId = UUID.randomUUID();

        /*
         * These organization tables already belong to the platform model.
         * CREATE IF NOT EXISTS keeps this focused test independently runnable
         * with the H2 test profile where Flyway is disabled.
         */
        exec("""
                CREATE TABLE IF NOT EXISTS platform.dealers (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    code VARCHAR(255),
                    name VARCHAR(255),
                    primary_location_id UUID,
                    status VARCHAR(32),
                    created_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.branches (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    dealer_id UUID NOT NULL,
                    location_id UUID,
                    code VARCHAR(255),
                    name VARCHAR(255),
                    status VARCHAR(32),
                    created_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.tenant_groups (
                    id UUID PRIMARY KEY,
                    status VARCHAR(32) NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.tenant_group_memberships (
                    id UUID PRIMARY KEY,
                    tenant_group_id UUID NOT NULL,
                    tenant_id UUID NOT NULL
                )
                """);

        exec("""
                CREATE TABLE IF NOT EXISTS platform.dealer_group_memberships (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    dealer_group_id UUID NOT NULL,
                    dealer_id UUID NOT NULL
                )
                """);
    }

    @AfterEach
    void clearTables() {
        serviceOrderRepository.deleteAll();

        exec("DELETE FROM platform.dealer_group_memberships");
        exec("DELETE FROM platform.tenant_group_memberships");
        exec("DELETE FROM platform.tenant_groups");
        exec("DELETE FROM platform.branches");
        exec("DELETE FROM platform.dealers");
    }

    @Test
    void serviceOrderBelongsToItsTenant() {
        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertTrue(
                scopeRepository.serviceOrderBelongsToTenant(
                        order.getId(),
                        tenantId
                )
        );
    }

    @Test
    void serviceOrderDoesNotBelongToAnotherTenant() {
        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToTenant(
                        order.getId(),
                        otherTenantId
                )
        );
    }

    @Test
    void tenantLevelServiceOrderDoesNotBelongToDealer() {
        UUID dealerId =
                insertDealer(tenantId);

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealer(
                        order.getId(),
                        dealerId,
                        tenantId
                )
        );
    }

    @Test
    void serviceOrderBelongsToCorrectDealer() {
        UUID dealerId =
                insertDealer(tenantId);

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertTrue(
                scopeRepository.serviceOrderBelongsToDealer(
                        order.getId(),
                        dealerId,
                        tenantId
                )
        );
    }

    @Test
    void serviceOrderDoesNotBelongToSiblingDealer() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID siblingDealerId =
                insertDealer(tenantId);

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealer(
                        order.getId(),
                        siblingDealerId,
                        tenantId
                )
        );
    }

    @Test
    void dealerContainmentCannotCrossTenantBoundary() {
        UUID dealerId =
                insertDealer(tenantId);

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealer(
                        order.getId(),
                        dealerId,
                        otherTenantId
                )
        );
    }

    @Test
    void serviceOrderBelongsToCorrectBranch() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID branchId =
                insertBranch(
                        tenantId,
                        dealerId
                );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        branchId
                );

        assertTrue(
                scopeRepository.serviceOrderBelongsToBranch(
                        order.getId(),
                        branchId,
                        tenantId
                )
        );
    }

    @Test
    void serviceOrderDoesNotBelongToSiblingBranch() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID branchId =
                insertBranch(
                        tenantId,
                        dealerId
                );

        UUID siblingBranchId =
                insertBranch(
                        tenantId,
                        dealerId
                );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        branchId
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToBranch(
                        order.getId(),
                        siblingBranchId,
                        tenantId
                )
        );
    }

    @Test
    void dealerLevelServiceOrderDoesNotBelongToBranch() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID branchId =
                insertBranch(
                        tenantId,
                        dealerId
                );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToBranch(
                        order.getId(),
                        branchId,
                        tenantId
                )
        );
    }

    @Test
    void branchContainmentCannotCrossTenantBoundary() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID branchId =
                insertBranch(
                        tenantId,
                        dealerId
                );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        branchId
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToBranch(
                        order.getId(),
                        branchId,
                        otherTenantId
                )
        );
    }

    @Test
    void serviceOrderBelongsToDealerGroupWhenDealerIsMember() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID dealerGroupId =
                UUID.randomUUID();

        insertDealerGroupMembership(
                tenantId,
                dealerGroupId,
                dealerId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertTrue(
                scopeRepository.serviceOrderBelongsToDealerGroup(
                        order.getId(),
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void serviceOrderDoesNotBelongToDealerGroupWhenDealerIsNotMember() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID dealerGroupId =
                UUID.randomUUID();

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealerGroup(
                        order.getId(),
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void tenantLevelServiceOrderDoesNotAcquireDealerGroupContainment() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID dealerGroupId =
                UUID.randomUUID();

        insertDealerGroupMembership(
                tenantId,
                dealerGroupId,
                dealerId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealerGroup(
                        order.getId(),
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void dealerGroupContainmentCannotUseMembershipFromAnotherTenant() {
        UUID dealerId =
                insertDealer(tenantId);

        UUID dealerGroupId =
                UUID.randomUUID();

        insertDealerGroupMembership(
                otherTenantId,
                dealerGroupId,
                dealerId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        dealerId,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToDealerGroup(
                        order.getId(),
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void activeTenantGroupContainsMemberTenantServiceOrder() {
        UUID tenantGroupId =
                insertTenantGroup("ACTIVE");

        insertTenantGroupMembership(
                tenantGroupId,
                tenantId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertTrue(
                scopeRepository.serviceOrderBelongsToActiveTenantGroup(
                        order.getId(),
                        tenantGroupId
                )
        );
    }

    @Test
    void tenantGroupDoesNotContainNonMemberTenantServiceOrder() {
        UUID tenantGroupId =
                insertTenantGroup("ACTIVE");

        insertTenantGroupMembership(
                tenantGroupId,
                otherTenantId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToActiveTenantGroup(
                        order.getId(),
                        tenantGroupId
                )
        );
    }

    @Test
    void inactiveTenantGroupDoesNotContainServiceOrder() {
        UUID tenantGroupId =
                insertTenantGroup("INACTIVE");

        insertTenantGroupMembership(
                tenantGroupId,
                tenantId
        );

        ServiceOrder order =
                persistOrder(
                        tenantId,
                        null,
                        null
                );

        assertFalse(
                scopeRepository.serviceOrderBelongsToActiveTenantGroup(
                        order.getId(),
                        tenantGroupId
                )
        );
    }

    private ServiceOrder persistOrder(
            UUID orderTenantId,
            UUID dealerId,
            UUID branchId
    ) {
        ServiceOrder order = ServiceOrder.open(
                UUID.randomUUID(),
                orderTenantId,
                dealerId,
                branchId,
                "SO-S535C-" + UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                OffsetDateTime.now()
        );

        return serviceOrderRepository.saveAndFlush(order);
    }

    private UUID insertDealer(
            UUID dealerTenantId
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.dealers
                    (
                        id,
                        tenant_id,
                        code,
                        name,
                        primary_location_id,
                        status,
                        created_at,
                        updated_at
                    )
                VALUES
                    (
                        :id,
                        :tenantId,
                        :code,
                        :name,
                        NULL,
                        'ACTIVE',
                        :now,
                        :now
                    )
                """)
                .param("id", id)
                .param("tenantId", dealerTenantId)
                .param("code", "D-" + id)
                .param("name", "Dealer " + id)
                .param("now", OffsetDateTime.now())
                .update();

        return id;
    }

    private UUID insertBranch(
            UUID branchTenantId,
            UUID dealerId
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.branches
                    (
                        id,
                        tenant_id,
                        dealer_id,
                        location_id,
                        code,
                        name,
                        status,
                        created_at,
                        updated_at
                    )
                VALUES
                    (
                        :id,
                        :tenantId,
                        :dealerId,
                        NULL,
                        :code,
                        :name,
                        'ACTIVE',
                        :now,
                        :now
                    )
                """)
                .param("id", id)
                .param("tenantId", branchTenantId)
                .param("dealerId", dealerId)
                .param("code", "B-" + id)
                .param("name", "Branch " + id)
                .param("now", OffsetDateTime.now())
                .update();

        return id;
    }

    private void insertDealerGroupMembership(
            UUID membershipTenantId,
            UUID dealerGroupId,
            UUID dealerId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.dealer_group_memberships
                    (
                        id,
                        tenant_id,
                        dealer_group_id,
                        dealer_id
                    )
                VALUES
                    (
                        :id,
                        :tenantId,
                        :dealerGroupId,
                        :dealerId
                    )
                """)
                .param("id", UUID.randomUUID())
                .param("tenantId", membershipTenantId)
                .param("dealerGroupId", dealerGroupId)
                .param("dealerId", dealerId)
                .update();
    }

    private UUID insertTenantGroup(
            String status
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.tenant_groups
                    (id, status)
                VALUES
                    (:id, :status)
                """)
                .param("id", id)
                .param("status", status)
                .update();

        return id;
    }

    private void insertTenantGroupMembership(
            UUID tenantGroupId,
            UUID memberTenantId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.tenant_group_memberships
                    (
                        id,
                        tenant_group_id,
                        tenant_id
                    )
                VALUES
                    (
                        :id,
                        :tenantGroupId,
                        :tenantId
                    )
                """)
                .param("id", UUID.randomUUID())
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", memberTenantId)
                .update();
    }

    private void exec(
            String sql
    ) {
        jdbcClient.sql(sql).update();
    }
}