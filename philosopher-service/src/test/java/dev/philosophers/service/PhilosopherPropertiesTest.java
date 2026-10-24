package dev.philosophers.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PhilosopherPropertiesTest {

    private static PhilosopherProperties props(
            String id, Duration thinkMin, Duration thinkMax, Duration eatMin, Duration eatMax) {
        return new PhilosopherProperties(
                id,
                "http://localhost:8080",
                null,
                thinkMin,
                thinkMax,
                eatMin,
                eatMax,
                Duration.ofSeconds(2),
                Duration.ofSeconds(3));
    }

    @Test
    void acceptsValidRanges() {
        PhilosopherProperties p =
                props(
                        "p1",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2));
        assertEquals("p1", p.id());
    }

    @Test
    void rejectsThinkMinGreaterThanThinkMax() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        props(
                                "p1",
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2)));
    }

    @Test
    void rejectsEatMinGreaterThanEatMax() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        props(
                                "p1",
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(4),
                                Duration.ofSeconds(2)));
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        props(
                                "p1",
                                Duration.ofSeconds(-1),
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2)));
    }

    @Test
    void rejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        props(
                                " ",
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2)));
    }

    @Test
    void seedDefaultsToIdHash() {
        PhilosopherProperties p =
                props(
                        "p1",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2));
        assertEquals("p1".hashCode(), p.effectiveSeed());
    }
}
