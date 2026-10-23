package dev.philosophers.service;

import dev.philosophers.common.event.PhilosopherEvent;
import dev.philosophers.core.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Envoie les événements au monitor par HTTP. Ne propage jamais une erreur réseau. */
public final class HttpEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEventPublisher.class);

    private final RestClient client;

    public HttpEventPublisher(RestClient client) {
        this.client = client;
    }

    @Override
    public void publish(PhilosopherEvent event) {
        try {
            client.post()
                    .uri("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            LOG.warn(
                    "Event {} clock={} of {} not delivered: {}",
                    event.type(),
                    event.clock(),
                    event.philosopherId(),
                    e.getMessage());
        }
    }
}
