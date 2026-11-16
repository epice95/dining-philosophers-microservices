package dev.philosophers.verrou;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * P0 (F0, F1) et P1 (F1, F2) se disputent F1 au même instant. Grâce au verrou, un seul des deux
 * doit l'obtenir, à chaque tour. Sans verrou (partie 3.1), certains tours donneraient F1 aux deux.
 */
class TableFourchettesConcurrenceTest {

    @Test
    void unSeulVoisinObtientLaFourchettePartagee() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int tour = 0; tour < 1000; tour++) {
                TableFourchettes table = new TableFourchettes(5);
                CountDownLatch depart = new CountDownLatch(1);

                Future<Boolean> p0 =
                        threads.submit(
                                () -> {
                                    depart.await();
                                    return table.prendre(0);
                                });
                Future<Boolean> p1 =
                        threads.submit(
                                () -> {
                                    depart.await();
                                    return table.prendre(1);
                                });
                depart.countDown(); // les deux threads partent en même temps

                int gagnants = (p0.get() ? 1 : 0) + (p1.get() ? 1 : 0);
                assertThat(gagnants).as("tour %d", tour).isEqualTo(1);
            }
        } finally {
            threads.shutdownNow();
        }
    }
}
