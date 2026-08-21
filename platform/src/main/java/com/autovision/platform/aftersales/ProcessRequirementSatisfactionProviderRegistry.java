package com.autovision.platform.aftersales;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProcessRequirementSatisfactionProviderRegistry {

    private final Map<ProcessRequirementKey, ProcessRequirementSatisfactionProvider>
            providersByKey;

    public ProcessRequirementSatisfactionProviderRegistry(
            List<ProcessRequirementSatisfactionProvider> providers
    ) {
        Objects.requireNonNull(providers, "Providers are required");

        Map<ProcessRequirementKey, ProcessRequirementSatisfactionProvider> registered =
                new HashMap<>();
        for (ProcessRequirementSatisfactionProvider provider : providers) {
            Objects.requireNonNull(provider, "Provider is required");
            ProcessRequirementKey key = Objects.requireNonNull(
                    provider.key(),
                    "Provider key is required"
            );
            if (registered.putIfAbsent(key, provider) != null) {
                throw new IllegalArgumentException(
                        "Duplicate process requirement provider: " + key.value()
                );
            }
        }
        providersByKey = Map.copyOf(registered);
    }

    public ProcessRequirementSatisfactionProvider require(
            ProcessRequirementKey key
    ) {
        Objects.requireNonNull(key, "Process requirement key is required");
        ProcessRequirementSatisfactionProvider provider = providersByKey.get(key);
        if (provider == null) {
            throw new IllegalStateException(
                    "No satisfaction provider registered for: " + key.value()
            );
        }
        return provider;
    }
}