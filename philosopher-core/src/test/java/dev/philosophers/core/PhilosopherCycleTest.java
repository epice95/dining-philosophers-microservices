package dev.philosophers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.philosophers.common.clock.LamportClock;
import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PhilosopherCycleTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private static final Duration THINK = Duration.ofMillis(120);
    private static final Duration EAT = Duration.ofMillis(80);

    private final List<String> journal = new ArrayList<>();
    private final List<PhilosopherEvent> events = new ArrayList<>();

    private final EventPublisher recordingPublisher =
            event -> {
                journal.add("event:" + event.type());
                events.add(event);
            };

    private final Pause recordingPause = duration -> journal.add("pause:" + duration.toMillis());

    private static DurationSource fixedDurations() {
        return new DurationSource() {
            @Override
            public Duration thinkTime() {
                return THINK;
            }

            @Override
            public Duration eatTime() {
                return EAT;
            }
        };
    }

    private PhilosopherCycle cycle(EventPublisher publisher, Pause pause) {
        return new PhilosopherCycle(
                "p1",
                new LamportClock(),
                publisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                fixedDurations(),
                pause);
    }

    @Test
    void followsThinkHungryEatDoneOrder() throws Exception {
        cycle(recordingPublisher, recordingPause).runOnce();

        assertEquals(
                List.of("pause:120", "event:HUNGRY", "event:EATING", "pause:80", "event:DONE"),
                journal);
    }

    @Test
    void eventsCarryIdClockAndTimestamp() throws Exception {
        cycle(recordingPublisher, recordingPause).runOnce();

        assertEquals(List.of(1L, 2L, 3L), events.stream().map(PhilosopherEvent::clock).toList());
        for (PhilosopherEvent event : events) {
            assertEquals("p1", event.philosopherId());
            assertEquals(NOW, event.emittedAt());
        }
    }

    @Test
    void clockKeepsIncreasingAcrossCycles() throws Exception {
        PhilosopherCycle cycle = cycle(recordingPublisher, recordingPause);
        cycle.runOnce();
        cycle.runOnce();

        assertEquals(
                List.of(1L, 2L, 3L, 4L, 5L, 6L),
                events.stream().map(PhilosopherEvent::clock).toList());
    }

    @Test
    void interruptionWhileEatingStopsBeforeDone() {
        AtomicInteger calls = new AtomicInteger();
        Pause interruptingPause =
                duration -> {
                    if (calls.incrementAndGet() == 2) {
                        throw new InterruptedException("stop");
                    }
                };

        assertThrows(
                InterruptedException.class,
                () -> cycle(recordingPublisher, interruptingPause).runOnce());

        assertEquals(
                List.of(EventType.HUNGRY, EventType.EATING),
                events.stream().map(PhilosopherEvent::type).toList());
    }

    @Test
    void failedPublishStillConsumesItsClockValue() throws Exception {
        AtomicBoolean failNextEating = new AtomicBoolean(true);
        EventPublisher flakyPublisher =
                event -> {
                    if (event.type() == EventType.EATING && failNextEating.getAndSet(false)) {
                        throw new IllegalStateException("monitor unreachable");
                    }
                    events.add(event);
                };
        PhilosopherCycle cycle = cycle(flakyPublisher, recordingPause);

        assertThrows(IllegalStateException.class, cycle::runOnce);
        cycle.runOnce();

        // La valeur 2 (EATING perdu) n'est jamais réutilisée : le trou reste visible.
        assertEquals(
                List.of(1L, 3L, 4L, 5L), events.stream().map(PhilosopherEvent::clock).toList());
    }
}
