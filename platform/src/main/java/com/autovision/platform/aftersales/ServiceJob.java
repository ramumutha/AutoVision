package com.autovision.platform.aftersales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "service_jobs",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_service_jobs_order_job_number",
                        columnNames = {"service_order_id", "job_number"}
                ),
                @UniqueConstraint(
                        name = "uq_service_jobs_id_service_order",
                        columnNames = {"id", "service_order_id"}
                )
        }
)
public class ServiceJob {

    @Id
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "job_number", nullable = false, length = 80)
    private String jobNumber;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ServiceJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 32)
    private ServiceJobApprovalStatus approvalStatus;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by_principal_id")
    private UUID approvedByPrincipalId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_by_principal_id")
    private UUID createdByPrincipalId;

    @Column(name = "updated_by_principal_id")
    private UUID updatedByPrincipalId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ServiceJob() {
    }

    public static ServiceJob open(
            UUID id,
            UUID serviceOrderId,
            String jobNumber,
            String summary,
            ServiceJobApprovalStatus approvalStatus,
            UUID principalId,
            OffsetDateTime now
    ) {
        ServiceJob serviceJob = new ServiceJob();

        serviceJob.id = id;
        serviceJob.serviceOrderId = serviceOrderId;
        serviceJob.jobNumber = jobNumber;
        serviceJob.summary = summary;
        serviceJob.status = ServiceJobStatus.OPEN;
        serviceJob.approvalStatus = approvalStatus;
        serviceJob.openedAt = now;
        serviceJob.createdByPrincipalId = principalId;
        serviceJob.updatedByPrincipalId = principalId;
        serviceJob.createdAt = now;
        serviceJob.updatedAt = now;

        return serviceJob;
    }

    public void markReady(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        requirePolicy(policy);

        if (!policy.isServiceJobReadyForExecution(approvalStatus)) {
            throw new IllegalStateException(
                    "Service job approval requirements are not satisfied"
            );
        }

        transition(
                ServiceJobStatus.READY,
                policy,
                principalId,
                now
        );

        readyAt = now;
    }

    public void start(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceJobStatus.IN_PROGRESS,
                policy,
                principalId,
                now
        );

        startedAt = now;
    }

    public void completeWork(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceJobStatus.WORK_COMPLETED,
                policy,
                principalId,
                now
        );

        completedAt = now;
    }

    public void close(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceJobStatus.CLOSED,
                policy,
                principalId,
                now
        );

        closedAt = now;
    }

    public void cancel(
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        transition(
                ServiceJobStatus.CANCELLED,
                policy,
                principalId,
                now
        );

        cancelledAt = now;
    }

    private void transition(
            ServiceJobStatus targetStatus,
            ServiceLifecyclePolicy policy,
            UUID principalId,
            OffsetDateTime now
    ) {
        requirePolicy(policy);

        if (!policy.isServiceJobTransitionAllowed(
                status,
                targetStatus
        )) {
            throw new IllegalStateException(
                    "Service job transition is not allowed: "
                            + status
                            + " -> "
                            + targetStatus
            );
        }

        status = targetStatus;
        updatedByPrincipalId = principalId;
        updatedAt = now;
    }

    private void requirePolicy(
            ServiceLifecyclePolicy policy
    ) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Service lifecycle policy is required"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public String getSummary() {
        return summary;
    }

    public ServiceJobStatus getStatus() {
        return status;
    }

    public ServiceJobApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getReadyAt() {
        return readyAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public UUID getApprovedByPrincipalId() {
        return approvedByPrincipalId;
    }

    public long getVersion() {
        return version;
    }

    public UUID getCreatedByPrincipalId() {
        return createdByPrincipalId;
    }

    public UUID getUpdatedByPrincipalId() {
        return updatedByPrincipalId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}