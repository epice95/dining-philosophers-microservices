package dev.philosophers.service;

import dev.philosophers.core.PhilosopherCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/** Relance le cycle du philosophe jusqu'à l'arrêt de l'application. */
public final class PhilosopherLoop implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(PhilosopherLoop.class);
    private static final long JOIN_TIMEOUT_MS = 5_000;
    private static final long ERROR_BACKOFF_MS = 1_000;

    private final PhilosopherCycle cycle;
    private volatile Thread thread;

    public PhilosopherLoop(PhilosopherCycle cycle) {
        this.cycle = cycle;
    }

    @Override
    public synchronized void start() {
        if (isRunning()) {
            return;
        }
        thread = new Thread(this::loop, "philosopher-loop");
        thread.start();
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                cycle.runOnce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                LOG.error("Philosopher cycle failed, retrying in {} ms", ERROR_BACKOFF_MS, e);
                backoff();
            }
        }
    }

    /** Petite pause après une erreur ; conserve le signal d'interruption s'il arrive. */
    private static void backoff() {
        try {
            Thread.sleep(ERROR_BACKOFF_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        Thread t = thread;
        if (t == null) {
            return;
        }
        t.interrupt();
        try {
            t.join(JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        Thread t = thread;
        return t != null && t.isAlive();
    }
}
