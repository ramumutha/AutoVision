package com.autovision.platform.aftersales;

import java.util.List;

public interface ServiceOrderAggregatePolicy {

    void validateWorkCompletion(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs,
            List<ServiceLine> serviceLines
    );

    void validateClose(
            ServiceOrder serviceOrder,
            List<ServiceJob> serviceJobs
    );
}