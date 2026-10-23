package dev.philosophers.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/** Durées aléatoires reproductibles : la même graine donne la même séquence. */
public final class RandomDurationSource implements DurationSource {

    private final Random random;
    private final DurationRange think;
    private final DurationRange eat;

    public RandomDurationSource(long seed, DurationRange think, DurationRange eat) {
        this.random = new Random(seed);
        this.think = Objects.requireNonNull(think, "think");
        this.eat = Objects.requireNonNull(eat, "eat");
    }

    @Override
    public Duration thinkTime() {
        return think.sample(random);
    }

    @Override
    public Duration eatTime() {
        return eat.sample(random);
    }
}
