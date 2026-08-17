package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrderAggregateLifecycleTests {

    private final ServiceLifecyclePolicy lifecyclePolicy =
            new DefaultServiceLifecyclePolicy();

    private final ServiceOrderAggregatePolicy aggregatePolicy =
            new DefaultServiceOrderAggregatePolicy();

    private final ServiceOrderAggregateLifecycle lifecycle =
            new ServiceOrderAggregateLifecycle();

    @Test
    void completesQuickServiceOrderWithDirectServiceLine() {

        ServiceOrder order = newOrder();

        ServiceLine directLine =
                newLine(
                        order.getId(),
                        null,
                        10
                );

        lifecycle.completeWork(
                order,
                List.of(),
                List.of(directLine),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
    }

    @Test
    void completesOrderWhenAllJobsAreResolved() {

        ServiceOrder order = newOrder();

        ServiceJob completedJob =
                completedJob(order.getId(), "JOB-001");

        ServiceJob cancelledJob =
                cancelledJob(order.getId(), "JOB-002");

        lifecycle.completeWork(
                order,
                List.of(
                        completedJob,
                        cancelledJob
                ),
                List.of(),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
    }

    @Test
    void allowsCancelledJobAlongsideCompletedDirectWork() {

        ServiceOrder order = newOrder();

        ServiceJob cancelledJob =
                cancelledJob(order.getId(), "JOB-001");

        ServiceLine directLine =
                newLine(
                        order.getId(),
                        null,
                        10
                );

        lifecycle.completeWork(
                order,
                List.of(cancelledJob),
                List.of(directLine),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
    }

    @Test
    void rejectsEmptyServiceOrderCompletion() {

        ServiceOrder order = newOrder();

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );

        assertEquals(
                ServiceOrderStatus.OPEN,
                order.getStatus()
        );
    }

    @Test
    void rejectsOrderContainingOnlyCancelledJobs() {

        ServiceOrder order = newOrder();

        ServiceJob cancelledJob =
                cancelledJob(order.getId(), "JOB-001");

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(cancelledJob),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsCompletionWhileJobIsOpen() {

        ServiceOrder order = newOrder();

        ServiceJob openJob = ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                "JOB-001",
                "Open job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(openJob),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsCompletionWhileJobIsReady() {

        ServiceOrder order = newOrder();

        ServiceJob job = ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                "JOB-001",
                "Ready job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        job.markReady(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(job),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsCompletionWhileJobIsInProgress() {

        ServiceOrder order = newOrder();

        ServiceJob job = ServiceJob.open(
                UUID.randomUUID(),
                order.getId(),
                "JOB-001",
                "Active job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        job.markReady(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        job.start(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(job),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsJobFromDifferentServiceOrder() {

        ServiceOrder order = newOrder();

        ServiceJob foreignJob =
                completedJob(
                        UUID.randomUUID(),
                        "JOB-FOREIGN"
                );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(foreignJob),
                        List.of(),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsServiceLineFromDifferentServiceOrder() {

        ServiceOrder order = newOrder();

        ServiceLine foreignLine =
                newLine(
                        UUID.randomUUID(),
                        null,
                        10
                );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(),
                        List.of(foreignLine),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void rejectsJobAssignedLineWhenJobIsMissingFromLoadedAggregate() {

        ServiceOrder order = newOrder();

        ServiceLine jobLine =
                newLine(
                        order.getId(),
                        UUID.randomUUID(),
                        10
                );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.completeWork(
                        order,
                        List.of(),
                        List.of(jobLine),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void closesDirectWorkOrderWithoutServiceJobs() {

        ServiceOrder order = newOrder();

        ServiceLine directLine =
                newLine(
                        order.getId(),
                        null,
                        10
                );

        lifecycle.completeWork(
                order,
                List.of(),
                List.of(directLine),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        lifecycle.close(
                order,
                List.of(),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceOrderStatus.CLOSED,
                order.getStatus()
        );
    }

    @Test
    void closesOrderWhenJobsAreClosedOrCancelled() {

        ServiceOrder order = newOrder();

        ServiceJob closedJob =
                completedJob(
                        order.getId(),
                        "JOB-001"
                );

        closedJob.close(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        ServiceJob cancelledJob =
                cancelledJob(
                        order.getId(),
                        "JOB-002"
                );

        lifecycle.completeWork(
                order,
                List.of(
                        closedJob,
                        cancelledJob
                ),
                List.of(),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        lifecycle.close(
                order,
                List.of(
                        closedJob,
                        cancelledJob
                ),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceOrderStatus.CLOSED,
                order.getStatus()
        );
    }

    @Test
    void rejectsOrderClosureWhileJobIsOnlyWorkCompleted() {

        ServiceOrder order = newOrder();

        ServiceJob completedJob =
                completedJob(
                        order.getId(),
                        "JOB-001"
                );

        lifecycle.completeWork(
                order,
                List.of(completedJob),
                List.of(),
                lifecyclePolicy,
                aggregatePolicy,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.close(
                        order,
                        List.of(completedJob),
                        lifecyclePolicy,
                        aggregatePolicy,
                        null,
                        OffsetDateTime.now()
                )
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
    }
    private ServiceOrder newOrder() {
        return ServiceOrder.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "SO-" + UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                OffsetDateTime.now()
        );
    }

    private ServiceJob completedJob(
            UUID serviceOrderId,
            String jobNumber
    ) {
        ServiceJob job = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrderId,
                jobNumber,
                "Completed job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        job.markReady(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        job.start(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        job.completeWork(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        return job;
    }

    private ServiceJob cancelledJob(
            UUID serviceOrderId,
            String jobNumber
    ) {
        ServiceJob job = ServiceJob.open(
                UUID.randomUUID(),
                serviceOrderId,
                jobNumber,
                "Cancelled job",
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        job.cancel(
                lifecyclePolicy,
                null,
                OffsetDateTime.now()
        );

        return job;
    }

    private ServiceLine newLine(
            UUID serviceOrderId,
            UUID serviceJobId,
            int lineNumber
    ) {
        return ServiceLine.create(
                UUID.randomUUID(),
                serviceOrderId,
                serviceJobId,
                lineNumber,
                ServiceLineType.LABOR,
                "Aggregate lifecycle test line",
                BigDecimal.ONE,
                "EA",
                null,
                OffsetDateTime.now()
        );
    }
}