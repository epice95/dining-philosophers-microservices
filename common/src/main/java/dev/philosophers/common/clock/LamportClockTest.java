package dev.philosophers.common.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LamportClockTest {

    @Test
    void initialValueIsZero() {
        assertEquals(0, new LamportClock().current());
    }

    @Test
    void tickIsStrictlyIncreasing() {
        LamportClock clock = new LamportClock();
        assertEquals(1, clock.tick());
        assertEquals(2, clock.tick());
        assertEquals(3, clock.tick());
        assertEquals(3, clock.current());
    }

    @Test
    void receiveWithLargerRemoteJumpsAhead() {
        LamportClock clock = new LamportClock();
        clock.tick();
        clock.tick();
        clock.tick(); // local = 3
        assertEquals(11, clock.onReceive(10));
    }

    @Test
    void receiveWithSmallerRemoteStillIncrements() {
        LamportClock clock = new LamportClock();
        for (int i = 0; i < 5; i++) {
            clock.tick(); // local = 5
        }
        assertEquals(6, clock.onReceive(2));
    }

    @Test
    void receiveRejectsNegativeTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new LamportClock().onReceive(-1));
    }

    @Test
    void concurrentTicksNeverLoseOrDuplicateValues() throws Exception {
        int threads = 8;
        int ticksPerThread = 10_000;
        LamportClock clock = new LamportClock();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<long[]>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    long[] values = new long[ticksPerThread];
                    ready.countDown();
                    start.await(); // tous les threads partent en même temps
                    for (int i = 0; i < ticksPerThread; i++) {
                        values[i] = clock.tick();
                    }
                    return values;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Set<Long> seen = new HashSet<>();
            for (Future<long[]> future : futures) {
                for (long value : future.get(30, TimeUnit.SECONDS)) {
                    assertTrue(seen.add(value), "valeur dupliquée : " + value);
                }
            }
            assertEquals(threads * ticksPerThread, seen.size());
            assertEquals(threads * ticksPerThread, clock.current());
        } finally {
            pool.shutdownNow();
        }
    }
}