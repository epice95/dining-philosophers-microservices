package dev.philosophers.unlocked;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fenêtre de course déterministe : chaque thread qui a vu ses fourchettes libres attend ici que
 * {@code parties} threads l'aient rejoint. Tous ont donc fait la vérification avant que le premier
 * ne réserve : c'est l'entrelacement défavorable, forcé au lieu d'être espéré.
 */
final class BarrierRaceWindow implements RaceWindow {

    private final CyclicBarrier barriere;
    private final List<String> threads = new CopyOnWriteArrayList<>();

    BarrierRaceWindow(int parties) {
        this.barriere = new CyclicBarrier(parties);
    }

    @Override
    public void between(int philosophe) {
        threads.add(Thread.currentThread().getName());
        try {
            barriere.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException("l'autre requête n'est jamais arrivée", e);
        }
    }

    /** Noms des threads passés par la fenêtre. */
    List<String> threads() {
        return List.copyOf(threads);
    }
}
