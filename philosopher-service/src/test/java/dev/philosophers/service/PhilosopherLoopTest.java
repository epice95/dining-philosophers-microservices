package dev.philosophers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import dev.philosophers.core.PhilosopherCycle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PhilosopherLoopTest {

    @Test
    void repeatsCyclesThenStopsOnInterrupt() throws Exception {
        PhilosopherCycle cycle = mock(PhilosopherCycle.class);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch twoCycles = new CountDownLatch(2);
        doAnswer(
                        inv -> {
                            calls.incrementAndGet();
                            twoCycles.countDown();
                            if (twoCycles.getCount() == 0) {
                                new CountDownLatch(1).await(); // bloque jusqu'à l'interruption
                            }
                            return null;
                        })
                .when(cycle)
                .runOnce();

        PhilosopherLoop loop = new PhilosopherLoop(cycle);
        loop.start();

        assertThat(twoCycles.await(5, TimeUnit.SECONDS)).isTrue();
        loop.stop();

        assertThat(loop.isRunning()).isFalse();
        assertThat(calls.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void continuesAfterUnexpectedError() throws Exception {
        PhilosopherCycle cycle = mock(PhilosopherCycle.class);
        CountDownLatch secondCall = new CountDownLatch(1);

        // 1er appel : erreur ; 2e appel : on signale, puis on bloque jusqu'à l'interruption
        doThrow(new IllegalStateException("boom"))
                .doAnswer(
                        inv -> {
                            secondCall.countDown();
                            new CountDownLatch(1).await();
                            return null;
                        })
                .when(cycle)
                .runOnce();

        PhilosopherLoop loop = new PhilosopherLoop(cycle);
        loop.start();
        try {
            // le backoff après l'erreur dure 1 s : on garde une marge large
            assertThat(secondCall.await(5, TimeUnit.SECONDS))
                    .as("la boucle doit rappeler runOnce après l'erreur")
                    .isTrue();
            assertThat(loop.isRunning()).as("la boucle doit toujours tourner").isTrue();
        } finally {
            loop.stop();
        }

        assertThat(loop.isRunning()).isFalse();
    }

    @Test
    void stopInterruptsRunningCycle() throws Exception {
        PhilosopherCycle cycle = mock(PhilosopherCycle.class);
        CountDownLatch started = new CountDownLatch(1);

        doAnswer(
                        inv -> {
                            started.countDown();
                            new CountDownLatch(1).await(); // bloque jusqu'à l'interruption
                            return null;
                        })
                .when(cycle)
                .runOnce();

        PhilosopherLoop loop = new PhilosopherLoop(cycle);
        loop.start();
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        loop.stop();

        assertThat(loop.isRunning()).isFalse();
    }
}
