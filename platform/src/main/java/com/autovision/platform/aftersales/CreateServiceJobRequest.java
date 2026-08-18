package com.autovision.platform.aftersales;

public record CreateServiceJobRequest(
        String jobNumber,
        String summary,
        ServiceJobApprovalStatus approvalStatus
) {
}