package com.autovision.platform.aftersales;

import com.autovision.platform.tenant.AuthenticatedTenantContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessRequirementSatisfactionProviderTests {

    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID serviceJobId = UUID.randomUUID();
    private final AuthenticatedTenantContext tenantContext =
            new AuthenticatedTenantContext(userId, tenantId, "user@example.com");
    private final ProcessRequirementEvaluationContext context =
            new ProcessRequirementEvaluationContext(
                    tenantContext,
                    serviceOrderId,
                    serviceJobId
            );

    @Test
    void contextPreservesTenantAndServiceIdentifiers() {
        assertEquals(tenantContext, context.tenantContext());
        assertEquals(serviceOrderId, context.serviceOrderId());
        assertEquals(serviceJobId, context.serviceJobId());
    }

    @Test
    void contextRejectsNullValues() {
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementEvaluationContext(
                        null,
                        serviceOrderId,
                        serviceJobId
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementEvaluationContext(
                        tenantContext,
                        null,
                        serviceJobId
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementEvaluationContext(
                        tenantContext,
                        serviceOrderId,
                        null
                )
        );
    }

    @Test
    void registryResolvesExactProviderKey() {
        ProcessRequirementSatisfactionProvider provider = provider(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION
        );
        ProcessRequirementSatisfactionProviderRegistry registry =
                new ProcessRequirementSatisfactionProviderRegistry(
                        new java.util.ArrayList<>(java.util.List.of(provider))
                );

        assertEquals(provider, registry.require(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION
        ));
    }

    @Test
    void registryRejectsMissingAndNullKeys() {
        ProcessRequirementSatisfactionProviderRegistry registry =
                new ProcessRequirementSatisfactionProviderRegistry(java.util.List.of());

        assertThrows(
                IllegalStateException.class,
                () -> registry.require(ServiceProcessRequirementKeys.QUOTE)
        );
        assertThrows(NullPointerException.class, () -> registry.require(null));
    }

    @Test
    void registryRejectsDuplicateKeysAndSupportsDistinctProviders() {
        ProcessRequirementSatisfactionProvider first = provider(
                ServiceProcessRequirementKeys.QUOTE
        );
        ProcessRequirementSatisfactionProvider duplicate = provider(
                ServiceProcessRequirementKeys.QUOTE
        );
        ProcessRequirementSatisfactionProvider second = provider(
                ServiceProcessRequirementKeys.INTERNAL_APPROVAL
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessRequirementSatisfactionProviderRegistry(
                        java.util.List.of(first, duplicate)
                )
        );

        ProcessRequirementSatisfactionProviderRegistry registry =
                new ProcessRequirementSatisfactionProviderRegistry(
                        java.util.List.of(first, second)
                );
        assertEquals(first, registry.require(ServiceProcessRequirementKeys.QUOTE));
        assertEquals(
                second,
                registry.require(ServiceProcessRequirementKeys.INTERNAL_APPROVAL)
        );
    }

    @Test
    void registryRejectsNullProvidersAndProviderKeys() {
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementSatisfactionProviderRegistry(
                        java.util.List.of((ProcessRequirementSatisfactionProvider) null)
                )
        );
        ProcessRequirementSatisfactionProvider provider = mock(
                ProcessRequirementSatisfactionProvider.class
        );
        when(provider.key()).thenReturn(null);
        assertThrows(
                NullPointerException.class,
                () -> new ProcessRequirementSatisfactionProviderRegistry(
                        java.util.List.of(provider)
                )
        );
    }

    @Test
    void registryIsUnaffectedByRegistrationListMutation() {
        ProcessRequirementSatisfactionProvider provider = provider(
                ServiceProcessRequirementKeys.QUOTE
        );
        java.util.List<ProcessRequirementSatisfactionProvider> providers =
                new java.util.ArrayList<>(java.util.List.of(provider));
        ProcessRequirementSatisfactionProviderRegistry registry =
                new ProcessRequirementSatisfactionProviderRegistry(providers);

        providers.clear();

        assertEquals(provider, registry.require(ServiceProcessRequirementKeys.QUOTE));
    }

    @Test
    void customerAuthorizationProviderOwnsExactKey() {
        CustomerAuthorizationRequirementSatisfactionProvider provider = provider();

        assertEquals(
                ServiceProcessRequirementKeys.CUSTOMER_AUTHORIZATION,
                provider.key()
        );
    }

    @Test
    void customerAuthorizationProviderPassesExactContextAndEvaluation() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        OperationalAuthorizationEvaluation evaluation = evaluation(
                OperationalAuthorizationStatus.FULLY_AUTHORIZED
        );
        ServiceJobAuthorizationReadiness readiness = readiness(evaluation, true);
        when(evaluationService.evaluate(tenantContext, serviceOrderId, serviceJobId))
                .thenReturn(evaluation);
        when(readinessPolicy.evaluate(evaluation)).thenReturn(readiness);
        CustomerAuthorizationRequirementSatisfactionProvider provider =
                new CustomerAuthorizationRequirementSatisfactionProvider(
                        evaluationService,
                        readinessPolicy
                );

        assertTrue(provider.isSatisfied(context));

        verify(evaluationService).evaluate(
                tenantContext,
                serviceOrderId,
                serviceJobId
        );
        verify(readinessPolicy).evaluate(evaluation);
    }

    @Test
    void readinessFalseMeansUnsatisfied() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        OperationalAuthorizationEvaluation evaluation = evaluation(
                OperationalAuthorizationStatus.NOT_AUTHORIZED
        );
        when(evaluationService.evaluate(tenantContext, serviceOrderId, serviceJobId))
                .thenReturn(evaluation);
        when(readinessPolicy.evaluate(evaluation)).thenReturn(readiness(evaluation, false));

        assertFalse(new CustomerAuthorizationRequirementSatisfactionProvider(
                evaluationService,
                readinessPolicy
        ).isSatisfied(context));
    }

    @Test
    void notRequiredReadinessRemainsSatisfied() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        OperationalAuthorizationEvaluation evaluation = evaluation(
                OperationalAuthorizationStatus.NOT_REQUIRED
        );
        when(evaluationService.evaluate(tenantContext, serviceOrderId, serviceJobId))
                .thenReturn(evaluation);
        when(readinessPolicy.evaluate(evaluation)).thenReturn(readiness(evaluation, true));

        assertTrue(new CustomerAuthorizationRequirementSatisfactionProvider(
                evaluationService,
                readinessPolicy
        ).isSatisfied(context));
    }

    @Test
    void propagatesEvaluatorException() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        RuntimeException evaluationFailure = new RuntimeException("evaluation failure");
        doThrow(evaluationFailure)
                .when(evaluationService)
                .evaluate(tenantContext, serviceOrderId, serviceJobId);
        CustomerAuthorizationRequirementSatisfactionProvider provider =
                new CustomerAuthorizationRequirementSatisfactionProvider(
                        evaluationService,
                        readinessPolicy
                );

        assertEquals(evaluationFailure, assertThrows(
                RuntimeException.class,
                () -> provider.isSatisfied(context)
        ));
        verifyNoInteractions(readinessPolicy);
    }

    @Test
    void propagatesReadinessPolicyExceptionAndPassesExactEvaluation() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        OperationalAuthorizationEvaluation evaluation = evaluation(
                OperationalAuthorizationStatus.PENDING
        );
        RuntimeException readinessFailure = new RuntimeException("readiness failure");
        when(evaluationService.evaluate(tenantContext, serviceOrderId, serviceJobId))
                .thenReturn(evaluation);
        doThrow(readinessFailure)
                .when(readinessPolicy)
                .evaluate(evaluation);
        CustomerAuthorizationRequirementSatisfactionProvider provider =
                new CustomerAuthorizationRequirementSatisfactionProvider(
                        evaluationService,
                        readinessPolicy
                );

        assertEquals(readinessFailure, assertThrows(
                RuntimeException.class,
                () -> provider.isSatisfied(context)
        ));
        verify(evaluationService).evaluate(
                tenantContext,
                serviceOrderId,
                serviceJobId
        );
        verify(readinessPolicy).evaluate(evaluation);
    }

    @Test
    void providerRejectsNullContextWithoutCallingDependencies() {
        OperationalAuthorizationEvaluationService evaluationService =
                mock(OperationalAuthorizationEvaluationService.class);
        ServiceJobAuthorizationReadinessPolicy readinessPolicy =
                mock(ServiceJobAuthorizationReadinessPolicy.class);
        CustomerAuthorizationRequirementSatisfactionProvider provider =
                new CustomerAuthorizationRequirementSatisfactionProvider(
                        evaluationService,
                        readinessPolicy
                );

        assertThrows(NullPointerException.class, () -> provider.isSatisfied(null));
        verifyNoInteractions(evaluationService, readinessPolicy);
    }

    private ProcessRequirementSatisfactionProvider provider(ProcessRequirementKey key) {
        ProcessRequirementSatisfactionProvider provider = mock(
                ProcessRequirementSatisfactionProvider.class
        );
        when(provider.key()).thenReturn(key);
        return provider;
    }

    private CustomerAuthorizationRequirementSatisfactionProvider provider() {
        return new CustomerAuthorizationRequirementSatisfactionProvider(
                mock(OperationalAuthorizationEvaluationService.class),
                mock(ServiceJobAuthorizationReadinessPolicy.class)
        );
    }

    private OperationalAuthorizationEvaluation evaluation(
            OperationalAuthorizationStatus status
    ) {
        return new OperationalAuthorizationEvaluation(
                serviceJobId,
                serviceOrderId,
                status,
                0,
                0,
                0,
                0
        );
    }

    private ServiceJobAuthorizationReadiness readiness(
            OperationalAuthorizationEvaluation evaluation,
            boolean ready
    ) {
        return new ServiceJobAuthorizationReadiness(
                evaluation.serviceJobId(),
                evaluation.serviceOrderId(),
                ready,
                evaluation.status(),
                ServiceJobAuthorizationReadinessReason.FULLY_AUTHORIZED
        );
    }
}