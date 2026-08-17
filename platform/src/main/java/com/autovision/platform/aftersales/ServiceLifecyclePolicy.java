package com.autovision.platform.aftersales;

public interface ServiceLifecyclePolicy {

    boolean isServiceOrderTransitionAllowed(
            ServiceOrderStatus currentStatus,
            ServiceOrderStatus targetStatus
    );

    boolean isServiceJobTransitionAllowed(
            ServiceJobStatus currentStatus,
            ServiceJobStatus targetStatus
    );

    boolean isServiceJobReadyForExecution(
            ServiceJobApprovalStatus approvalStatus
    );
}