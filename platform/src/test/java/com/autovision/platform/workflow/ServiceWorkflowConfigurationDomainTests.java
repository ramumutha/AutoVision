package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceWorkflowConfigurationDomainTests {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void createsTenantScopedWorkflowDefinition() {

        ServiceWorkflowDefinition definition = definition(
                null,
                null
        );

        assertEquals(tenantId, definition.getTenantId());
        assertEquals("STANDARD_SERVICE", definition.getCode());
        assertEquals("Standard Service", definition.getDisplayName());
        assertTrue(definition.isActive());
        assertEquals(principalId, definition.getCreatedByPrincipalId());
        assertEquals(now, definition.getCreatedAt());
        assertEquals(principalId, definition.getUpdatedByPrincipalId());
        assertEquals(now, definition.getUpdatedAt());
    }

    @Test
    void requiresTenantForWorkflowDefinition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowDefinition.create(
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        "STANDARD_SERVICE",
                        "Standard Service",
                        principalId,
                        now
                )
        );
    }

    @Test
    void requiresDealerWhenBranchIsSpecified() {

        assertThrows(
                IllegalArgumentException.class,
                () -> definition(
                        null,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void requiresStableWorkflowCodeAndDisplayName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowDefinition.create(
                        UUID.randomUUID(),
                        tenantId,
                        null,
                        null,
                        " ",
                        "Standard Service",
                        principalId,
                        now
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowDefinition.create(
                        UUID.randomUUID(),
                        tenantId,
                        null,
                        null,
                        "STANDARD_SERVICE",
                        "",
                        principalId,
                        now
                )
        );
    }

    @Test
    void createsDraftWorkflowVersionWithPositiveVersionNumber() {

        ServiceWorkflowVersion version = version(2);

        assertEquals(2, version.getVersionNumber());
        assertEquals(
                ServiceWorkflowVersionStatus.DRAFT,
                version.getStatus()
        );
        assertEquals(principalId, version.getCreatedByPrincipalId());
        assertEquals(now, version.getCreatedAt());
        assertEquals(0, version.getLockVersion());
    }

    @Test
    void rejectsNonPositiveWorkflowVersionNumber() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowVersion.draft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        0,
                        principalId,
                        now
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowVersion.draft(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        -1,
                        principalId,
                        now
                )
        );
    }

    @Test
    void publishesDraftVersionWithAuditFields() {

        ServiceWorkflowVersion version = version(1);
        UUID publishingPrincipalId = UUID.randomUUID();
        OffsetDateTime publishedAt = now.plusMinutes(5);

        version.publish(
                publishingPrincipalId,
                publishedAt
        );

        assertEquals(
                ServiceWorkflowVersionStatus.PUBLISHED,
                version.getStatus()
        );
        assertEquals(
                publishingPrincipalId,
                version.getPublishedByPrincipalId()
        );
        assertEquals(publishedAt, version.getPublishedAt());
    }

    @Test
    void rejectsDuplicatePublishAttempt() {

        ServiceWorkflowVersion version = version(1);
        version.publish(
                UUID.randomUUID(),
                now.plusMinutes(1)
        );

        assertThrows(
                IllegalStateException.class,
                () -> version.publish(
                        UUID.randomUUID(),
                        now.plusMinutes(2)
                )
        );
    }

    @Test
    void retiresPublishedVersionOnly() {

        ServiceWorkflowVersion version = version(1);

        assertThrows(
                IllegalStateException.class,
                version::retire
        );

        version.publish(
                UUID.randomUUID(),
                now.plusMinutes(1)
        );
        version.retire();

        assertEquals(
                ServiceWorkflowVersionStatus.RETIRED,
                version.getStatus()
        );

        assertThrows(
                IllegalStateException.class,
                version::retire
        );
    }

    @Test
    void createsStageAndPreservesSequence() {

        UUID versionId = UUID.randomUUID();
        ServiceWorkflowStage stage = ServiceWorkflowStage.create(
                UUID.randomUUID(),
                versionId,
                "CHECK_IN",
                "Check In",
                20
        );

        assertEquals(versionId, stage.getWorkflowVersionId());
        assertEquals("CHECK_IN", stage.getCode());
        assertEquals("Check In", stage.getDisplayName());
        assertEquals(20, stage.getSequence());
        assertTrue(stage.isActive());
    }

    @Test
    void rejectsNegativeStageSequence() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowStage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "CHECK_IN",
                        "Check In",
                        -1
                )
        );
    }

    @Test
    void createsStatusAndPreservesSequence() {

        UUID stageId = UUID.randomUUID();
        ServiceWorkflowStatus status = ServiceWorkflowStatus.create(
                UUID.randomUUID(),
                stageId,
                "AWAITING_APPROVAL",
                "Awaiting Approval",
                30
        );

        assertEquals(stageId, status.getWorkflowStageId());
        assertEquals("AWAITING_APPROVAL", status.getCode());
        assertEquals("Awaiting Approval", status.getDisplayName());
        assertEquals(30, status.getSequence());
        assertTrue(status.isActive());
    }

    @Test
    void rejectsNegativeStatusSequence() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ServiceWorkflowStatus.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "AWAITING_APPROVAL",
                        "Awaiting Approval",
                        -1
                )
        );
    }

    @Test
    void configurableWorkflowStatusIsIndependentFromServiceOrderStatus() {

        ServiceWorkflowStatus status = ServiceWorkflowStatus.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "READY_FOR_ASSIGNMENT",
                "Ready for Assignment",
                10
        );

        assertNotNull(status);
        assertEquals("READY_FOR_ASSIGNMENT", status.getCode());
        assertFalse(
                status.getCode().equals(ServiceOrderStatus.OPEN.name())
        );
        assertNotEquals(ServiceOrderStatus.class, status.getClass());
    }

    private ServiceWorkflowDefinition definition(
            UUID dealerId,
            UUID branchId
    ) {
        return ServiceWorkflowDefinition.create(
                UUID.randomUUID(),
                tenantId,
                dealerId,
                branchId,
                "STANDARD_SERVICE",
                "Standard Service",
                principalId,
                now
        );
    }

    private ServiceWorkflowVersion version(long versionNumber) {
        return ServiceWorkflowVersion.draft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                versionNumber,
                principalId,
                now
        );
    }
}
