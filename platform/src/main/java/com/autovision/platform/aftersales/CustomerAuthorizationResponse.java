package com.autovision.platform.aftersales;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerAuthorizationResponse(
        UUID id,
        UUID tenantId,
        UUID dealerId,
        UUID branchId,
        UUID aftersalesCaseId,
        UUID serviceQuoteId,
        String authorizationNumber,
        CustomerAuthorizationStatus authorizationStatus,
        String customerReference,
        String customerDisplayNameSnapshot,
        String authorizationSummary,
        JsonNode authorizationScopeSnapshot,
        JsonNode commercialSnapshot,
        String termsSnapshot,
        String disclaimerSnapshot,
        OffsetDateTime requestedAt,
        OffsetDateTime decidedAt,
        String decisionChannel,
        String decisionReference,
        long version,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
