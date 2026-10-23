package dev.philosophers.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PhilosopherEventTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    void createsValidEvent() {
        PhilosopherEvent event = new PhilosopherEvent("p1", EventType.HUNGRY, 1, NOW);
        assertEquals("p1", event.philosopherId());
        assertEquals(EventType.HUNGRY, event.type());
        assertEquals(1, event.clock());
        assertEquals(NOW, event.emittedAt());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PhilosopherEvent(" ", EventType.HUNGRY, 1, NOW));
    }

    @Test
    void rejectsClockBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PhilosopherEvent("p1", EventType.HUNGRY, 0, NOW));
    }

    @Test
    void rejectsNullType() {
        assertThrows(NullPointerException.class, () -> new PhilosopherEvent("p1", null, 1, NOW));
    }
}
