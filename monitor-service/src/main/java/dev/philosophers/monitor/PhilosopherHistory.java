package dev.philosophers.monitor;

import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Historique d'un philosophe, thread-safe.
 *
 * <p>Les horloges d'un philosophe sont consécutives (1, 2, 3...). On retient donc {@code
 * contiguousUpTo} (toutes les horloges de 1 à cette valeur sont reçues) et l'ensemble {@code
 * pending} des horloges reçues au-delà : les trous sont les valeurs absentes entre les deux. Seuls
 * les {@code capacity} derniers événements sont conservés pour l'affichage.
 */
final class PhilosopherHistory {

    private static final int MAX_REPORTED_GAPS = 100;

    private final String id;
    private final int capacity;
    private final NavigableSet<Long> pending = new TreeSet<>();
    private final NavigableMap<Long, PhilosopherEvent> recent = new TreeMap<>();
    private long contiguousUpTo;
    private long eventCount;

    PhilosopherHistory(String id, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1 but was " + capacity);
        }
        this.id = Objects.requireNonNull(id, "id");
        this.capacity = capacity;
    }

    synchronized RecordResult record(PhilosopherEvent event) {
        long clock = event.clock();
        if (clock <= contiguousUpTo || pending.contains(clock)) {
            PhilosopherEvent previous = recent.get(clock);
            if (previous != null && previous.type() != event.type()) {
                throw new ConflictingEventException(
                        "Event %s#%d already recorded as %s, received %s"
                                .formatted(id, clock, previous.type(), event.type()));
            }
            return RecordResult.DUPLICATE;
        }
        pending.add(clock);
        while (!pending.isEmpty() && pending.first().longValue() == contiguousUpTo + 1) {
            pending.pollFirst();
            contiguousUpTo++;
        }
        recent.put(clock, event);
        if (recent.size() > capacity) {
            recent.pollFirstEntry();
        }
        eventCount++;
        return RecordResult.ACCEPTED;
    }

    synchronized PhilosopherView view() {
        Map.Entry<Long, PhilosopherEvent> last = recent.lastEntry();
        PhilosopherState state = PhilosopherState.THINKING;
        long lastClock = 0;
        String lastEventAt = "";
        if (last != null) {
            state = toState(last.getValue().type());
            lastClock = last.getKey();
            lastEventAt = last.getValue().emittedAt().toString();
        }
        return new PhilosopherView(id, state, lastClock, lastEventAt, eventCount, missingClocks());
    }

    synchronized List<PhilosopherEvent> history() {
        return List.copyOf(recent.values());
    }

    private List<Long> missingClocks() {
        List<Long> missing = new ArrayList<>();
        long expected = contiguousUpTo + 1;
        for (long received : pending) {
            for (long m = expected; m < received && missing.size() < MAX_REPORTED_GAPS; m++) {
                missing.add(m);
            }
            expected = received + 1;
        }
        return missing;
    }

    private static PhilosopherState toState(EventType type) {
        return switch (type) {
            case HUNGRY -> PhilosopherState.HUNGRY;
            case EATING -> PhilosopherState.EATING;
            case DONE -> PhilosopherState.THINKING;
        };
    }
}
