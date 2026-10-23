package dev.philosophers.core;

import java.time.Duration;

/** Fournit les durées de réflexion et de repas d'un philosophe. */
public interface DurationSource {

    Duration thinkTime();

    Duration eatTime();
}
