package dev.philosophers.verrou;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Scénario de l'énoncé en HTTP : P0 et P1 envoient leur demande en même temps, Tomcat les traite
 * dans deux threads, mais un seul obtient la fourchette partagée F1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SuperviseurHttpTest {

    @Value("${local.server.port}")
    private int port;

    // HTTP/1.1 : une connexion par requête, donc un thread Tomcat par requête
    private final HttpClient client =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private CompletableFuture<HttpResponse<String>> post(String chemin) {
        HttpRequest requete =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return client.sendAsync(requete, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void deuxVoisinsEnMemeTempsUnSeulMange() throws Exception {
        CompletableFuture<HttpResponse<String>> p0 = post("/forks/0/acquire");
        CompletableFuture<HttpResponse<String>> p1 = post("/forks/1/acquire");

        String reponse0 = p0.get().body();
        String reponse1 = p1.get().body();

        // exactement un "true" et un "false"
        assertThat(reponse0).isNotEqualTo(reponse1);
        assertThat(reponse0).isIn("true", "false");
        assertThat(reponse1).isIn("true", "false");
    }
}
