package dev.philosophers.service.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import dev.philosophers.core.graph.DegreeCalculator;
import dev.philosophers.core.graph.DegreeJobCoordinator;
import dev.philosophers.core.graph.DegreeResult;
import dev.philosophers.core.graph.DegreeTask;
import dev.philosophers.core.graph.Edge;
import dev.philosophers.core.graph.RandomGraphGenerator;
import dev.philosophers.core.graph.WorkerClient;
import dev.philosophers.core.graph.WorkerException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Un Worker qui répond trop tard : le timeout coupe l'appel et la tâche est relancée ailleurs. */
class SlowWorkerTest {

    private static final long SERVER_DELAY_MS = 1_500;
    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

    private HttpServer slowServer;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @BeforeEach
    void startSlowServer() throws IOException {
        slowServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        slowServer.createContext(
                "/graph/tasks",
                exchange -> {
                    try {
                        Thread.sleep(SERVER_DELAY_MS);
                        exchange.sendResponseHeaders(200, -1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (IOException e) {
                        // le client a déjà fermé la connexion : c'est le comportement attendu
                    } finally {
                        exchange.close();
                    }
                });
        slowServer.start();
    }

    @AfterEach
    void tearDown() {
        slowServer.stop(0);
        executor.shutdownNow();
    }

    private HttpWorkerClient slowClient() {
        String url = "http://localhost:" + slowServer.getAddress().getPort();
        return HttpWorkerClient.create(url, Duration.ofSeconds(1), READ_TIMEOUT);
    }

    private static WorkerClient healthyWorker() {
        return new WorkerClient() {
            @Override
            public String name() {
                return "healthy";
            }

            @Override
            public DegreeResult compute(DegreeTask task) {
                return new DegreeResult(
                        task.jobId(), task.taskId(), DegreeCalculator.compute(task.edges()));
            }
        };
    }

    @Test
    void slowWorkerIsCutOffByTheReadTimeout() {
        HttpWorkerClient client = slowClient();
        DegreeTask task = new DegreeTask("j1", 0, List.of(new Edge(1, 2)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.compute(task)).isInstanceOf(WorkerException.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(SERVER_DELAY_MS);
    }

    @Test
    void tasksOfASlowWorkerAreRetriedAndTheTotalStaysExact() throws Exception {
        List<Edge> graph = RandomGraphGenerator.generate(20, 100, 7);
        DegreeJobCoordinator coordinator =
                new DegreeJobCoordinator(List.of(slowClient(), healthyWorker()), executor, 4, 2);

        var result = coordinator.run("job", graph);

        assertThat(result).isEqualTo(DegreeCalculator.compute(graph));
        assertThat(result.values().stream().mapToInt(Integer::intValue).sum())
                .isEqualTo(2 * graph.size());
    }
}
