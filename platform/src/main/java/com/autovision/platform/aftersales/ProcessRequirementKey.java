package com.autovision.platform.aftersales;

import java.util.Objects;

public record ProcessRequirementKey(String value) {
    public ProcessRequirementKey {
        Objects.requireNonNull(value, "Process requirement key is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Process requirement key must not be blank"
            );
        }
    }
}
