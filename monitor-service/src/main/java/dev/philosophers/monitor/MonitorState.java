package dev.philosophers.monitor;

import dev.philosophers.common.event.PhilosopherEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** État global du monitor : un historique par philosophe. Thread-safe. */
public final class MonitorState {

    private final ConcurrentMap<String, PhilosopherHistory> histories = new ConcurrentHashMap<>();
    private final int historyCapacity;

    public MonitorState(int historyCapacity) {
        if (historyCapacity < 1) {
            throw new IllegalArgumentException(
                    "historyCapacity must be >= 1 but was " + historyCapacity);
        }
        this.historyCapacity = historyCapacity;
    }

    public RecordResult record(PhilosopherEvent event) {
        Objects.requireNonNull(event, "event");
        return histories
                .computeIfAbsent(
                        event.philosopherId(), id -> new PhilosopherHistory(id, historyCapacity))
                .record(event);
    }

    public List<PhilosopherView> snapshot() {
        return histories.values().stream()
                .map(PhilosopherHistory::view)
                .sorted(Comparator.comparing(PhilosopherView::id))
                .toList();
    }

    public Optional<List<PhilosopherEvent>> history(String philosopherId) {
        return Optional.ofNullable(histories.get(philosopherId)).map(PhilosopherHistory::history);
    }
}
