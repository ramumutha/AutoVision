package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceWorkflowTransitionRequirementTests {

    @Test
    void createsAndPreservesConfigurationValues() {
        UUID id = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID transitionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        ServiceWorkflowTransitionRequirement requirement =
                ServiceWorkflowTransitionRequirement.create(
                        id,
                        versionId,
                        transitionId,
                        new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION"),
                        ProcessRequirementMode.REQUIRED,
                        createdAt,
                        principalId
                );

        assertEquals(id, requirement.getId());
        assertEquals(versionId, requirement.getWorkflowVersionId());
        assertEquals(transitionId, requirement.getWorkflowTransitionId());
        assertEquals(
                new ProcessRequirementKey("SERVICE.CUSTOMER_AUTHORIZATION"),
                requirement.getRequirementKey()
        );
        assertEquals(ProcessRequirementMode.REQUIRED, requirement.getRequirementMode());
        assertEquals(createdAt, requirement.getCreatedAt());
        assertEquals(principalId, requirement.getCreatedByPrincipalId());
    }

    @Test
    void rejectsMissingIdentityKeyModeAndTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> create(null, UUID.randomUUID(), UUID.randomUUID(), new ProcessRequirementKey("A"), ProcessRequirementMode.REQUIRED, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), null, UUID.randomUUID(), new ProcessRequirementKey("A"), ProcessRequirementMode.REQUIRED, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), null, new ProcessRequirementKey("A"), ProcessRequirementMode.REQUIRED, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, ProcessRequirementMode.REQUIRED, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new ProcessRequirementKey("A"), null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new ProcessRequirementKey("A"), ProcessRequirementMode.REQUIRED, null));
        assertThrows(IllegalArgumentException.class, () -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new ProcessRequirementKey("   "), ProcessRequirementMode.REQUIRED, OffsetDateTime.now()));
    }

    private ServiceWorkflowTransitionRequirement create(
            UUID id,
            UUID versionId,
            UUID transitionId,
            ProcessRequirementKey key,
            ProcessRequirementMode mode,
            OffsetDateTime createdAt
    ) {
        return ServiceWorkflowTransitionRequirement.create(
                id,
                versionId,
                transitionId,
                key,
                mode,
                createdAt,
                UUID.randomUUID()
        );
    }
}