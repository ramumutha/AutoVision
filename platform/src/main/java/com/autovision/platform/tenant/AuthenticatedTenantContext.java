package com.autovision.platform.tenant;

import java.util.UUID;

public record AuthenticatedTenantContext(
        UUID userRefId,
        UUID tenantId,
        String externalUserId
) {
}