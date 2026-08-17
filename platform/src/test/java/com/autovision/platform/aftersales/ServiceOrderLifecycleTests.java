package com.autovision.platform.aftersales;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceOrderLifecycleTests {

    private final ServiceLifecyclePolicy policy =
            new DefaultServiceLifecyclePolicy();

    @Test
    void supportsNormalServiceOrderLifecycle() {

        UUID principalId = UUID.randomUUID();
        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceOrder order =
                newServiceOrder(principalId, openedAt);

        OffsetDateTime startedAt = openedAt.plusMinutes(5);
        OffsetDateTime completedAt = openedAt.plusHours(2);
        OffsetDateTime closedAt = openedAt.plusHours(3);

        order.start(policy, principalId, startedAt);

        assertEquals(
                ServiceOrderStatus.IN_PROGRESS,
                order.getStatus()
        );
        assertEquals(startedAt, order.getUpdatedAt());

        order.completeWork(
                policy,
                principalId,
                completedAt
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
        assertEquals(completedAt, order.getCompletedAt());

        order.close(
                policy,
                principalId,
                closedAt
        );

        assertEquals(
                ServiceOrderStatus.CLOSED,
                order.getStatus()
        );
        assertEquals(closedAt, order.getClosedAt());
    }

    @Test
    void supportsQuickServiceOrderCompletionWithoutInProgressState() {

        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceOrder order =
                newServiceOrder(null, openedAt);

        OffsetDateTime completedAt =
                openedAt.plusMinutes(15);

        order.completeWork(
                policy,
                null,
                completedAt
        );

        assertEquals(
                ServiceOrderStatus.WORK_COMPLETED,
                order.getStatus()
        );
        assertEquals(
                completedAt,
                order.getCompletedAt()
        );
    }

    @Test
    void supportsCancellationFromOpenState() {

        OffsetDateTime openedAt = OffsetDateTime.now();

        ServiceOrder order =
                newServiceOrder(null, openedAt);

        OffsetDateTime cancelledAt =
                openedAt.plusMinutes(10);

        order.cancel(
                policy,
                null,
                cancelledAt
        );

        assertEquals(
                ServiceOrderStatus.CANCELLED,
                order.getStatus()
        );
        assertEquals(
                cancelledAt,
                order.getCancelledAt()
        );
    }

    @Test
    void rejectsInvalidAndRepeatedTransitions() {

        ServiceOrder order =
                newServiceOrder(
                        null,
                        OffsetDateTime.now()
                );

        assertThrows(
                IllegalStateException.class,
                () -> order.close(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );

        order.start(
                policy,
                null,
                OffsetDateTime.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> order.start(
                        policy,
                        null,
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void updatesActingPrincipalDuringTransition() {

        UUID createPrincipal = UUID.randomUUID();
        UUID transitionPrincipal = UUID.randomUUID();

        ServiceOrder order =
                newServiceOrder(
                        createPrincipal,
                        OffsetDateTime.now()
                );

        order.start(
                policy,
                transitionPrincipal,
                OffsetDateTime.now()
        );

        assertEquals(
                transitionPrincipal,
                order.getUpdatedByPrincipalId()
        );
        assertNotNull(order.getUpdatedAt());
    }

    private ServiceOrder newServiceOrder(
            UUID principalId,
            OffsetDateTime now
    ) {
        return ServiceOrder.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "SO-" + UUID.randomUUID(),
                UUID.randomUUID(),
                principalId,
                now
        );
    }
}