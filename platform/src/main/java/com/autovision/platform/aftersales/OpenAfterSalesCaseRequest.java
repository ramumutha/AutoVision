package com.autovision.platform.aftersales;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OpenAfterSalesCaseRequest(
        @NotBlank
        @Size(max = 80)
        String caseNumber,

        UUID dealerId,

        UUID branchId,

        @NotNull
        AfterSalesCaseSourceChannel sourceChannel
) {
    @AssertTrue(message = "dealerId is required when branchId is provided")
    public boolean isDealerContextValid() {
        return branchId == null || dealerId != null;
    }
}