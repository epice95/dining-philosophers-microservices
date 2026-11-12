package dev.philosophers.service.graph;

import dev.philosophers.core.graph.DegreeJobCoordinator;
import dev.philosophers.core.graph.Edge;
import dev.philosophers.core.graph.JobFailedException;
import dev.philosophers.core.graph.RandomGraphGenerator;
import dev.philosophers.core.graph.ReachabilityCoordinator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Point d'entrée du Master : degrés des nœuds et accessibilité dans un graphe. */
@RestController
@ConditionalOnProperty(name = "philosopher.graph.role", havingValue = "master")
public class MasterController {

    /** Corps de la requête : les arêtes du graphe. */
    public record JobRequest(List<Edge> edges) {
        public JobRequest {
            edges = edges == null ? List.of() : List.copyOf(edges);
        }
    }

    /** Réponse : degré de chaque nœud et durée du calcul. */
    public record JobResponse(String jobId, Map<Integer, Integer> degrees, long elapsedMillis) {
        public JobResponse {
            degrees = Map.copyOf(degrees);
        }
    }

    /** Résultat d'un benchmark : la somme des degrés doit valoir 2 x edges (contrôle). */
    public record BenchmarkResponse(int nodes, int edges, long elapsedMillis, long degreeSum) {}

    /** Réponse d'un calcul d'accessibilité. */
    public record ReachResponse(
            String jobId,
            boolean reachable,
            int levels,
            int visited,
            int releaseFailures,
            long elapsedMillis) {}

    private final DegreeJobCoordinator coordinator;
    private final ReachabilityCoordinator reachability;

    public MasterController(
            DegreeJobCoordinator coordinator, ReachabilityCoordinator reachability) {
        this.coordinator = coordinator;
        this.reachability = reachability;
    }

    @PostMapping("/graph/jobs")
    public JobResponse run(@RequestBody JobRequest request) {
        String jobId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            Map<Integer, Integer> degrees = coordinator.run(jobId, request.edges());
            return new JobResponse(jobId, degrees, (System.nanoTime() - start) / 1_000_000);
        } catch (JobFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Job interrupted", e);
        }
    }

    /** Génère un graphe aléatoire reproductible et en calcule les degrés : sert au benchmark. */
    @PostMapping("/graph/benchmark")
    public BenchmarkResponse benchmark(
            @RequestParam("nodes") int nodes,
            @RequestParam("edges") int edges,
            @RequestParam(name = "seed", defaultValue = "42") long seed) {
        List<Edge> graph = RandomGraphGenerator.generate(nodes, edges, seed);
        String jobId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            Map<Integer, Integer> degrees = coordinator.run(jobId, graph);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            long total = degrees.values().stream().mapToLong(Integer::longValue).sum();
            return new BenchmarkResponse(nodes, edges, elapsed, total);
        } catch (JobFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Job interrupted", e);
        }
    }

    /** Existe-t-il un chemin de source vers target dans le graphe orienté envoyé en corps ? */
    @PostMapping("/graph/reach")
    public ReachResponse reach(
            @RequestParam("source") int source,
            @RequestParam("target") int target,
            @RequestBody JobRequest request) {
        return reachability(request.edges(), source, target);
    }

    /** Même calcul sur un graphe aléatoire reproductible : sert au benchmark. */
    @PostMapping("/graph/reach/benchmark")
    public ReachResponse reachBenchmark(
            @RequestParam("nodes") int nodes,
            @RequestParam("edges") int edges,
            @RequestParam(name = "seed", defaultValue = "42") long seed,
            @RequestParam(name = "source", defaultValue = "0") int source,
            @RequestParam("target") int target) {
        return reachability(RandomGraphGenerator.generate(nodes, edges, seed), source, target);
    }

    private ReachResponse reachability(List<Edge> edges, int source, int target) {
        String jobId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        try {
            ReachabilityCoordinator.Result result = reachability.run(jobId, edges, source, target);
            return new ReachResponse(
                    jobId,
                    result.reachable(),
                    result.levels(),
                    result.visited(),
                    result.releaseFailures(),
                    (System.nanoTime() - start) / 1_000_000);
        } catch (JobFailedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Job interrupted", e);
        }
    }
}
