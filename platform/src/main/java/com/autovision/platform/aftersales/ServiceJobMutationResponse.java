package com.autovision.platform.aftersales;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceJobMutationResponse(
        UUID id,
        UUID serviceOrderId,
        String jobNumber,
        String summary,
        ServiceJobStatus status,
        ServiceJobApprovalStatus approvalStatus,
        OffsetDateTime openedAt,
        OffsetDateTime readyAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime closedAt,
        OffsetDateTime approvedAt,
        UUID approvedByPrincipalId,
        long version,
        UUID createdByPrincipalId,
        UUID updatedByPrincipalId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ServiceJobMutationResponse from(
            ServiceJob job
    ) {
        if (job == null) {
            throw new IllegalArgumentException(
                    "Service job is required"
            );
        }

        return new ServiceJobMutationResponse(
                job.getId(),
                job.getServiceOrderId(),
                job.getJobNumber(),
                job.getSummary(),
                job.getStatus(),
                job.getApprovalStatus(),
                job.getOpenedAt(),
                job.getReadyAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCancelledAt(),
                job.getClosedAt(),
                job.getApprovedAt(),
                job.getApprovedByPrincipalId(),
                job.getVersion(),
                job.getCreatedByPrincipalId(),
                job.getUpdatedByPrincipalId(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}