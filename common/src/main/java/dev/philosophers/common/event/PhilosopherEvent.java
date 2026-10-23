package dev.philosophers.common.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement émis par un philosophe.
 *
 * @param philosopherId identifiant du philosophe émetteur (non vide)
 * @param type type d'événement
 * @param clock valeur de l'horloge logique locale à l'émission (>= 1)
 * @param emittedAt instant physique d'émission (informatif, ne sert pas à l'ordonnancement)
 */
public record PhilosopherEvent(
        String philosopherId, EventType type, long clock, Instant emittedAt) {

    public PhilosopherEvent {
        Objects.requireNonNull(philosopherId, "philosopherId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(emittedAt, "emittedAt");
        if (philosopherId.isBlank()) {
            throw new IllegalArgumentException("philosopherId must not be blank");
        }
        if (clock < 1) {
            throw new IllegalArgumentException("clock must be >= 1 but was " + clock);
        }
    }
}
