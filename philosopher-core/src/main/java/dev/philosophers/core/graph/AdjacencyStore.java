package dev.philosophers.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mémoire d'un Worker : pour chaque calcul, la liste d'adjacence de ses nœuds. Thread-safe. */
public final class AdjacencyStore {

    private final Map<String, Map<Integer, List<Integer>>> jobs = new HashMap<>();

    public synchronized void load(String jobId, Collection<Edge> edges) {
        Map<Integer, List<Integer>> adjacency = jobs.computeIfAbsent(jobId, id -> new HashMap<>());
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.from(), node -> new ArrayList<>()).add(edge.to());
        }
    }

    /** Voisins des nœuds donnés, sans doublon. Calcul inconnu : IllegalStateException. */
    public synchronized List<Integer> expand(String jobId, Collection<Integer> nodes) {
        Map<Integer, List<Integer>> adjacency = jobs.get(jobId);
        if (adjacency == null) {
            throw new IllegalStateException("Unknown job " + jobId);
        }
        Set<Integer> neighbors = new LinkedHashSet<>();
        for (Integer node : nodes) {
            List<Integer> successors = adjacency.get(node);
            if (successors != null) {
                neighbors.addAll(successors);
            }
        }
        return new ArrayList<>(neighbors);
    }

    public synchronized void release(String jobId) {
        jobs.remove(jobId);
    }
}
