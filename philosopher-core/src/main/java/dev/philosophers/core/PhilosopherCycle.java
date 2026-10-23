package dev.philosophers.core;

import dev.philosophers.common.clock.LamportClock;
import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Cycle d'un philosophe gâté : penser, avoir faim, manger, puis recommencer.
 *
 * <p>Chaque événement consomme une valeur de l'horloge logique locale. Si l'envoi échoue, la valeur
 * n'est pas réutilisée : le monitor voit alors un trou dans la séquence et peut le signaler.
 */
public final class PhilosopherCycle {

    private final String id;
    private final LamportClock clock;
    private final EventPublisher publisher;
    private final Clock wallClock;
    private final DurationSource durations;
    private final Pause pause;

    public PhilosopherCycle(
            String id,
            LamportClock clock,
            EventPublisher publisher,
            Clock wallClock,
            DurationSource durations,
            Pause pause) {
        this.id = Objects.requireNonNull(id, "id");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.wallClock = Objects.requireNonNull(wallClock, "wallClock");
        this.durations = Objects.requireNonNull(durations, "durations");
        this.pause = Objects.requireNonNull(pause, "pause");
    }

    /** Exécute un cycle complet : réflexion, HUNGRY, EATING, repas, DONE. */
    public void runOnce() throws InterruptedException {
        pause.sleep(durations.thinkTime());
        emit(EventType.HUNGRY);
        emit(EventType.EATING);
        pause.sleep(durations.eatTime());
        emit(EventType.DONE);
    }

    private void emit(EventType type) {
        publisher.publish(new PhilosopherEvent(id, type, clock.tick(), Instant.now(wallClock)));
    }
}
