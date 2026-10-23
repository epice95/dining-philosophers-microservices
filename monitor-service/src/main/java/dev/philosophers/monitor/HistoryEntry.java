package dev.philosophers.monitor;

import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;

/** Ligne d'historique exposée par l'API. */
public record HistoryEntry(long clock, EventType type, String emittedAt) {

    static HistoryEntry from(PhilosopherEvent event) {
        return new HistoryEntry(event.clock(), event.type(), event.emittedAt().toString());
    }
}
