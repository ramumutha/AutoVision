package com.autovision.platform.aftersales;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceOrderAggregateResponse(
        ServiceOrderResponse order,
        List<ServiceJobResponse> jobs,
        List<ServiceLineResponse> lines
) {

    public ServiceOrderAggregateResponse {
        if (order == null) {
            throw new IllegalArgumentException(
                    "Service order response is required"
            );
        }

        jobs = List.copyOf(jobs);
        lines = List.copyOf(lines);
    }

    public static ServiceOrderAggregateResponse from(
            ServiceOrderAggregateView aggregate
    ) {
        if (aggregate == null) {
            throw new IllegalArgumentException(
                    "Service order aggregate is required"
            );
        }

        return new ServiceOrderAggregateResponse(
                ServiceOrderResponse.from(
                        aggregate.order()
                ),
                aggregate.jobs()
                        .stream()
                        .map(ServiceJobResponse::from)
                        .toList(),
                aggregate.lines()
                        .stream()
                        .map(ServiceLineResponse::from)
                        .toList()
        );
    }

    public record ServiceOrderResponse(
            UUID id,
            UUID tenantId,
            UUID dealerId,
            UUID branchId,
            String orderNumber,
            UUID vehicleId,
            ServiceOrderStatus status,
            OffsetDateTime openedAt,
            OffsetDateTime completedAt,
            OffsetDateTime closedAt,
            OffsetDateTime cancelledAt,
            long version,
            UUID createdByPrincipalId,
            UUID updatedByPrincipalId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {

        static ServiceOrderResponse from(
                ServiceOrder order
        ) {
            return new ServiceOrderResponse(
                    order.getId(),
                    order.getTenantId(),
                    order.getDealerId(),
                    order.getBranchId(),
                    order.getOrderNumber(),
                    order.getVehicleId(),
                    order.getStatus(),
                    order.getOpenedAt(),
                    order.getCompletedAt(),
                    order.getClosedAt(),
                    order.getCancelledAt(),
                    order.getVersion(),
                    order.getCreatedByPrincipalId(),
                    order.getUpdatedByPrincipalId(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }

    public record ServiceJobResponse(
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

        static ServiceJobResponse from(
                ServiceJob job
        ) {
            return new ServiceJobResponse(
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

    public record ServiceLineResponse(
            UUID id,
            UUID serviceOrderId,
            UUID serviceJobId,
            int lineNumber,
            ServiceLineType lineType,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            String currencyCode,
            BigDecimal netAmount,
            BigDecimal taxAmount,
            BigDecimal grossAmount,
            boolean hasCommercialSnapshot,
            long version,
            UUID createdByPrincipalId,
            UUID updatedByPrincipalId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {

        static ServiceLineResponse from(
                ServiceLine line
        ) {
            return new ServiceLineResponse(
                    line.getId(),
                    line.getServiceOrderId(),
                    line.getServiceJobId(),
                    line.getLineNumber(),
                    line.getLineType(),
                    line.getDescription(),
                    line.getQuantity(),
                    line.getUnitOfMeasure(),
                    line.getUnitPrice(),
                    line.getCurrencyCode(),
                    line.getNetAmount(),
                    line.getTaxAmount(),
                    line.getGrossAmount(),
                    line.hasCommercialSnapshot(),
                    line.getVersion(),
                    line.getCreatedByPrincipalId(),
                    line.getUpdatedByPrincipalId(),
                    line.getCreatedAt(),
                    line.getUpdatedAt()
            );
        }
    }
}