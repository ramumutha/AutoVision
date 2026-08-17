package com.autovision.platform.aftersales;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ServiceOrderAggregateLifecycle {

    public void completeWork(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs,
            List<ServiceLine> serviceLines,
            ServiceLifecyclePolicy lifecyclePolicy,
            ServiceOrderAggregatePolicy aggregatePolicy,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireOrderAndAggregatePolicy(
                serviceOrder,
                aggregatePolicy
        );

        aggregatePolicy.validateWorkCompletion(
                serviceOrder,
                serviceJobs,
                serviceLines
        );

        serviceOrder.completeWork(
                lifecyclePolicy,
                principalId,
                now
        );
    }

    public void close(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs,
            ServiceLifecyclePolicy lifecyclePolicy,
            ServiceOrderAggregatePolicy aggregatePolicy,
            UUID principalId,
            OffsetDateTime now
    ) {
        requireOrderAndAggregatePolicy(
                serviceOrder,
                aggregatePolicy
        );

        aggregatePolicy.validateClose(
                serviceOrder,
                serviceJobs
        );

        serviceOrder.close(
                lifecyclePolicy,
                principalId,
                now
        );
    }

    private void requireOrderAndAggregatePolicy(
            ServiceOrder serviceOrder,
            ServiceOrderAggregatePolicy aggregatePolicy
    ) {
        if (serviceOrder == null) {
            throw new IllegalArgumentException(
                    "Service order is required"
            );
        }

        if (aggregatePolicy == null) {
            throw new IllegalArgumentException(
                    "Service order aggregate policy is required"
            );
        }
    }
}