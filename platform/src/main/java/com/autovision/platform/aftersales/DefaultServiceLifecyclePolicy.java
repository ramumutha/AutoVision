package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

@Component
public class DefaultServiceLifecyclePolicy
        implements ServiceLifecyclePolicy {

    @Override
    public boolean isServiceOrderTransitionAllowed(
            ServiceOrderStatus currentStatus,
            ServiceOrderStatus targetStatus
    ) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        if (currentStatus == targetStatus) {
            return false;
        }

        return switch (currentStatus) {
            case OPEN ->
                    targetStatus == ServiceOrderStatus.IN_PROGRESS
                            || targetStatus == ServiceOrderStatus.WORK_COMPLETED
                            || targetStatus == ServiceOrderStatus.CANCELLED;

            case IN_PROGRESS ->
                    targetStatus == ServiceOrderStatus.WORK_COMPLETED
                            || targetStatus == ServiceOrderStatus.CANCELLED;

            case WORK_COMPLETED ->
                    targetStatus == ServiceOrderStatus.CLOSED;

            case CLOSED, CANCELLED -> false;
        };
    }

    @Override
    public boolean isServiceJobTransitionAllowed(
            ServiceJobStatus currentStatus,
            ServiceJobStatus targetStatus
    ) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        if (currentStatus == targetStatus) {
            return false;
        }

        return switch (currentStatus) {
            case OPEN ->
                    targetStatus == ServiceJobStatus.READY
                            || targetStatus == ServiceJobStatus.CANCELLED;

            case READY ->
                    targetStatus == ServiceJobStatus.IN_PROGRESS
                            || targetStatus == ServiceJobStatus.CANCELLED;

            case IN_PROGRESS ->
                    targetStatus == ServiceJobStatus.WORK_COMPLETED
                            || targetStatus == ServiceJobStatus.CANCELLED;

            case WORK_COMPLETED ->
                    targetStatus == ServiceJobStatus.CLOSED;

            case CLOSED, CANCELLED -> false;
        };
    }

    @Override
    public boolean isServiceJobReadyForExecution(
            ServiceJobApprovalStatus approvalStatus
    ) {
        return approvalStatus == ServiceJobApprovalStatus.NOT_REQUIRED
                || approvalStatus == ServiceJobApprovalStatus.APPROVED;
    }
}