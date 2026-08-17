package com.autovision.platform.aftersales;

import jakarta.validation.constraints.Size;

public record CustomerAuthorizationDecisionRequest(
        @Size(max = 32)
        String decisionChannel,

        @Size(max = 160)
        String decisionReference
) {
}
