package dev.philosophers.core;

import java.time.Duration;

/** Attente injectable : permet de tester le cycle sans dormir réellement. */
@FunctionalInterface
public interface Pause {

    /** Implémentation réelle, basée sur {@link Thread#sleep(Duration)}. */
    Pause THREAD = Thread::sleep;

    void sleep(Duration duration) throws InterruptedException;
}
