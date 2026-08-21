package com.autovision.platform.workflow;

import com.autovision.platform.aftersales.ProcessRequirementDefinition;
import com.autovision.platform.aftersales.ProcessRequirementKey;
import com.autovision.platform.aftersales.ProcessRequirementMode;
import com.autovision.platform.authorization.AuthorizationService;
import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceWorkflowTransitionRequirementReadServiceTests {

    @Test
    void returnsAllFiveModesInPersistedOrder() {
        UUID tenantId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID transitionId = UUID.randomUUID();
        AuthenticatedTenantContext context =
                new AuthenticatedTenantContext(principalId, tenantId, "user");
        ServiceWorkflowDefinitionRepository definitions = mock(ServiceWorkflowDefinitionRepository.class);
        ServiceWorkflowVersionRepository versions = mock(ServiceWorkflowVersionRepository.class);
        ServiceWorkflowTransitionRepository transitions = mock(ServiceWorkflowTransitionRepository.class);
        ServiceWorkflowTransitionRequirementRepository requirements = mock(ServiceWorkflowTransitionRequirementRepository.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        when(versions.findById(versionId)).thenReturn(Optional.of(version(versionId, definitionId, principalId)));
        when(definitions.findByIdAndTenantId(definitionId, tenantId)).thenReturn(Optional.of(
                ServiceWorkflowDefinition.create(definitionId, tenantId, null, null, "W", "Workflow", principalId, OffsetDateTime.now())
        ));
        when(transitions.findByIdAndWorkflowVersionId(transitionId, versionId))
                .thenReturn(Optional.of(mock(ServiceWorkflowTransition.class)));
        List<ServiceWorkflowTransitionRequirement> bindings = List.of(
                binding(versionId, transitionId, "REQUIRED", ProcessRequirementMode.REQUIRED),
                binding(versionId, transitionId, "OPTIONAL", ProcessRequirementMode.OPTIONAL),
                binding(versionId, transitionId, "CONDITIONAL", ProcessRequirementMode.CONDITIONAL),
                binding(versionId, transitionId, "AUTOMATIC", ProcessRequirementMode.AUTOMATIC),
                binding(versionId, transitionId, "NOT_APPLICABLE", ProcessRequirementMode.NOT_APPLICABLE)
        );
        when(requirements.findAllByWorkflowVersionIdAndWorkflowTransitionIdOrderByCreatedAtAscIdAsc(versionId, transitionId))
                .thenReturn(bindings);

        ServiceWorkflowTransitionRequirementReadService service =
                new ServiceWorkflowTransitionRequirementReadService(
                        definitions, versions, transitions, requirements, authorization
                );

        List<ProcessRequirementDefinition> result = service.findDefinitions(
                context, versionId, transitionId
        );

        assertEquals(List.of(
                new ProcessRequirementDefinition(new ProcessRequirementKey("REQUIRED"), ProcessRequirementMode.REQUIRED),
                new ProcessRequirementDefinition(new ProcessRequirementKey("OPTIONAL"), ProcessRequirementMode.OPTIONAL),
                new ProcessRequirementDefinition(new ProcessRequirementKey("CONDITIONAL"), ProcessRequirementMode.CONDITIONAL),
                new ProcessRequirementDefinition(new ProcessRequirementKey("AUTOMATIC"), ProcessRequirementMode.AUTOMATIC),
                new ProcessRequirementDefinition(new ProcessRequirementKey("NOT_APPLICABLE"), ProcessRequirementMode.NOT_APPLICABLE)
        ), result);
    }

    private static ServiceWorkflowVersion version(UUID id, UUID definitionId, UUID principalId) {
        return ServiceWorkflowVersion.draft(id, definitionId, 1, principalId, OffsetDateTime.now());
    }

    private static ServiceWorkflowTransitionRequirement binding(
            UUID versionId, UUID transitionId, String key, ProcessRequirementMode mode
    ) {
        return ServiceWorkflowTransitionRequirement.create(
                UUID.randomUUID(), versionId, transitionId,
                new ProcessRequirementKey(key), mode, OffsetDateTime.now(), UUID.randomUUID()
        );
    }
}