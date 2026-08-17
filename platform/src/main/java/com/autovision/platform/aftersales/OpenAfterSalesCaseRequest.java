package com.autovision.platform.aftersales;

import java.util.UUID;

public record OpenAfterSalesCaseRequest(
        String caseNumber,
        UUID dealerId,
        UUID branchId,
        AfterSalesCaseSourceChannel sourceChannel
) {
}