package dev.philosophers.service;

import dev.philosophers.core.PhilosopherCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/** Relance le cycle du philosophe jusqu'à l'arrêt de l'application. */
public final class PhilosopherLoop implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(PhilosopherLoop.class);
    private static final long JOIN_TIMEOUT_MS = 5_000;

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
        try {
            while (!Thread.currentThread().isInterrupted()) {
                cycle.runOnce();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            LOG.error("Philosopher loop stopped by unexpected error", e);
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
