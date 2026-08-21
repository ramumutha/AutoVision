package com.autovision.platform.aftersales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestQuoteCustomerAuthorizationRequest(
        @NotBlank
        @Size(max = 80)
        String authorizationNumber,

        @Size(max = 160)
        String customerReference,

        @Size(max = 200)
        String customerDisplayNameSnapshot,

        @NotBlank
        @Size(max = 500)
        String authorizationSummary
) {
}
