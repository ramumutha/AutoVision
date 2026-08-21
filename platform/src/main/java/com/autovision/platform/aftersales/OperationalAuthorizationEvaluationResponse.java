package com.autovision.platform.aftersales;

import java.util.UUID;

public record OperationalAuthorizationEvaluationResponse(
        UUID serviceJobId,
        UUID serviceOrderId,
        OperationalAuthorizationStatus status,
        int totalLineCount,
        int authorizedLineCount,
        int pendingLineCount,
        int notAuthorizedLineCount
) {
}
