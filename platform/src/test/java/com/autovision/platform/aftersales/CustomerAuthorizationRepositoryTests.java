package com.autovision.platform.aftersales;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CustomerAuthorizationRepositoryTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private CustomerAuthorizationRepository repository;

    @AfterEach
    void clearAuthorizations() {
        repository.deleteAll();
    }

    @Test
    void persistsAndReloadsRequestedAuthorizationWithSnapshots()
            throws Exception {

        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID aftersalesCaseId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        JsonNode scopeSnapshot = OBJECT_MAPPER.readTree("""
                {
                  "scopeVersion": 1,
                  "items": [
                    {
                      "reference": "ITEM-1",
                      "description": "Front brake pad replacement"
                    }
                  ]
                }
                """);

        JsonNode commercialSnapshot = OBJECT_MAPPER.readTree("""
                {
                  "currency": "INR",
                  "subtotal": 8500.00,
                  "tax": 1530.00,
                  "total": 10030.00
                }
                """);

        CustomerAuthorization authorization =
                CustomerAuthorization.request(
                        id,
                        tenantId,
                        dealerId,
                        branchId,
                        aftersalesCaseId,
                        "AUTH-000001",
                        "CUSTOMER-REF-1",
                        "Test Customer",
                        "Authorize proposed brake service",
                        scopeSnapshot,
                        commercialSnapshot,
                        "Quoted pricing is subject to applicable terms.",
                        "Final charges may vary according to approved changes.",
                        principalId,
                        now
                );

        repository.saveAndFlush(authorization);

        Optional<CustomerAuthorization> reloaded =
                repository.findByIdAndTenantId(id, tenantId);

        assertTrue(reloaded.isPresent());

        CustomerAuthorization persisted = reloaded.orElseThrow();

        assertEquals(id, persisted.getId());
        assertEquals(tenantId, persisted.getTenantId());
        assertEquals(dealerId, persisted.getDealerId());
        assertEquals(branchId, persisted.getBranchId());
        assertEquals(
                aftersalesCaseId,
                persisted.getAftersalesCaseId()
        );
        assertEquals(
                "AUTH-000001",
                persisted.getAuthorizationNumber()
        );
        assertEquals(
                CustomerAuthorizationStatus.REQUESTED,
                persisted.getAuthorizationStatus()
        );
        assertEquals(
                "CUSTOMER-REF-1",
                persisted.getCustomerReference()
        );
        assertEquals(
                "Test Customer",
                persisted.getCustomerDisplayNameSnapshot()
        );
        assertEquals(
                "Authorize proposed brake service",
                persisted.getAuthorizationSummary()
        );
        assertEquals(
                scopeSnapshot,
                persisted.getAuthorizationScopeSnapshot()
        );
        assertEquals(
                commercialSnapshot,
                persisted.getCommercialSnapshot()
        );
        assertEquals(
                "Quoted pricing is subject to applicable terms.",
                persisted.getTermsSnapshot()
        );
        assertEquals(
                "Final charges may vary according to approved changes.",
                persisted.getDisclaimerSnapshot()
        );
        assertNotNull(persisted.getRequestedAt());
        assertEquals(principalId, persisted.getCreatedByPrincipalId());
        assertEquals(principalId, persisted.getUpdatedByPrincipalId());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void findByIdAndTenantIdDoesNotReturnAnotherTenantsAuthorization()
            throws Exception {

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        CustomerAuthorization authorization =
                newAuthorization(
                        tenantId,
                        UUID.randomUUID(),
                        "AUTH-TENANT-1"
                );

        repository.saveAndFlush(authorization);

        assertTrue(
                repository.findByIdAndTenantId(
                        authorization.getId(),
                        tenantId
                ).isPresent()
        );

        assertFalse(
                repository.findByIdAndTenantId(
                        authorization.getId(),
                        otherTenantId
                ).isPresent()
        );
    }

    @Test
    void findsAuthorizationByTenantAndAuthorizationNumber()
            throws Exception {

        UUID tenantId = UUID.randomUUID();

        CustomerAuthorization authorization =
                newAuthorization(
                        tenantId,
                        UUID.randomUUID(),
                        "AUTH-NUMBER-1"
                );

        repository.saveAndFlush(authorization);

        assertTrue(
                repository.existsByTenantIdAndAuthorizationNumber(
                        tenantId,
                        "AUTH-NUMBER-1"
                )
        );

        assertTrue(
                repository.findByTenantIdAndAuthorizationNumber(
                        tenantId,
                        "AUTH-NUMBER-1"
                ).isPresent()
        );

        assertFalse(
                repository.existsByTenantIdAndAuthorizationNumber(
                        UUID.randomUUID(),
                        "AUTH-NUMBER-1"
                )
        );
    }

    @Test
    void findsAuthorizationsOnlyForRequestedTenantAndCase()
            throws Exception {

        UUID tenantId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        UUID otherCaseId = UUID.randomUUID();

        repository.saveAndFlush(
                newAuthorization(
                        tenantId,
                        caseId,
                        "AUTH-CASE-1"
                )
        );

        repository.saveAndFlush(
                newAuthorization(
                        tenantId,
                        otherCaseId,
                        "AUTH-CASE-2"
                )
        );

        List<CustomerAuthorization> matches =
                repository.findAllByTenantIdAndAftersalesCaseId(
                        tenantId,
                        caseId
                );

        assertEquals(1, matches.size());
        assertEquals(
                caseId,
                matches.getFirst().getAftersalesCaseId()
        );
        assertEquals(
                "AUTH-CASE-1",
                matches.getFirst().getAuthorizationNumber()
        );
    }

    @Test
    void findsRequestedAuthorizationsByTenantAndStatus()
            throws Exception {

        UUID tenantId = UUID.randomUUID();

        repository.saveAndFlush(
                newAuthorization(
                        tenantId,
                        UUID.randomUUID(),
                        "AUTH-STATUS-1"
                )
        );

        List<CustomerAuthorization> matches =
                repository.findAllByTenantIdAndAuthorizationStatus(
                        tenantId,
                        CustomerAuthorizationStatus.REQUESTED
                );

        assertEquals(1, matches.size());
        assertEquals(
                CustomerAuthorizationStatus.REQUESTED,
                matches.getFirst().getAuthorizationStatus()
        );
    }

    private CustomerAuthorization newAuthorization(
            UUID tenantId,
            UUID aftersalesCaseId,
            String authorizationNumber
    ) throws Exception {

        JsonNode scopeSnapshot = OBJECT_MAPPER.readTree("""
                {
                  "scopeVersion": 1,
                  "items": []
                }
                """);

        return CustomerAuthorization.request(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                aftersalesCaseId,
                authorizationNumber,
                null,
                null,
                "Test customer authorization",
                scopeSnapshot,
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
