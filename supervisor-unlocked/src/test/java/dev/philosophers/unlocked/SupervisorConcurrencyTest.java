package dev.philosophers.unlocked;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Scénario de l'énoncé, de bout en bout : deux philosophes voisins envoient leur demande en même
 * temps, le serveur web les confie à deux threads différents, et la section critique non protégée
 * attribue la fourchette partagée aux deux.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SupervisorConcurrencyTest.RaceConfig.class)
class SupervisorConcurrencyTest {

    @TestConfiguration
    static class RaceConfig {
        @Bean
        @Primary
        BarrierRaceWindow barrierRaceWindow() {
            return new BarrierRaceWindow(2);
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired private BarrierRaceWindow fenetre;

    // HTTP/1.1 : une connexion par requête en vol, donc un thread Tomcat chacune
    private final HttpClient client =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private CompletableFuture<HttpResponse<String>> post(String chemin) {
        HttpRequest requete =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return client.sendAsync(requete, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void simultaneousRequestsFromNeighboursBothGetTheSharedFork() throws Exception {
        CompletableFuture<HttpResponse<String>> p0 = post("/forks/0/acquire");
        CompletableFuture<HttpResponse<String>> p1 = post("/forks/1/acquire");

        // Deux threads du serveur ont exécuté la section critique en même temps
        assertThat(p0.get().body()).isEqualTo("true");
        assertThat(p1.get().body()).isEqualTo("true"); // devrait être false : course
        assertThat(fenetre.threads()).hasSize(2).doesNotHaveDuplicates();
    }
}
