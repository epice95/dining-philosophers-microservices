package dev.philosophers.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import dev.philosophers.common.event.EventType;
import dev.philosophers.common.event.PhilosopherEvent;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpEventPublisherTest {

    private static final PhilosopherEvent EVENT =
            new PhilosopherEvent("p1", EventType.HUNGRY, 1, Instant.parse("2026-10-23T10:00:00Z"));

    private MockRestServiceServer server;
    private HttpEventPublisher publisher;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://monitor");
        server = MockRestServiceServer.bindTo(builder).build();
        publisher = new HttpEventPublisher(builder.build());
    }

    @Test
    void postsEventAsJsonToEventsEndpoint() {
        server.expect(requestTo("http://monitor/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"philosopherId\":\"p1\"")))
                .andRespond(withStatus(HttpStatus.CREATED));

        publisher.publish(EVENT);

        server.verify();
    }

    @Test
    void doesNotThrowOnConflict() {
        server.expect(requestTo("http://monitor/events"))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertThatCode(() -> publisher.publish(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void doesNotThrowOnServerError() {
        server.expect(requestTo("http://monitor/events"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatCode(() -> publisher.publish(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void doesNotThrowWhenMonitorIsUnreachable() {
        server.expect(requestTo("http://monitor/events"))
                .andRespond(
                        request -> {
                            throw new IOException("connection refused");
                        });
        assertThatCode(() -> publisher.publish(EVENT)).doesNotThrowAnyException();
    }
}
