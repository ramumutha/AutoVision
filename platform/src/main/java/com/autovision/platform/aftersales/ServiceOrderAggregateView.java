package com.autovision.platform.aftersales;

import java.util.List;

/**
 * Read-only application projection of the canonical ServiceOrder aggregate.
 *
 * <p>The projection deliberately keeps jobs and lines as independent
 * collections. A ServiceOrder may contain lines without requiring a
 * ServiceJob, and neither jobs nor lines imply an Inspection dependency.</p>
 */
public record ServiceOrderAggregateView(
        ServiceOrder order,
        List<ServiceJob> jobs,
        List<ServiceLine> lines
) {

    public ServiceOrderAggregateView {
        if (order == null) {
            throw new IllegalArgumentException(
                    "Service order is required"
            );
        }

        jobs = List.copyOf(jobs);
        lines = List.copyOf(lines);
    }
}