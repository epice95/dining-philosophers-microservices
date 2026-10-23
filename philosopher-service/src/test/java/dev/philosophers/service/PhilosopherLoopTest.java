package dev.philosophers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
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
}
