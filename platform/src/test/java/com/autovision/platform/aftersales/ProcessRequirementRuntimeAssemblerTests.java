package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessRequirementRuntimeAssemblerTests {

    private final ProcessRequirementSatisfactionProviderRegistry providerRegistry =
            mock(ProcessRequirementSatisfactionProviderRegistry.class);
    private final RuntimeProcessRequirementStateResolver stateResolver =
            new RuntimeProcessRequirementStateResolver();
    private final RuntimeTransitionDecisionComposer decisionComposer =
            new RuntimeTransitionDecisionComposer();
    private final ProcessRequirementEvaluationContext context =
            new ProcessRequirementEvaluationContext(
                    new AuthenticatedTenantContext(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "user@example.com"
                    ),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );
    private final ProcessRequirementRuntimeAssembler assembler =
            new ProcessRequirementRuntimeAssembler(
                    providerRegistry,
                    stateResolver,
                    decisionComposer
            );

    @Test
    void emptyRequirementsProduceEmptyStatesAndCanProceedDecision() {
        ProcessRequirementRuntimeAssembly assembly = assembler.assemble(
                List.of(),
                context
        );

        assertTrue(assembly.states().isEmpty());
        assertTrue(assembly.decision().canProceed());
        verifyNoInteractions(providerRegistry);
    }

    @Test
    void applicableRequiredSatisfiedCallsProviderAndCanProceed() {
        ProcessRequirementKey key = key("REQUIRED");
        ProcessRequirementSatisfactionProvider provider = provider(key, true);
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.REQUIRED,
                true,
                true,
                true
        );
        when(providerRegistry.require(key)).thenReturn(provider);

        ProcessRequirementRuntimeAssembly assembly = assemble(requirement);

        verify(provider).isSatisfied(same(context));
        assertTrue(assembly.states().get(0).satisfied());
        assertTrue(assembly.decision().canProceed());
    }

    @Test
    void applicableRequiredUnsatisfiedCreatesUnresolvedBlocker() {
        ProcessRequirementKey key = key("REQUIRED");
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.REQUIRED,
                true,
                true,
                true
        );
        ProcessRequirementSatisfactionProvider provider = provider(key, false);
        when(providerRegistry.require(key)).thenReturn(provider);

        ProcessRequirementRuntimeAssembly assembly = assemble(requirement);

        assertFalse(assembly.decision().canProceed());
        assertEquals(List.of(key), assembly.decision().unresolvedBlockingRequirements());
    }

    @Test
    void applicableOptionalUnsatisfiedCallsProviderAndIsActionableOnly() {
        ProcessRequirementKey key = key("OPTIONAL");
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.OPTIONAL,
                true,
                false,
                true
        );
        ProcessRequirementSatisfactionProvider provider = provider(key, false);
        when(providerRegistry.require(key)).thenReturn(provider);

        ProcessRequirementRuntimeAssembly assembly = assemble(requirement);

        verify(provider).isSatisfied(same(context));
        assertTrue(assembly.decision().canProceed());
        assertTrue(assembly.decision().unresolvedBlockingRequirements().isEmpty());
        assertEquals(List.of(key), assembly.decision().unresolvedActionableRequirements());
    }

    @Test
    void nonApplicableRequirementSkipsProviderAndIsSatisfied() {
        ProcessRequirementKey key = key("NOT_APPLICABLE");
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.NOT_APPLICABLE,
                false,
                false,
                false
        );

        ProcessRequirementRuntimeAssembly assembly = assemble(requirement);

        verifyNoInteractions(providerRegistry);
        assertTrue(assembly.states().get(0).satisfied());
        assertTrue(assembly.decision().canProceed());
    }

    @Test
    void applicableConditionalSatisfiedAndUnsatisfiedAreResolvedByProvider() {
        ProcessRequirementKey satisfiedKey = key("CONDITIONAL_SATISFIED");
        ProcessRequirementKey unsatisfiedKey = key("CONDITIONAL_UNSATISFIED");
        EffectiveProcessRequirement satisfiedRequirement = requirement(
                satisfiedKey,
                ProcessRequirementMode.CONDITIONAL,
                true,
                true,
                true
        );
        EffectiveProcessRequirement unsatisfiedRequirement = requirement(
                unsatisfiedKey,
                ProcessRequirementMode.CONDITIONAL,
                true,
                true,
                true
        );
        ProcessRequirementSatisfactionProvider satisfiedProvider = provider(
                satisfiedKey,
                true
        );
        ProcessRequirementSatisfactionProvider unsatisfiedProvider = provider(
                unsatisfiedKey,
                false
        );
        when(providerRegistry.require(satisfiedKey)).thenReturn(satisfiedProvider);
        when(providerRegistry.require(unsatisfiedKey)).thenReturn(unsatisfiedProvider);

        ProcessRequirementRuntimeAssembly assembly = assembler.assemble(
                List.of(satisfiedRequirement, unsatisfiedRequirement),
                context
        );

        assertTrue(assembly.states().get(0).satisfied());
        assertFalse(assembly.states().get(1).satisfied());
        assertFalse(assembly.decision().canProceed());
    }

    @Test
    void applicableAutomaticRequirementInvokesProviderWithoutSpecialTreatment() {
        ProcessRequirementKey key = key("AUTOMATIC");
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.AUTOMATIC,
                true,
                false,
                false
        );
        ProcessRequirementSatisfactionProvider provider = provider(key, false);
        when(providerRegistry.require(key)).thenReturn(provider);

        ProcessRequirementRuntimeAssembly assembly = assemble(requirement);

        verify(provider).isSatisfied(same(context));
        assertFalse(assembly.states().get(0).satisfied());
        assertTrue(assembly.decision().canProceed());
        assertTrue(assembly.decision().unresolvedActionableRequirements().isEmpty());
    }

    @Test
    void preservesStateOrderAndPassesSameContextToProviders() {
        ProcessRequirementKey firstKey = key("FIRST");
        ProcessRequirementKey secondKey = key("SECOND");
        ProcessRequirementSatisfactionProvider first = provider(firstKey, true);
        ProcessRequirementSatisfactionProvider second = provider(secondKey, true);
        when(providerRegistry.require(firstKey)).thenReturn(first);
        when(providerRegistry.require(secondKey)).thenReturn(second);

        ProcessRequirementRuntimeAssembly assembly = assembler.assemble(
                List.of(
                        requirement(firstKey, ProcessRequirementMode.REQUIRED, true, true, true),
                        requirement(secondKey, ProcessRequirementMode.OPTIONAL, true, false, true)
                ),
                context
        );

        assertEquals(firstKey, assembly.states().get(0).key());
        assertEquals(secondKey, assembly.states().get(1).key());
        verify(first).isSatisfied(same(context));
        verify(second).isSatisfied(same(context));
    }

    @Test
    void selectsExactProviderByRequirementKey() {
        ProcessRequirementKey key = key("EXACT");
        ProcessRequirementSatisfactionProvider provider = provider(key, true);
        when(providerRegistry.require(key)).thenReturn(provider);

        assembler.assemble(
                List.of(requirement(key, ProcessRequirementMode.REQUIRED, true, true, true)),
                context
        );

        verify(providerRegistry).require(same(key));
    }

    @Test
    void missingApplicableProviderFailsExplicitly() {
        ProcessRequirementKey key = key("MISSING");
        EffectiveProcessRequirement requirement = requirement(
                key,
                ProcessRequirementMode.REQUIRED,
                true,
                true,
                true
        );
        IllegalStateException failure = new IllegalStateException("missing provider");
        when(providerRegistry.require(key)).thenThrow(failure);

        assertSame(failure, assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(List.of(requirement), context)
        ));
    }

    @Test
    void missingNonApplicableProviderDoesNotFail() {
        EffectiveProcessRequirement requirement = requirement(
                key("MISSING_NON_APPLICABLE"),
                ProcessRequirementMode.NOT_APPLICABLE,
                false,
                false,
                false
        );

        assembler.assemble(List.of(requirement), context);

        verifyNoInteractions(providerRegistry);
    }

    @Test
    void providerExceptionPropagatesUnchanged() {
        ProcessRequirementKey key = key("EXCEPTION");
        ProcessRequirementSatisfactionProvider provider = mock(
                ProcessRequirementSatisfactionProvider.class
        );
        RuntimeException failure = new RuntimeException("provider failure");
        when(providerRegistry.require(key)).thenReturn(provider);
        when(provider.isSatisfied(context)).thenThrow(failure);

        assertSame(failure, assertThrows(
                RuntimeException.class,
                () -> assembler.assemble(
                        List.of(requirement(key, ProcessRequirementMode.REQUIRED, true, true, true)),
                        context
                )
        ));
    }

    @Test
    void passesExactStateListToComposerAndPreservesExactDecision() {
        RuntimeTransitionDecisionComposer composer =
                mock(RuntimeTransitionDecisionComposer.class);
        RuntimeTransitionDecision decision = new RuntimeTransitionDecision(
                true,
                List.of(),
                List.of()
        );
        when(composer.compose(any())).thenReturn(decision);
        ProcessRequirementRuntimeAssembler testAssembler =
                new ProcessRequirementRuntimeAssembler(
                        providerRegistry,
                        stateResolver,
                        composer
                );
        EffectiveProcessRequirement requirement = requirement(
                key("EXACT_STATE"),
                ProcessRequirementMode.NOT_APPLICABLE,
                false,
                false,
                false
        );

        ProcessRequirementRuntimeAssembly assembly = testAssembler.assemble(
                List.of(requirement),
                context
        );

        var statesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(composer).compose(statesCaptor.capture());
        assertSame(statesCaptor.getValue().get(0), assembly.states().get(0));
        assertSame(decision, assembly.decision());
    }

    @Test
    void inputRequirementsRemainUnchanged() {
        EffectiveProcessRequirement requirement = requirement(
                key("UNCHANGED"),
                ProcessRequirementMode.NOT_APPLICABLE,
                false,
                false,
                false
        );
        List<EffectiveProcessRequirement> requirements =
                new ArrayList<>(List.of(requirement));

        assembler.assemble(requirements, context);

        assertEquals(List.of(requirement), requirements);
        assertFalse(requirement.applicable());
        assertFalse(requirement.blocking());
        assertFalse(requirement.actionable());
    }

    @Test
    void assemblyStateListIsImmutableAndDefensive() {
        EffectiveProcessRequirement requirement = requirement(
                key("IMMUTABLE"),
                ProcessRequirementMode.NOT_APPLICABLE,
                false,
                false,
                false
        );

        ProcessRequirementRuntimeAssembly assembly = assembler.assemble(
                new ArrayList<>(List.of(requirement)),
                context
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> assembly.states().clear()
        );
    }

    @Test
    void nullRequirementsContextAndEntriesAreRejected() {
        assertThrows(
                NullPointerException.class,
                () -> assembler.assemble(null, context)
        );
        assertThrows(
                NullPointerException.class,
                () -> assembler.assemble(List.of(), null)
        );
        assertThrows(
                NullPointerException.class,
                () -> assembler.assemble(
                        List.of((EffectiveProcessRequirement) null),
                        context
                )
        );
    }

    private ProcessRequirementRuntimeAssembly assemble(
            EffectiveProcessRequirement requirement
    ) {
        return assembler.assemble(List.of(requirement), context);
    }

    private ProcessRequirementRuntimeAssembly assemble(
            EffectiveProcessRequirement requirement,
            ProcessRequirementEvaluationContext evaluationContext
    ) {
        return assembler.assemble(List.of(requirement), evaluationContext);
    }

    private ProcessRequirementSatisfactionProvider provider(
            ProcessRequirementKey key,
            boolean satisfied
    ) {
        ProcessRequirementSatisfactionProvider provider = mock(
                ProcessRequirementSatisfactionProvider.class
        );
        when(provider.isSatisfied(same(context))).thenReturn(satisfied);
        when(provider.key()).thenReturn(key);
        return provider;
    }

    private EffectiveProcessRequirement requirement(
            ProcessRequirementKey key,
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable
    ) {
        return new EffectiveProcessRequirement(
                key,
                mode,
                applicable,
                blocking,
                actionable
        );
    }

    private List<EffectiveProcessRequirement> requirement(
            ProcessRequirementKey key,
            ProcessRequirementMode mode,
            boolean applicable,
            boolean blocking,
            boolean actionable,
            ProcessRequirementEvaluationContext evaluationContext
    ) {
        return List.of(requirement(key, mode, applicable, blocking, actionable));
    }

    private ProcessRequirementKey key(String value) {
        return new ProcessRequirementKey("TEST." + value);
    }
}