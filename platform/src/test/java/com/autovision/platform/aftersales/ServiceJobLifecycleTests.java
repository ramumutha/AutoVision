package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceJobLifecycleTests {

    private final ServiceLifecyclePolicy policy =
            new DefaultServiceLifecyclePolicy();

    @Test
    void supportsCanonicalJobLifecycleWhenApprovalNotRequired() {

        UUID principalId = UUID.randomUUID();
        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                principalId,
                openedAt
        );

        OffsetDateTime readyAt =
                openedAt.plusMinutes(5);

        OffsetDateTime startedAt =
                openedAt.plusMinutes(10);

        OffsetDateTime completedAt =
                openedAt.plusHours(2);

        job.markReady(
                policy,
                principalId,
                readyAt
        );

        assertEquals(
                ServiceJobStatus.READY,
                job.getStatus()
        );
        assertEquals(readyAt, job.getReadyAt());

        job.start(
                policy,
                principalId,
                startedAt
        );

        assertEquals(
                ServiceJobStatus.IN_PROGRESS,
                job.getStatus()
        );
        assertEquals(startedAt, job.getStartedAt());

        job.completeWork(
                policy,
                principalId,
                completedAt
        );

        assertEquals(
                ServiceJobStatus.WORK_COMPLETED,
                job.getStatus()
        );
        assertEquals(
                completedAt,
                job.getCompletedAt()
        );

        OffsetDateTime closedAt =
                completedAt.plusMinutes(30);

        job.close(
                policy,
                principalId,
                closedAt
        );

        assertEquals(
                ServiceJobStatus.CLOSED,
                job.getStatus()
        );

        assertEquals(
                closedAt,
                job.getClosedAt()
        );
    }

    @Test
    void allowsApprovedJobToBecomeReady() {

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.APPROVED,
                null,
                OffsetDateTime.now()
        );

        job.markReady(
                policy,
                null,
                OffsetDateTime.now()
        );

        assertEquals(
                ServiceJobStatus.READY,
                job.getStatus()
        );
    }

    @Test
    void rejectsPendingApprovalJobFromBecomingReady() {

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.PENDING,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> job.markReady(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                job.getStatus()
        );
    }

    @Test
    void rejectsDeclinedApprovalJobFromBecomingReady() {

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.DECLINED,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> job.markReady(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );

        assertEquals(
                ServiceJobStatus.OPEN,
                job.getStatus()
        );
    }

    @Test
    void rejectsSkippingReadyState() {

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> job.start(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void supportsJobCancellationFromReadyState() {

        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                openedAt
        );

        job.markReady(
                policy,
                null,
                openedAt.plusMinutes(5)
        );

        OffsetDateTime cancelledAt =
                openedAt.plusMinutes(10);

        job.cancel(
                policy,
                null,
                cancelledAt
        );

        assertEquals(
                ServiceJobStatus.CANCELLED,
                job.getStatus()
        );
        assertEquals(
                cancelledAt,
                job.getCancelledAt()
        );
    }

    @Test
    void rejectsTransitionsAfterTerminalState() {

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                OffsetDateTime.now()
        );

        job.cancel(
                policy,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> job.markReady(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void updatesActualExecutionTimestamps() {

        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                null,
                openedAt
        );

        OffsetDateTime readyAt =
                openedAt.plusMinutes(5);

        OffsetDateTime actualStart =
                openedAt.plusMinutes(15);

        OffsetDateTime actualCompletion =
                openedAt.plusHours(1);

        job.markReady(policy, null, readyAt);
        job.start(policy, null, actualStart);
        job.completeWork(
                policy,
                null,
                actualCompletion
        );

        assertEquals(
                actualStart,
                job.getStartedAt()
        );
        assertEquals(
                actualCompletion,
                job.getCompletedAt()
        );
    }

    @Test
    void updatesActingPrincipalDuringTransition() {

        UUID transitionPrincipal = UUID.randomUUID();

        ServiceJob job = newJob(
                ServiceJobApprovalStatus.NOT_REQUIRED,
                UUID.randomUUID(),
                OffsetDateTime.now()
        );

        job.markReady(
                policy,
                transitionPrincipal,
                OffsetDateTime.now()
        );

        assertEquals(
                transitionPrincipal,
                job.getUpdatedByPrincipalId()
        );
        assertNotNull(job.getUpdatedAt());
    }

    private ServiceJob newJob(
            ServiceJobApprovalStatus approvalStatus,
            UUID principalId,
            OffsetDateTime now
    ) {
        return ServiceJob.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "JOB-" + UUID.randomUUID(),
                "Lifecycle test job",
                approvalStatus,
                principalId,
                now
        );
    }
}