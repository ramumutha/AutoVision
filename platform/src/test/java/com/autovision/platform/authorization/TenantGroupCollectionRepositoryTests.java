package com.autovision.platform.authorization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.autovision.platform.organization.Branch;
import com.autovision.platform.organization.BranchRepository;
import com.autovision.platform.organization.Dealer;
import com.autovision.platform.organization.DealerRepository;
import com.autovision.platform.organization.Location;
import com.autovision.platform.organization.LocationRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security-hardening integration tests for the E.4 TENANT_GROUP collection
 * repository boundary. These tests execute the real native repository SQL
 * against the test database rather than mocking collection behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantGroupCollectionRepositoryTests {

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private JdbcClient jdbcClient;

    private final UUID callerTenantId = UUID.randomUUID();
    private final UUID memberTenantId = UUID.randomUUID();
    private final UUID secondMemberTenantId = UUID.randomUUID();
    private final UUID outsiderTenantId = UUID.randomUUID();

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
    }

    @AfterEach
    void clearTables() {
        exec("DELETE FROM platform.tenant_group_memberships");
        exec("DELETE FROM platform.tenant_groups");
        exec("DELETE FROM platform.branches");
        exec("DELETE FROM platform.locations");
        exec("DELETE FROM platform.dealers");
    }

    @Test
    void activeGroupReturnsResourcesFromCallerAndMemberTenants() {
        UUID groupId = insertTenantGroup("ACTIVE");
        insertMembership(groupId, callerTenantId);
        insertMembership(groupId, memberTenantId);

        UUID callerDealer = insertDealer(callerTenantId);
        UUID memberDealer = insertDealer(memberTenantId);
        UUID callerLocation = insertLocation(callerTenantId);
        UUID memberLocation = insertLocation(memberTenantId);
        UUID callerBranch = insertBranch(
                callerTenantId, callerDealer, callerLocation
        );
        UUID memberBranch = insertBranch(
                memberTenantId, memberDealer, memberLocation
        );

        assertEquals(
                Set.of(callerDealer, memberDealer),
                dealerIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(callerBranch, memberBranch),
                branchIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(callerLocation, memberLocation),
                locationIds(groupId, callerTenantId)
        );
    }

    @Test
    void inactiveGroupReturnsNoResources() {
        UUID groupId = insertTenantGroup("INACTIVE");
        insertMembership(groupId, callerTenantId);
        insertMembership(groupId, memberTenantId);

        UUID dealerId = insertDealer(memberTenantId);
        UUID locationId = insertLocation(memberTenantId);
        insertBranch(memberTenantId, dealerId, locationId);

        assertAllCollectionsEmpty(groupId, callerTenantId);
    }

    @Test
    void callerOutsideGroupReturnsNoResources() {
        UUID groupId = insertTenantGroup("ACTIVE");
        insertMembership(groupId, memberTenantId);

        UUID dealerId = insertDealer(memberTenantId);
        UUID locationId = insertLocation(memberTenantId);
        insertBranch(memberTenantId, dealerId, locationId);

        assertAllCollectionsEmpty(groupId, callerTenantId);
    }

    @Test
    void targetTenantOutsideGroupIsNotReturned() {
        UUID groupId = insertTenantGroup("ACTIVE");
        insertMembership(groupId, callerTenantId);
        insertMembership(groupId, memberTenantId);

        UUID allowedDealer = insertDealer(memberTenantId);
        UUID deniedDealer = insertDealer(outsiderTenantId);

        UUID allowedLocation = insertLocation(memberTenantId);
        UUID deniedLocation = insertLocation(outsiderTenantId);

        UUID allowedBranch = insertBranch(
                memberTenantId, allowedDealer, allowedLocation
        );
        insertBranch(
                outsiderTenantId, deniedDealer, deniedLocation
        );

        assertEquals(
                Set.of(allowedDealer),
                dealerIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(allowedBranch),
                branchIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(allowedLocation),
                locationIds(groupId, callerTenantId)
        );
    }

    @Test
    void membershipInDifferentGroupDoesNotLeakResources() {
        UUID authorizedGroup = insertTenantGroup("ACTIVE");
        UUID unrelatedGroup = insertTenantGroup("ACTIVE");

        insertMembership(authorizedGroup, callerTenantId);
        insertMembership(unrelatedGroup, outsiderTenantId);

        UUID dealerId = insertDealer(outsiderTenantId);
        UUID locationId = insertLocation(outsiderTenantId);
        insertBranch(outsiderTenantId, dealerId, locationId);

        assertAllCollectionsEmpty(authorizedGroup, callerTenantId);
    }

    @Test
    void multipleMemberTenantsAreReturned() {
        UUID groupId = insertTenantGroup("ACTIVE");
        insertMembership(groupId, callerTenantId);
        insertMembership(groupId, memberTenantId);
        insertMembership(groupId, secondMemberTenantId);

        UUID dealerOne = insertDealer(memberTenantId);
        UUID dealerTwo = insertDealer(secondMemberTenantId);

        UUID locationOne = insertLocation(memberTenantId);
        UUID locationTwo = insertLocation(secondMemberTenantId);

        UUID branchOne = insertBranch(
                memberTenantId, dealerOne, locationOne
        );
        UUID branchTwo = insertBranch(
                secondMemberTenantId, dealerTwo, locationTwo
        );

        assertEquals(
                Set.of(dealerOne, dealerTwo),
                dealerIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(branchOne, branchTwo),
                branchIds(groupId, callerTenantId)
        );
        assertEquals(
                Set.of(locationOne, locationTwo),
                locationIds(groupId, callerTenantId)
        );
    }

    @Test
    void duplicateMembershipJoinPathsDoNotDuplicateResources() {
        UUID groupId = insertTenantGroup("ACTIVE");
        insertMembership(groupId, callerTenantId);
        insertMembership(groupId, memberTenantId);
        insertMembership(groupId, memberTenantId);

        UUID dealerId = insertDealer(memberTenantId);
        UUID locationId = insertLocation(memberTenantId);
        UUID branchId = insertBranch(
                memberTenantId, dealerId, locationId
        );

        List<Dealer> dealers =
                dealerRepository.findAllWithinActiveTenantGroup(
                        groupId, callerTenantId
                );

        List<Branch> branches =
                branchRepository.findAllWithinActiveTenantGroup(
                        groupId, callerTenantId
                );

        List<Location> locations =
                locationRepository.findAllWithinActiveTenantGroup(
                        groupId, callerTenantId
                );

        assertEquals(1, dealers.size());
        assertEquals(dealerId, dealers.getFirst().getId());

        assertEquals(1, branches.size());
        assertEquals(branchId, branches.getFirst().getId());

        assertEquals(1, locations.size());
        assertEquals(locationId, locations.getFirst().getId());
    }

    @Test
    void unknownTenantGroupReturnsNoResources() {
        UUID dealerId = insertDealer(memberTenantId);
        UUID locationId = insertLocation(memberTenantId);
        insertBranch(memberTenantId, dealerId, locationId);

        assertAllCollectionsEmpty(
                UUID.randomUUID(),
                callerTenantId
        );
    }

    private void assertAllCollectionsEmpty(
            UUID groupId,
            UUID authenticatedTenantId
    ) {
        assertTrue(
                dealerRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                ).isEmpty()
        );

        assertTrue(
                branchRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                ).isEmpty()
        );

        assertTrue(
                locationRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                ).isEmpty()
        );
    }

    private Set<UUID> dealerIds(
            UUID groupId,
            UUID authenticatedTenantId
    ) {
        return dealerRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                )
                .stream()
                .map(Dealer::getId)
                .collect(Collectors.toSet());
    }

    private Set<UUID> branchIds(
            UUID groupId,
            UUID authenticatedTenantId
    ) {
        return branchRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                )
                .stream()
                .map(Branch::getId)
                .collect(Collectors.toSet());
    }

    private Set<UUID> locationIds(
            UUID groupId,
            UUID authenticatedTenantId
    ) {
        return locationRepository.findAllWithinActiveTenantGroup(
                        groupId, authenticatedTenantId
                )
                .stream()
                .map(Location::getId)
                .collect(Collectors.toSet());
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

    private void insertMembership(
            UUID tenantGroupId,
            UUID tenantId
    ) {
        jdbcClient.sql("""
                INSERT INTO platform.tenant_group_memberships
                    (id, tenant_group_id, tenant_id)
                VALUES (:id, :tenantGroupId, :tenantId)
                """)
                .param("id", UUID.randomUUID())
                .param("tenantGroupId", tenantGroupId)
                .param("tenantId", tenantId)
                .update();
    }

    private UUID insertDealer(UUID tenantId) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbcClient.sql("""
                INSERT INTO platform.dealers
                    (id, tenant_id, code, name, primary_location_id,
                     status, created_at, updated_at)
                VALUES (:id, :tenantId, :code, :name, NULL,
                        'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("code", "D-" + id)
                .param("name", "Tenant Group Dealer " + id)
                .param("now", now)
                .update();

        return id;
    }

    private UUID insertLocation(UUID tenantId) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbcClient.sql("""
                INSERT INTO platform.locations
                    (id, tenant_id, code, name, status, created_at, updated_at)
                VALUES (:id, :tenantId, :code, :name,
                        'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("code", "L-" + id)
                .param("name", "Tenant Group Location " + id)
                .param("now", now)
                .update();

        return id;
    }

    private UUID insertBranch(
            UUID tenantId,
            UUID dealerId,
            UUID locationId
    ) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        jdbcClient.sql("""
                INSERT INTO platform.branches
                    (id, tenant_id, dealer_id, location_id, code, name,
                     status, created_at, updated_at)
                VALUES (:id, :tenantId, :dealerId, :locationId, :code, :name,
                        'ACTIVE', :now, :now)
                """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("dealerId", dealerId)
                .param("locationId", locationId)
                .param("code", "B-" + id)
                .param("name", "Tenant Group Branch " + id)
                .param("now", now)
                .update();

        return id;
    }

    private void exec(String sql) {
        jdbcClient.sql(sql).update();
    }
}
