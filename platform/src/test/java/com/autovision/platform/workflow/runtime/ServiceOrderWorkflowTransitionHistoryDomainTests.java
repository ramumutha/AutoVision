package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrderWorkflowTransitionHistoryDomainTests {

    private final UUID id = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID workflowExecutionId = UUID.randomUUID();
    private final UUID workflowDefinitionId = UUID.randomUUID();
    private final UUID workflowVersionId = UUID.randomUUID();
    private final UUID transitionId = UUID.randomUUID();
    private final UUID fromStageId = UUID.randomUUID();
    private final UUID fromStatusId = UUID.randomUUID();
    private final UUID toStageId = UUID.randomUUID();
    private final UUID toStatusId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final OffsetDateTime executedAt = OffsetDateTime.now();

    @Test
    void recordsValidSameStageDifferentStatusHistory() {
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowTransitionHistory history = record(
                fromStageId, fromStatusId, fromStageId, newStatusId);

        assertEquals(fromStageId, history.getFromStageId());
        assertEquals(fromStageId, history.getToStageId());
        assertEquals(fromStatusId, history.getFromStatusId());
        assertEquals(newStatusId, history.getToStatusId());
    }

    @Test
    void recordsValidCrossStageHistory() {
        ServiceOrderWorkflowTransitionHistory history = record(
                fromStageId, fromStatusId, toStageId, toStatusId);

        assertEquals(fromStageId, history.getFromStageId());
        assertEquals(toStageId, history.getToStageId());
        assertEquals(fromStatusId, history.getFromStatusId());
        assertEquals(toStatusId, history.getToStatusId());
    }

    @Test
    void preservesTenantId() {
        assertEquals(tenantId, record(fromStageId, fromStatusId, toStageId, toStatusId)
                .getTenantId());
    }

    @Test
    void preservesServiceOrderId() {
        assertEquals(serviceOrderId, record(fromStageId, fromStatusId, toStageId, toStatusId)
                .getServiceOrderId());
    }

    @Test
    void preservesWorkflowExecutionId() {
        assertEquals(workflowExecutionId, record(
                fromStageId, fromStatusId, toStageId, toStatusId).getWorkflowExecutionId());
    }

    @Test
    void preservesWorkflowDefinitionId() {
        assertEquals(workflowDefinitionId, record(
                fromStageId, fromStatusId, toStageId, toStatusId).getWorkflowDefinitionId());
    }

    @Test
    void preservesWorkflowVersionId() {
        assertEquals(workflowVersionId, record(
                fromStageId, fromStatusId, toStageId, toStatusId).getWorkflowVersionId());
    }

    @Test
    void preservesTransitionId() {
        assertEquals(transitionId, record(fromStageId, fromStatusId, toStageId, toStatusId)
                .getTransitionId());
    }

    @Test
    void preservesFromStageAndStatus() {
        ServiceOrderWorkflowTransitionHistory history = record(
                fromStageId, fromStatusId, toStageId, toStatusId);
        assertEquals(fromStageId, history.getFromStageId());
        assertEquals(fromStatusId, history.getFromStatusId());
    }

    @Test
    void preservesToStageAndStatus() {
        ServiceOrderWorkflowTransitionHistory history = record(
                fromStageId, fromStatusId, toStageId, toStatusId);
        assertEquals(toStageId, history.getToStageId());
        assertEquals(toStatusId, history.getToStatusId());
    }

    @Test
    void preservesExecutedPrincipal() {
        assertEquals(principalId, record(fromStageId, fromStatusId, toStageId, toStatusId)
                .getExecutedByPrincipalId());
    }

    @Test
    void preservesExecutedAt() {
        assertEquals(executedAt, record(fromStageId, fromStatusId, toStageId, toStatusId)
                .getExecutedAt());
    }

    @Test
    void rejectsNullId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        null, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, null, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullServiceOrderId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, null, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullWorkflowExecutionId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, null,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullWorkflowDefinitionId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        null, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullWorkflowVersionId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, null, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullTransitionId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, null,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullFromStageId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        null, fromStatusId, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullFromStatusId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, null, toStageId, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullToStageId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, null, toStatusId,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullToStatusId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, null,
                        principalId, executedAt));
    }

    @Test
    void rejectsNullPrincipalId() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        null, executedAt));
    }

    @Test
    void rejectsNullExecutedAt() {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowTransitionHistory.record(
                        id, tenantId, serviceOrderId, workflowExecutionId,
                        workflowDefinitionId, workflowVersionId, transitionId,
                        fromStageId, fromStatusId, toStageId, toStatusId,
                        principalId, null));
    }

    @Test
    void rejectsCompleteNoOp() {
        assertThrows(IllegalArgumentException.class, () ->
                record(fromStageId, fromStatusId, fromStageId, fromStatusId));
    }

    @Test
    void allowsSameStageDifferentStatus() {
        UUID newStatusId = UUID.randomUUID();
        ServiceOrderWorkflowTransitionHistory history = record(
                fromStageId, fromStatusId, fromStageId, newStatusId);
        assertEquals(fromStageId, history.getToStageId());
        assertEquals(newStatusId, history.getToStatusId());
    }

    @Test
    void hasNoPublicMutationApi() {
        Set<String> forbidden = Set.of(
                "update", "move", "change", "set", "cancel", "reverse", "correct");
        Set<String> methodNames = Arrays.stream(
                        ServiceOrderWorkflowTransitionHistory.class.getMethods())
                .map(Method::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        for (String name : methodNames) {
            for (String badPrefix : forbidden) {
                assertFalse(name.startsWith(badPrefix),
                        "Unexpected mutating method: " + name);
            }
        }
    }

    @Test
    void hasNoJpaRelationshipFields() {
        for (Field field : ServiceOrderWorkflowTransitionHistory.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(jakarta.persistence.ManyToOne.class));
            assertFalse(field.isAnnotationPresent(jakarta.persistence.OneToMany.class));
            assertFalse(field.isAnnotationPresent(jakarta.persistence.OneToOne.class));
            assertFalse(field.isAnnotationPresent(jakarta.persistence.ManyToMany.class));
        }
    }

    @Test
    void hasNoServiceOrderStatusDependency() {
        for (Field field : ServiceOrderWorkflowTransitionHistory.class.getDeclaredFields()) {
            assertFalse(field.getType().equals(ServiceOrderStatus.class));
        }
    }

    @Test
    void hasNoCurrentStageOrStatusFields() {
        Set<String> fieldNames = Arrays.stream(
                        ServiceOrderWorkflowTransitionHistory.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertFalse(fieldNames.contains("currentStageId"));
        assertFalse(fieldNames.contains("currentStatusId"));
    }

    private ServiceOrderWorkflowTransitionHistory record(
            UUID fromStageIdValue, UUID fromStatusIdValue,
            UUID toStageIdValue, UUID toStatusIdValue
    ) {
        return ServiceOrderWorkflowTransitionHistory.record(
                id, tenantId, serviceOrderId, workflowExecutionId,
                workflowDefinitionId, workflowVersionId, transitionId,
                fromStageIdValue, fromStatusIdValue, toStageIdValue, toStatusIdValue,
                principalId, executedAt);
    }
}
