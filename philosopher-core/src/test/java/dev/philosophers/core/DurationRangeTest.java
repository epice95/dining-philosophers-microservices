package dev.philosophers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationRangeTest {

    @Test
    void acceptsEqualBounds() {
        DurationRange range = new DurationRange(Duration.ofMillis(5), Duration.ofMillis(5));
        assertEquals(Duration.ofMillis(5), range.min());
        assertEquals(Duration.ofMillis(5), range.max());
    }

    @Test
    void rejectsNegativeMin() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DurationRange(Duration.ofMillis(-1), Duration.ofMillis(5)));
    }

    @Test
    void rejectsMinGreaterThanMax() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DurationRange(Duration.ofMillis(10), Duration.ofMillis(5)));
    }
}
