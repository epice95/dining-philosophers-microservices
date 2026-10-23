package dev.philosophers.core;

import dev.philosophers.common.event.PhilosopherEvent;

/** Port de sortie : envoie un événement au monitor (HTTP aujourd'hui, Kafka plus tard). */
public interface EventPublisher {

    void publish(PhilosopherEvent event);
}
