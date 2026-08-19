package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ServiceOrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceWorkflowTransitionDomainTests {

    private final UUID id = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID fromStageId = UUID.randomUUID();
    private final UUID fromStatusId = UUID.randomUUID();
    private final UUID toStageId = UUID.randomUUID();
    private final UUID toStatusId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void createsSameStageDifferentStatusTransition() {
        ServiceWorkflowTransition transition = create(fromStageId, fromStatusId,
                fromStageId, toStatusId);

        assertEquals(versionId, transition.getWorkflowVersionId());
        assertEquals(fromStageId, transition.getFromStageId());
        assertEquals(fromStatusId, transition.getFromStatusId());
        assertEquals(fromStageId, transition.getToStageId());
        assertEquals(toStatusId, transition.getToStatusId());
    }

    @Test
    void createsCrossStageTransition() {
        ServiceWorkflowTransition transition = create(fromStageId, fromStatusId,
                toStageId, toStatusId);

        assertEquals("SEND_TO_QC", transition.getCode());
        assertEquals("Send to quality control", transition.getDisplayName());
    }

    @Test
    void initializesActiveAuditAndVersionState() {
        ServiceWorkflowTransition transition = create(fromStageId, fromStatusId,
                toStageId, toStatusId);

        assertTrue(transition.isActive());
        assertEquals(principalId, transition.getCreatedByPrincipalId());
        assertEquals(principalId, transition.getUpdatedByPrincipalId());
        assertEquals(now, transition.getCreatedAt());
        assertEquals(now, transition.getUpdatedAt());
        assertEquals(0, transition.getVersion());
    }

    @Test
    void rejectsMissingRequiredIds() {
        assertMissing(null, versionId, fromStageId, fromStatusId, toStageId, toStatusId);
        assertMissing(id, null, fromStageId, fromStatusId, toStageId, toStatusId);
        assertMissing(id, versionId, null, fromStatusId, toStageId, toStatusId);
        assertMissing(id, versionId, fromStageId, null, toStageId, toStatusId);
        assertMissing(id, versionId, fromStageId, fromStatusId, null, toStatusId);
        assertMissing(id, versionId, fromStageId, fromStatusId, toStageId, null);
    }

    @Test
    void rejectsBlankCodeDisplayNameAndNegativeSequence() {
        assertThrows(IllegalArgumentException.class, () -> create(
                fromStageId, fromStatusId, toStageId, toStatusId,
                " ", "Display", 0));
        assertThrows(IllegalArgumentException.class, () -> create(
                fromStageId, fromStatusId, toStageId, toStatusId,
                "CODE", " ", 0));
        assertThrows(IllegalArgumentException.class, () -> create(
                fromStageId, fromStatusId, toStageId, toStatusId,
                "CODE", "Display", -1));
    }

    @Test
    void rejectsCompleteSelfLoop() {
        assertThrows(IllegalArgumentException.class, () -> create(
                fromStageId, fromStatusId, fromStageId, fromStatusId));
    }

    @Test
    void codeHasNoPublicMutationApi() {
        assertFalse(Arrays.stream(ServiceWorkflowTransition.class.getMethods())
                .map(Method::getName)
                .anyMatch(name -> name.equals("setCode") || name.equals("setDisplayName")));
    }

    @Test
    void hasNoJpaRelationshipsOrServiceOrderStatusCoupling() {
        assertTrue(ServiceWorkflowTransition.class.isAnnotationPresent(Entity.class));
        for (Field field : ServiceWorkflowTransition.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(OneToMany.class));
            assertFalse(field.isAnnotationPresent(OneToOne.class));
        }
        assertEquals(5, ServiceOrderStatus.values().length);
    }

    private ServiceWorkflowTransition create(
            UUID fromStage,
            UUID fromStatus,
            UUID toStage,
            UUID toStatus
    ) {
        return create(fromStage, fromStatus, toStage, toStatus,
                "SEND_TO_QC", "Send to quality control", 2);
    }

    private ServiceWorkflowTransition create(
            UUID fromStage,
            UUID fromStatus,
            UUID toStage,
            UUID toStatus,
            String code,
            String displayName,
            int sequence
    ) {
        return ServiceWorkflowTransition.create(
                id, versionId, fromStage, fromStatus, toStage, toStatus,
                code, displayName, sequence, principalId, now);
    }

    private void assertMissing(
            UUID id,
            UUID versionId,
            UUID fromStage,
            UUID fromStatus,
            UUID toStage,
            UUID toStatus
    ) {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceWorkflowTransition.create(
                        id, versionId, fromStage, fromStatus, toStage, toStatus,
                        "CODE", "Display", 0, principalId, now));
    }
}