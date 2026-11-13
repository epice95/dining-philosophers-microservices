package dev.philosophers.unlocked;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Faille de la version sans verrou, rendue reproductible. Ce test <strong>documente un
 * défaut</strong> : il passe parce que la section critique n'est pas protégée.
 */
class UnlockedForkTableRaceTest {

    @Test
    void twoNeighboursBothGetTheSharedFork() throws Exception {
        UnlockedForkTable table = new UnlockedForkTable(5, new BarrierRaceWindow(2));
        ExecutorService groupe = Executors.newFixedThreadPool(2);
        try {
            // P0 veut F0 et F1, P1 veut F1 et F2 : F1 est partagée
            Future<Boolean> p0 = groupe.submit(() -> table.acquire(0));
            Future<Boolean> p1 = groupe.submit(() -> table.acquire(1));

            assertThat(p0.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(p1.get(10, TimeUnit.SECONDS)).isTrue(); // devrait être false : course
        } finally {
            groupe.shutdownNow();
        }
    }
}
