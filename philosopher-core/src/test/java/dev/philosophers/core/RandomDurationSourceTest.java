package dev.philosophers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RandomDurationSourceTest {

    private static final DurationRange THINK =
            new DurationRange(Duration.ofMillis(10), Duration.ofMillis(20));
    private static final DurationRange EAT =
            new DurationRange(Duration.ofMillis(5), Duration.ofMillis(7));

    @Test
    void valuesStayWithinConfiguredRanges() {
        RandomDurationSource source = new RandomDurationSource(42L, THINK, EAT);
        for (int i = 0; i < 1000; i++) {
            long think = source.thinkTime().toMillis();
            long eat = source.eatTime().toMillis();
            assertTrue(think >= 10 && think <= 20, "think out of range: " + think);
            assertTrue(eat >= 5 && eat <= 7, "eat out of range: " + eat);
        }
    }

    @Test
    void sameSeedGivesSameSequence() {
        RandomDurationSource a = new RandomDurationSource(7L, THINK, EAT);
        RandomDurationSource b = new RandomDurationSource(7L, THINK, EAT);
        for (int i = 0; i < 100; i++) {
            assertEquals(a.thinkTime(), b.thinkTime());
            assertEquals(a.eatTime(), b.eatTime());
        }
    }
}
