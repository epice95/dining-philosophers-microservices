package dev.philosophers.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MonitorStateTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    void tracksPhilosophersIndependentlySortedById() {
        MonitorState state = new MonitorState(50);
        state.record(new PhilosopherEvent("b", EventType.HUNGRY, 1, NOW));
        state.record(new PhilosopherEvent("a", EventType.HUNGRY, 1, NOW));
        state.record(new PhilosopherEvent("a", EventType.EATING, 2, NOW));

        List<PhilosopherView> snapshot = state.snapshot();
        assertEquals(List.of("a", "b"), snapshot.stream().map(PhilosopherView::id).toList());
        assertEquals(PhilosopherState.EATING, snapshot.get(0).state());
        assertEquals(PhilosopherState.HUNGRY, snapshot.get(1).state());
    }

    @Test
    void unknownPhilosopherHasNoHistory() {
        assertTrue(new MonitorState(10).history("ghost").isEmpty());
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new MonitorState(0));
    }
}
