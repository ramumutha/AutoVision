package com.autovision.platform.workflow.runtime;

import com.autovision.platform.aftersales.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrderWorkflowExecutionDomainTests {

    private final UUID id = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID definitionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID stageId = UUID.randomUUID();
    private final UUID statusId = UUID.randomUUID();
    private final UUID principalId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void createsValidExecutionAndPinsRuntimeIdentities() {
        ServiceOrderWorkflowExecution execution = execution();

        assertEquals(id, execution.getId());
        assertEquals(tenantId, execution.getTenantId());
        assertEquals(serviceOrderId, execution.getServiceOrderId());
        assertEquals(definitionId, execution.getWorkflowDefinitionId());
        assertEquals(versionId, execution.getWorkflowVersionId());
        assertEquals(stageId, execution.getCurrentStageId());
        assertEquals(statusId, execution.getCurrentStatusId());
        assertEquals(principalId, execution.getCreatedByPrincipalId());
        assertEquals(principalId, execution.getUpdatedByPrincipalId());
        assertEquals(now, execution.getCreatedAt());
        assertEquals(now, execution.getUpdatedAt());
        assertEquals(0, execution.getVersion());
    }

    @Test
    void missingIdIsRejected() {
        assertMissing(null, tenantId, serviceOrderId, definitionId, versionId,
                stageId, statusId, principalId, now);
    }

    @Test
    void missingTenantIsRejected() {
        assertMissing(id, null, serviceOrderId, definitionId, versionId,
                stageId, statusId, principalId, now);
    }

    @Test
    void missingServiceOrderIsRejected() {
        assertMissing(id, tenantId, null, definitionId, versionId,
                stageId, statusId, principalId, now);
    }

    @Test
    void missingDefinitionIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, null, versionId,
                stageId, statusId, principalId, now);
    }

    @Test
    void missingVersionIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, definitionId, null,
                stageId, statusId, principalId, now);
    }

    @Test
    void missingPrincipalIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, definitionId, versionId,
                stageId, statusId, null, now);
    }

    @Test
    void missingTimestampIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, definitionId, versionId,
                stageId, statusId, principalId, null);
    }

    @Test
    void missingInitialStageIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, definitionId, versionId,
                null, statusId, principalId, now);
    }

    @Test
    void missingInitialStatusIsRejected() {
        assertMissing(id, tenantId, serviceOrderId, definitionId, versionId,
                stageId, null, principalId, now);
    }

    @Test
    void configurableStageAndStatusRemainIndependentFromServiceOrderStatus() {
        ServiceOrderWorkflowExecution execution = execution();

        assertEquals(stageId, execution.getCurrentStageId());
        assertEquals(statusId, execution.getCurrentStatusId());
        assertEquals(Set.of("OPEN", "IN_PROGRESS", "WORK_COMPLETED", "CLOSED", "CANCELLED"),
                Set.of(ServiceOrderStatus.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()));
    }

    @Test
    void movesWithinSameStageToDifferentStatus() {
        ServiceOrderWorkflowExecution execution = execution();
        UUID newStatusId = UUID.randomUUID();
        OffsetDateTime moveTime = now.plusMinutes(1);

        execution.moveTo(stageId, newStatusId, principalId, moveTime);

        assertEquals(stageId, execution.getCurrentStageId());
        assertEquals(newStatusId, execution.getCurrentStatusId());
    }

    @Test
    void movesAcrossStages() {
        ServiceOrderWorkflowExecution execution = execution();
        UUID newStageId = UUID.randomUUID();
        UUID newStatusId = UUID.randomUUID();
        OffsetDateTime moveTime = now.plusMinutes(1);

        execution.moveTo(newStageId, newStatusId, principalId, moveTime);

        assertEquals(newStageId, execution.getCurrentStageId());
        assertEquals(newStatusId, execution.getCurrentStatusId());
    }

    @Test
    void moveUpdatesUpdatedByPrincipalId() {
        ServiceOrderWorkflowExecution execution = execution();
        UUID movingPrincipalId = UUID.randomUUID();

        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(),
                movingPrincipalId, now.plusMinutes(1));

        assertEquals(movingPrincipalId, execution.getUpdatedByPrincipalId());
    }

    @Test
    void moveUpdatesUpdatedAt() {
        ServiceOrderWorkflowExecution execution = execution();
        OffsetDateTime moveTime = now.plusMinutes(1);

        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, moveTime);

        assertEquals(moveTime, execution.getUpdatedAt());
    }

    @Test
    void movePreservesTenantId() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));
        assertEquals(tenantId, execution.getTenantId());
    }

    @Test
    void movePreservesServiceOrderId() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));
        assertEquals(serviceOrderId, execution.getServiceOrderId());
    }

    @Test
    void movePreservesWorkflowDefinitionId() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));
        assertEquals(definitionId, execution.getWorkflowDefinitionId());
    }

    @Test
    void movePreservesWorkflowVersionId() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));
        assertEquals(versionId, execution.getWorkflowVersionId());
    }

    @Test
    void movePreservesCreatedByPrincipalId() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), now.plusMinutes(1));
        assertEquals(principalId, execution.getCreatedByPrincipalId());
    }

    @Test
    void movePreservesCreatedAt() {
        ServiceOrderWorkflowExecution execution = execution();
        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));
        assertEquals(now, execution.getCreatedAt());
    }

    @Test
    void moveRejectsNullTargetStageId() {
        ServiceOrderWorkflowExecution execution = execution();
        assertThrows(IllegalArgumentException.class, () ->
                execution.moveTo(null, UUID.randomUUID(), principalId, now.plusMinutes(1)));
    }

    @Test
    void moveRejectsNullTargetStatusId() {
        ServiceOrderWorkflowExecution execution = execution();
        assertThrows(IllegalArgumentException.class, () ->
                execution.moveTo(UUID.randomUUID(), null, principalId, now.plusMinutes(1)));
    }

    @Test
    void moveRejectsNullPrincipalId() {
        ServiceOrderWorkflowExecution execution = execution();
        assertThrows(IllegalArgumentException.class, () ->
                execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), null, now.plusMinutes(1)));
    }

    @Test
    void moveRejectsNullTimestamp() {
        ServiceOrderWorkflowExecution execution = execution();
        assertThrows(IllegalArgumentException.class, () ->
                execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, null));
    }

    @Test
    void moveRejectsCompleteNoOp() {
        ServiceOrderWorkflowExecution execution = execution();
        assertThrows(IllegalStateException.class, () ->
                execution.moveTo(stageId, statusId, principalId, now.plusMinutes(1)));
    }

    @Test
    void moveAllowsSameStageWithDifferentStatus() {
        ServiceOrderWorkflowExecution execution = execution();
        UUID newStatusId = UUID.randomUUID();

        execution.moveTo(stageId, newStatusId, principalId, now.plusMinutes(1));

        assertEquals(stageId, execution.getCurrentStageId());
        assertEquals(newStatusId, execution.getCurrentStatusId());
    }

    @Test
    void moveDoesNotManuallyChangeOptimisticLockVersion() {
        ServiceOrderWorkflowExecution execution = execution();
        long versionBeforeMove = execution.getVersion();

        execution.moveTo(UUID.randomUUID(), UUID.randomUUID(), principalId, now.plusMinutes(1));

        assertEquals(versionBeforeMove, execution.getVersion());
    }

    @Test
    void noServiceOrderStatusDependencyExistsOnMoveTo() throws NoSuchMethodException {
        Method moveTo = ServiceOrderWorkflowExecution.class.getMethod(
                "moveTo", UUID.class, UUID.class, UUID.class, OffsetDateTime.class);

        for (Class<?> parameterType : moveTo.getParameterTypes()) {
            org.junit.jupiter.api.Assertions.assertNotEquals(
                    ServiceOrderStatus.class, parameterType);
        }
    }

    @Test
    void noTransitionRepositoryOrConfigurationDependencyExists() {
        Set<String> fieldTypeNames = Set.of(
                        ServiceOrderWorkflowExecution.class.getDeclaredFields())
                .stream()
                .map(field -> field.getType().getName())
                .collect(Collectors.toSet());

        boolean hasConfigurationDependency = fieldTypeNames.stream().anyMatch(name ->
                name.contains("Repository") || name.contains("Transition"));

        org.junit.jupiter.api.Assertions.assertFalse(hasConfigurationDependency);
    }

    @Test
    void moveToDoesNotRequireTransitionIdParameter() throws NoSuchMethodException {
        Method moveTo = ServiceOrderWorkflowExecution.class.getMethod(
                "moveTo", UUID.class, UUID.class, UUID.class, OffsetDateTime.class);

        assertEquals(4, moveTo.getParameterCount());
    }

    private ServiceOrderWorkflowExecution execution() {
        return ServiceOrderWorkflowExecution.start(
                id, tenantId, serviceOrderId, definitionId, versionId,
                stageId, statusId, principalId, now);
    }

    private void assertMissing(
            UUID id,
            UUID tenantId,
            UUID serviceOrderId,
            UUID definitionId,
            UUID versionId,
            UUID stageId,
            UUID statusId,
            UUID principalId,
            OffsetDateTime now
    ) {
        assertThrows(IllegalArgumentException.class, () ->
                ServiceOrderWorkflowExecution.start(
                        id, tenantId, serviceOrderId, definitionId, versionId,
                        stageId, statusId, principalId, now));
    }
}