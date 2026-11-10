package dev.philosophers.service.graph;

import dev.philosophers.core.graph.DegreeJobCoordinator;
import dev.philosophers.core.graph.Edge;
import dev.philosophers.core.graph.JobFailedException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Point d'entrée du Master : lance le calcul des degrés d'un graphe. */
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

    private final DegreeJobCoordinator coordinator;

    public MasterController(DegreeJobCoordinator coordinator) {
        this.coordinator = coordinator;
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
}
