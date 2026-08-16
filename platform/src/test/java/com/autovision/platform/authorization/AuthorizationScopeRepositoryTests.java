package com.autovision.platform.authorization;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real containment SQL against an H2 database. platform.dealers/
 * branches/locations already exist via the JPA entities' ddl-auto schema;
 * dealer_group_memberships is test-only infrastructure (no JPA entity yet).
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthorizationScopeRepositoryTests {

    @Autowired
    private AuthorizationScopeRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID otherTenantId = UUID.randomUUID();

    @BeforeEach
    void createSchema() {
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
        exec("DELETE FROM platform.dealer_group_memberships");
        exec("DELETE FROM platform.tenant_group_memberships");
        exec("DELETE FROM platform.tenant_groups");
        exec("DELETE FROM platform.branches");
        exec("DELETE FROM platform.locations");
        exec("DELETE FROM platform.dealers");
    }

    @Test
    void activeTenantGroupContainsMemberTenantAndItsResources() {
        UUID tenantGroupId = insertTenantGroup("ACTIVE");
        insertTenantGroupMembership(tenantGroupId, otherTenantId);

        UUID dealerId = insertDealer(otherTenantId, null);
        UUID locationId = insertLocation(otherTenantId);
        UUID branchId = insertBranch(otherTenantId, dealerId, locationId);

        assertTrue(repository.tenantBelongsToActiveTenantGroup(
                otherTenantId, tenantGroupId
        ));
        assertTrue(repository.dealerBelongsToActiveTenantGroup(
                dealerId, tenantGroupId
        ));
        assertTrue(repository.branchBelongsToActiveTenantGroup(
                branchId, tenantGroupId
        ));
        assertTrue(repository.locationBelongsToActiveTenantGroup(
                locationId, tenantGroupId
        ));
    }

    @Test
    void tenantGroupDoesNotContainNonMemberTenantResources() {
        UUID tenantGroupId = insertTenantGroup("ACTIVE");
        insertTenantGroupMembership(tenantGroupId, tenantId);

        UUID dealerId = insertDealer(otherTenantId, null);

        assertFalse(repository.tenantBelongsToActiveTenantGroup(
                otherTenantId, tenantGroupId
        ));
        assertFalse(repository.dealerBelongsToActiveTenantGroup(
                dealerId, tenantGroupId
        ));
    }

    @Test
    void inactiveTenantGroupDoesNotAuthorizeMemberTenant() {
        UUID tenantGroupId = insertTenantGroup("INACTIVE");
        insertTenantGroupMembership(tenantGroupId, otherTenantId);

        assertFalse(repository.tenantBelongsToActiveTenantGroup(
                otherTenantId, tenantGroupId
        ));
    }

    @Test
    void dealerBelongsToTenantWhenSameTenant() {
        UUID dealerId = insertDealer(tenantId, null);

        assertTrue(
                repository.dealerBelongsToTenant(dealerId, tenantId)
        );
    }

    @Test
    void dealerDoesNotBelongToAnotherTenant() {
        UUID dealerId = insertDealer(tenantId, null);

        assertFalse(
                repository.dealerBelongsToTenant(dealerId, otherTenantId)
        );
    }

    @Test
    void branchBelongsToCorrectTenant() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);

        assertTrue(
                repository.branchBelongsToTenant(branchId, tenantId)
        );
    }

    @Test
    void branchDoesNotBelongToAnotherTenant() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);

        assertFalse(
                repository.branchBelongsToTenant(branchId, otherTenantId)
        );
    }

    @Test
    void branchBelongsToCorrectDealer() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);

        assertTrue(
                repository.branchBelongsToDealer(branchId, dealerId, tenantId)
        );
    }

    @Test
    void branchDoesNotBelongToSiblingDealer() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID siblingDealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);

        assertFalse(
                repository.branchBelongsToDealer(
                        branchId,
                        siblingDealerId,
                        tenantId
                )
        );
    }

    @Test
    void locationBelongsToCorrectTenant() {
        UUID locationId = insertLocation(tenantId);

        assertTrue(
                repository.locationBelongsToTenant(locationId, tenantId)
        );
    }

    @Test
    void locationDoesNotBelongToAnotherTenant() {
        UUID locationId = insertLocation(tenantId);

        assertFalse(
                repository.locationBelongsToTenant(locationId, otherTenantId)
        );
    }

    @Test
    void locationBelongsToBranchAtThatLocation() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        UUID branchId = insertBranch(tenantId, dealerId, locationId);

        assertTrue(
                repository.locationBelongsToBranch(
                        locationId,
                        branchId,
                        tenantId
                )
        );
    }

    @Test
    void locationDoesNotBelongToSiblingBranch() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        UUID otherLocationId = insertLocation(tenantId);
        UUID branchId = insertBranch(tenantId, dealerId, otherLocationId);

        assertFalse(
                repository.locationBelongsToBranch(
                        locationId,
                        branchId,
                        tenantId
                )
        );
    }

    @Test
    void locationBelongsToDealerViaBranch() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        insertBranch(tenantId, dealerId, locationId);

        assertTrue(
                repository.locationBelongsToDealer(
                        locationId,
                        dealerId,
                        tenantId
                )
        );
    }

    @Test
    void primaryLocationReferenceDoesNotEstablishDealerAuthorizationContainment() {
        UUID locationId = insertLocation(tenantId);
        UUID dealerId = insertDealer(tenantId, locationId);

        assertFalse(
                repository.locationBelongsToDealer(
                        locationId,
                        dealerId,
                        tenantId
                )
        );
    }

    @Test
    void locationDoesNotBelongToWrongDealer() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID otherDealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        insertBranch(tenantId, dealerId, locationId);

        assertFalse(
                repository.locationBelongsToDealer(
                        locationId,
                        otherDealerId,
                        tenantId
                )
        );
    }

    @Test
    void dealerIsMemberOfGroup() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID dealerGroupId = UUID.randomUUID();
        insertDealerGroupMembership(tenantId, dealerGroupId, dealerId);

        assertTrue(
                repository.dealerIsMemberOfGroup(
                        dealerId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void dealerIsNotMemberOfGroupWhenNoMembershipExists() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID dealerGroupId = UUID.randomUUID();

        assertFalse(
                repository.dealerIsMemberOfGroup(
                        dealerId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void dealerGroupMembershipFromAnotherTenantDoesNotSatisfyContainment() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID dealerGroupId = UUID.randomUUID();
        insertDealerGroupMembership(otherTenantId, dealerGroupId, dealerId);

        assertFalse(
                repository.dealerIsMemberOfGroup(
                        dealerId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void branchBelongsToDealerGroupWhenDealerIsMember() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);
        UUID dealerGroupId = UUID.randomUUID();
        insertDealerGroupMembership(tenantId, dealerGroupId, dealerId);

        assertTrue(
                repository.branchBelongsToDealerGroup(
                        branchId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void branchDoesNotBelongToDealerGroupWhenDealerIsNotMember() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID branchId = insertBranch(tenantId, dealerId, null);
        UUID dealerGroupId = UUID.randomUUID();

        assertFalse(
                repository.branchBelongsToDealerGroup(
                        branchId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void locationBelongsToDealerGroupWhenDealerIsMember() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        insertBranch(tenantId, dealerId, locationId);
        UUID dealerGroupId = UUID.randomUUID();
        insertDealerGroupMembership(tenantId, dealerGroupId, dealerId);

        assertTrue(
                repository.locationBelongsToDealerGroup(
                        locationId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void primaryLocationReferenceDoesNotEstablishDealerGroupAuthorizationContainment() {
        UUID locationId = insertLocation(tenantId);
        UUID dealerId = insertDealer(tenantId, locationId);
        UUID dealerGroupId = UUID.randomUUID();

        insertDealerGroupMembership(
                tenantId,
                dealerGroupId,
                dealerId
        );

        assertFalse(
                repository.locationBelongsToDealerGroup(
                        locationId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    @Test
    void locationDoesNotBelongToDealerGroupWhenDealerIsNotMember() {
        UUID dealerId = insertDealer(tenantId, null);
        UUID locationId = insertLocation(tenantId);
        insertBranch(tenantId, dealerId, locationId);
        UUID dealerGroupId = UUID.randomUUID();

        assertFalse(
                repository.locationBelongsToDealerGroup(
                        locationId,
                        dealerGroupId,
                        tenantId
                )
        );
    }

    private UUID insertDealer(UUID dealerTenantId, UUID primaryLocationId) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.dealers
                    (id, tenant_id, code, name, primary_location_id,
                     status, created_at, updated_at)
                VALUES (:id, :tenantId, :code, :name, :primaryLocationId,
                        'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", dealerTenantId)
                .param("code", "D-" + id)
                .param("name", "Test Dealer " + id)
                .param("primaryLocationId", primaryLocationId)
                .param("now", OffsetDateTime.now())
                .update();

        return id;
    }

    private UUID insertBranch(
            UUID branchTenantId,
            UUID dealerId,
            UUID locationId
    ) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.branches
                    (id, tenant_id, dealer_id, location_id, code, name,
                     status, created_at, updated_at)
                VALUES (:id, :tenantId, :dealerId, :locationId, :code, :name,
                        'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", branchTenantId)
                .param("dealerId", dealerId)
                .param("locationId", locationId)
                .param("code", "B-" + id)
                .param("name", "Test Branch " + id)
                .param("now", OffsetDateTime.now())
                .update();

        return id;
    }

    private UUID insertLocation(UUID locationTenantId) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.locations
                    (id, tenant_id, code, name, status, created_at, updated_at)
                VALUES (:id, :tenantId, :code, :name, 'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", locationTenantId)
                .param("code", "L-" + id)
                .param("name", "Test Location " + id)
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
                    (id, tenant_id, dealer_group_id, dealer_id)
                VALUES (:id, :tenantId, :dealerGroupId, :dealerId)
                """)
                .param("id", UUID.randomUUID())
                .param("tenantId", membershipTenantId)
                .param("dealerGroupId", dealerGroupId)
                .param("dealerId", dealerId)
                .update();
    }

    private UUID insertTenantGroup(String status) {
        UUID id = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO platform.tenant_groups (id, status)
                VALUES (:id, :status)
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
                    (id, tenant_group_id, tenant_id)
                VALUES (:id, :tenantGroupId, :tenantId)
                """)
                .param("id", UUID.randomUUID())
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", memberTenantId)
                .update();
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }
}
