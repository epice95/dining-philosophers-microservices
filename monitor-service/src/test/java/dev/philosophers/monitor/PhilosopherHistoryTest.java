package dev.philosophers.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PhilosopherHistoryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private static EventType typeFor(long clock) {
        return EventType.values()[(int) ((clock - 1) % 3)];
    }

    private static PhilosopherEvent event(long clock, EventType type) {
        return new PhilosopherEvent("p1", type, clock, NOW);
    }

    @Test
    void acceptsInOrderEventsAndTracksState() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        history.record(event(1, EventType.HUNGRY));
        history.record(event(2, EventType.EATING));
        history.record(event(3, EventType.DONE));

        PhilosopherView view = history.view();
        assertEquals(PhilosopherState.THINKING, view.state());
        assertEquals(3, view.lastClock());
        assertEquals(3, view.eventCount());
        assertEquals(List.of(), view.missingClocks());
    }

    @Test
    void stateFollowsHighestClockNotArrivalOrder() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        history.record(event(2, EventType.EATING));
        history.record(event(1, EventType.HUNGRY));

        PhilosopherView view = history.view();
        assertEquals(PhilosopherState.EATING, view.state());
        assertEquals(List.of(), view.missingClocks());
    }

    @Test
    void detectsMissingClocks() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        history.record(event(1, EventType.HUNGRY));
        history.record(event(3, EventType.DONE));
        history.record(event(6, EventType.EATING));

        assertEquals(List.of(2L, 4L, 5L), history.view().missingClocks());
    }

    @Test
    void gapFillsWhenMissingEventArrivesLate() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        history.record(event(1, EventType.HUNGRY));
        history.record(event(3, EventType.DONE));
        history.record(event(2, EventType.EATING));

        PhilosopherView view = history.view();
        assertEquals(List.of(), view.missingClocks());
        assertEquals(3, view.eventCount());
    }

    @Test
    void duplicateReplayIsIdempotent() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        assertEquals(RecordResult.ACCEPTED, history.record(event(1, EventType.HUNGRY)));
        assertEquals(RecordResult.DUPLICATE, history.record(event(1, EventType.HUNGRY)));

        assertEquals(1, history.view().eventCount());
    }

    @Test
    void conflictingEventWithSameClockIsRejected() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        history.record(event(1, EventType.HUNGRY));

        assertThrows(
                ConflictingEventException.class, () -> history.record(event(1, EventType.EATING)));

        PhilosopherView view = history.view();
        assertEquals(PhilosopherState.HUNGRY, view.state());
        assertEquals(1, view.eventCount());
    }

    @Test
    void historyIsBoundedAndSortedByClock() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 3);
        for (long clock = 1; clock <= 5; clock++) {
            history.record(event(clock, typeFor(clock)));
        }

        assertEquals(
                List.of(3L, 4L, 5L),
                history.history().stream().map(PhilosopherEvent::clock).toList());
        PhilosopherView view = history.view();
        assertEquals(5, view.eventCount());
        assertEquals(List.of(), view.missingClocks());
    }

    @Test
    void duplicateOfCompactedEventIsStillADuplicate() {
        PhilosopherHistory history = new PhilosopherHistory("p1", 2);
        for (long clock = 1; clock <= 5; clock++) {
            history.record(event(clock, typeFor(clock)));
        }

        assertEquals(RecordResult.DUPLICATE, history.record(event(1, typeFor(1))));
        assertEquals(5, history.view().eventCount());
    }

    @Test
    void concurrentReplaysAcceptEachEventExactlyOnce() throws Exception {
        int threads = 8;
        int events = 2000;
        PhilosopherHistory history = new PhilosopherHistory("p1", 100);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int offset = t * (events / threads);
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    start.await();
                                    for (int i = 0; i < events; i++) {
                                        long clock = ((i + offset) % events) + 1;
                                        RecordResult result =
                                                history.record(event(clock, typeFor(clock)));
                                        if (result == RecordResult.ACCEPTED) {
                                            accepted.incrementAndGet();
                                        } else {
                                            duplicates.incrementAndGet();
                                        }
                                    }
                                    return null;
                                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<Object> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            assertEquals(events, accepted.get());
            assertEquals(events * (threads - 1), duplicates.get());
            PhilosopherView view = history.view();
            assertEquals(events, view.eventCount());
            assertEquals(List.of(), view.missingClocks());
        } finally {
            pool.shutdownNow();
        }
    }
}
