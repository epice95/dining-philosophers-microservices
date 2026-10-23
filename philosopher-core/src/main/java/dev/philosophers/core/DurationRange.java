package dev.philosophers.core;

import java.time.Duration;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Intervalle de durées [min, max], bornes incluses, au pas de la milliseconde.
 *
 * @param min borne basse (>= 0)
 * @param max borne haute (>= min)
 */
public record DurationRange(Duration min, Duration max) {

    public DurationRange {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        if (min.isNegative()) {
            throw new IllegalArgumentException("min must not be negative");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must be <= max");
        }
    }

    Duration sample(RandomGenerator random) {
        return Duration.ofMillis(random.nextLong(min.toMillis(), max.toMillis() + 1));
    }
}
