package dev.philosophers.common.clock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Horloge logique de Lamport, thread-safe.
 *
 * <ul>
 *   <li>Événement local : {@code time = time + 1}</li>
 *   <li>Réception d'un message portant {@code t} : {@code time = max(time, t) + 1}</li>
 * </ul>
 *
 * Garantie : si l'événement a se produit avant b (au sens de Lamport), alors C(a) < C(b).
 * La réciproque est fausse : C(a) < C(b) n'implique pas que a précède causalement b.
 */
public final class LamportClock {

    private final AtomicLong time = new AtomicLong(0);

    /** Événement local : incrémente l'horloge et renvoie la nouvelle valeur. */
    public long tick() {
        return time.incrementAndGet();
    }

    /** Réception d'un message horodaté : applique max(local, remote) + 1 de façon atomique. */
    public long onReceive(long remoteTime) {
        if (remoteTime < 0) {
            throw new IllegalArgumentException("remoteTime must be >= 0 but was " + remoteTime);
        }
        return time.updateAndGet(local -> Math.max(local, remoteTime) + 1);
    }

    /** Valeur courante, sans modification. */
    public long current() {
        return time.get();
    }
}