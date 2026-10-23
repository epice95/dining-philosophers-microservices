package dev.philosophers.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitorApiTest {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private static String json(String id, String type, long clock) {
        return """
        {"philosopherId":"%s","type":"%s","clock":%d,"emittedAt":"2026-10-22T10:00:00Z"}"""
                .formatted(id, type, clock);
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/events"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void newEventIsCreatedAndVisibleInState() throws Exception {
        assertEquals(201, post(json("it-state", "EATING", 1)).statusCode());

        HttpResponse<String> state = get("/api/state");
        assertEquals(200, state.statusCode());
        assertTrue(state.body().contains("it-state"));
        assertTrue(state.body().contains("EATING"));
    }

    @Test
    void replayedEventIsAcknowledgedWithoutDuplication() throws Exception {
        assertEquals(201, post(json("it-replay", "HUNGRY", 1)).statusCode());
        assertEquals(200, post(json("it-replay", "HUNGRY", 1)).statusCode());

        String history = get("/api/philosophers/it-replay/history").body();
        assertEquals(1, history.split("HUNGRY", -1).length - 1);
    }

    @Test
    void conflictingEventIsRejectedWith409() throws Exception {
        assertEquals(201, post(json("it-conflict", "HUNGRY", 1)).statusCode());
        assertEquals(409, post(json("it-conflict", "EATING", 1)).statusCode());
    }

    @Test
    void invalidEventIsRejectedWith400() throws Exception {
        assertEquals(400, post(json("it-invalid", "HUNGRY", 0)).statusCode());
        assertEquals(400, post(json(" ", "HUNGRY", 1)).statusCode());
        assertEquals(400, post(json("it-invalid", "SLEEPING", 1)).statusCode());
    }

    @Test
    void malformedJsonIsRejectedWith400() throws Exception {
        assertEquals(400, post("{not json").statusCode());
    }

    @Test
    void historyReturnsEventsSortedByClock() throws Exception {
        post(json("it-order", "EATING", 2));
        post(json("it-order", "HUNGRY", 1));

        String body = get("/api/philosophers/it-order/history").body();
        int first = body.indexOf("\"clock\":1");
        int second = body.indexOf("\"clock\":2");
        assertTrue(first >= 0 && second >= 0 && first < second, body);
    }

    @Test
    void unknownPhilosopherHistoryIsNotFound() throws Exception {
        assertEquals(404, get("/api/philosophers/ghost-philosopher/history").statusCode());
    }

    @Test
    void dashboardIsServedAtRoot() throws Exception {
        HttpResponse<String> page = get("/");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("philosophes"));
    }
}
