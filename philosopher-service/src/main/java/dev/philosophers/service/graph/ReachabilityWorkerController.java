package dev.philosophers.service.graph;

import dev.philosophers.core.graph.AdjacencyStore;
import dev.philosophers.core.graph.Edge;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Worker du calcul d'accessibilité : garde en mémoire les arêtes dont il est propriétaire et
 * renvoie les voisins des nœuds demandés. Actif uniquement si {@code
 * philosopher.graph.role=worker}.
 */
@RestController
@ConditionalOnProperty(name = "philosopher.graph.role", havingValue = "worker")
public class ReachabilityWorkerController {

    /** Arêtes confiées au Worker. */
    public record LoadRequest(List<Edge> edges) {
        public LoadRequest {
            edges = edges == null ? List.of() : List.copyOf(edges);
        }
    }

    /** Nœuds de la frontière à développer. */
    public record ExpandRequest(List<Integer> nodes) {
        public ExpandRequest {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
        }
    }

    /** Voisins découverts. */
    public record ExpandResponse(List<Integer> neighbors) {
        public ExpandResponse {
            neighbors = neighbors == null ? List.of() : List.copyOf(neighbors);
        }
    }

    private final AdjacencyStore store = new AdjacencyStore();

    @PostMapping("/graph/reach/{jobId}/load")
    public void load(@PathVariable("jobId") String jobId, @RequestBody LoadRequest request) {
        store.load(jobId, request.edges());
    }

    @PostMapping("/graph/reach/{jobId}/expand")
    public ExpandResponse expand(
            @PathVariable("jobId") String jobId, @RequestBody ExpandRequest request) {
        try {
            return new ExpandResponse(store.expand(jobId, request.nodes()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @DeleteMapping("/graph/reach/{jobId}")
    public void release(@PathVariable("jobId") String jobId) {
        store.release(jobId);
    }
}
