package com.autovision.platform.aftersales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record CreateCustomerAuthorizationRequest(
        @NotBlank
        @Size(max = 80)
        String authorizationNumber,

        @Size(max = 160)
        String customerReference,

        @Size(max = 200)
        String customerDisplayNameSnapshot,

        @NotBlank
        @Size(max = 500)
        String authorizationSummary,

        @NotNull
        JsonNode authorizationScopeSnapshot,

        UUID serviceQuoteId,

        JsonNode commercialSnapshot,

        String termsSnapshot,

        String disclaimerSnapshot
) {
}
