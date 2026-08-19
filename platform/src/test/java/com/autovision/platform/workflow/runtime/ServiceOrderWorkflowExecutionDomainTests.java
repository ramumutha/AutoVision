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
    void noTransitionMethodsAreExposedYet() {
        Set<String> methodNames = Set.of(
                ServiceOrderWorkflowExecution.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        org.junit.jupiter.api.Assertions.assertFalse(methodNames.contains("moveToStage"));
        org.junit.jupiter.api.Assertions.assertFalse(methodNames.contains("changeStatus"));
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