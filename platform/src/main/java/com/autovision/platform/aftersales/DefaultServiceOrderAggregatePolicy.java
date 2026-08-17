package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultServiceOrderAggregatePolicy
        implements ServiceOrderAggregatePolicy {

    @Override
    public void validateWorkCompletion(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs,
            List<ServiceLine> serviceLines
    ) {
        if (serviceOrder == null) {
            throw new IllegalArgumentException(
                    "Service order is required"
            );
        }

        List<ServiceJob> jobs =
                serviceJobs == null ? List.of() : serviceJobs;

        List<ServiceLine> lines =
                serviceLines == null ? List.of() : serviceLines;

        validateAggregateMembership(
                serviceOrder,
                jobs,
                lines
        );

        validateJobsResolvedForWorkCompletion(jobs);

        if (!containsCompletedWork(jobs, lines)) {
            throw new IllegalStateException(
                    "Service order cannot complete without completed work"
            );
        }
    }

    @Override
    public void validateClose(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs
    ) {
        if (serviceOrder == null) {
            throw new IllegalArgumentException(
                    "Service order is required"
            );
        }

        List<ServiceJob> jobs =
                serviceJobs == null ? List.of() : serviceJobs;

        validateJobMembership(
                serviceOrder,
                jobs
        );

        for (ServiceJob job : jobs) {
            if (
                    job.getStatus() != ServiceJobStatus.CLOSED
                            && job.getStatus()
                            != ServiceJobStatus.CANCELLED
            ) {
                throw new IllegalStateException(
                        "Service order cannot close while service job "
                                + job.getJobNumber()
                                + " is "
                                + job.getStatus()
                );
            }
        }
    }

    private void validateAggregateMembership(
            ServiceOrder serviceOrder,
            List<ServiceJob> jobs,
            List<ServiceLine> lines
    ) {
        validateJobMembership(
                serviceOrder,
                jobs
        );

        UUID serviceOrderId = serviceOrder.getId();

        Set<UUID> jobIds = new HashSet<>();

        for (ServiceJob job : jobs) {
            jobIds.add(job.getId());
        }

        for (ServiceLine line : lines) {
            if (!serviceOrderId.equals(line.getServiceOrderId())) {
                throw new IllegalStateException(
                        "Service line does not belong to service order"
                );
            }

            if (
                    line.getServiceJobId() != null
                            && !jobIds.contains(line.getServiceJobId())
            ) {
                throw new IllegalStateException(
                        "Service line references a job outside the loaded service order aggregate"
                );
            }
        }
    }

    private void validateJobMembership(
            ServiceOrder serviceOrder,
            List<ServiceJob> jobs
    ) {
        UUID serviceOrderId = serviceOrder.getId();

        for (ServiceJob job : jobs) {
            if (!serviceOrderId.equals(job.getServiceOrderId())) {
                throw new IllegalStateException(
                        "Service job does not belong to service order"
                );
            }
        }
    }

    private void validateJobsResolvedForWorkCompletion(
            List<ServiceJob> jobs
    ) {
        for (ServiceJob job : jobs) {
            if (!isResolvedForWorkCompletion(job.getStatus())) {
                throw new IllegalStateException(
                        "Service order cannot complete while service job "
                                + job.getJobNumber()
                                + " is "
                                + job.getStatus()
                );
            }
        }
    }

    private boolean isResolvedForWorkCompletion(
            ServiceJobStatus status
    ) {
        return status == ServiceJobStatus.WORK_COMPLETED
                || status == ServiceJobStatus.CLOSED
                || status == ServiceJobStatus.CANCELLED;
    }

    private boolean containsCompletedWork(
            List<ServiceJob> jobs,
            List<ServiceLine> lines
    ) {
        if (!lines.isEmpty()) {
            return true;
        }

        return jobs.stream()
                .anyMatch(
                        job ->
                                job.getStatus()
                                        == ServiceJobStatus.WORK_COMPLETED
                                        || job.getStatus()
                                        == ServiceJobStatus.CLOSED
                );
    }
}