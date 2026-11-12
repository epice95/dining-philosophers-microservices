package dev.philosophers.core.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rôle de Master pour l'accessibilité : parcours en largeur parallèle, par niveaux.
 *
 * <p>Les nœuds sont partitionnés entre les Workers (propriétaire = nœud modulo nombre de Workers).
 * Le Master garde seul l'ensemble des nœuds visités, donc aucun nœud n'est traité deux fois même
 * s'il est découvert par plusieurs Workers.
 */
public final class ReachabilityCoordinator {

    /**
     * Résultat : accessibilité, nombre de niveaux parcourus, nœuds visités, échecs de nettoyage.
     */
    public record Result(boolean reachable, int levels, int visited, int releaseFailures) {}

    private record Traversal(boolean reachable, int levels, int visited) {}

    private final List<ReachabilityWorkerClient> workers;
    private final ExecutorService executor;

    public ReachabilityCoordinator(
            List<ReachabilityWorkerClient> workers, ExecutorService executor) {
        if (workers.isEmpty()) {
            throw new IllegalArgumentException("at least one worker is required");
        }
        this.workers = List.copyOf(workers);
        this.executor = executor;
    }

    /**
     * Existe-t-il un chemin de source vers target dans le graphe orienté défini par les arêtes ?
     */
    public Result run(String jobId, List<Edge> edges, int source, int target)
            throws JobFailedException, InterruptedException {
        AtomicInteger releaseFailures = new AtomicInteger();
        Traversal traversal;
        try {
            loadPartitions(jobId, edges);
            traversal = traverse(jobId, source, target);
        } finally {
            releaseAll(jobId, releaseFailures);
        }
        return new Result(
                traversal.reachable(),
                traversal.levels(),
                traversal.visited(),
                releaseFailures.get());
    }

    private int ownerOf(int node) {
        return Math.floorMod(node, workers.size());
    }

    private void loadPartitions(String jobId, List<Edge> edges)
            throws JobFailedException, InterruptedException {
        List<List<Edge>> parts = new ArrayList<>();
        for (int i = 0; i < workers.size(); i++) {
            parts.add(new ArrayList<>());
        }
        for (Edge edge : edges) {
            parts.get(ownerOf(edge.from())).add(edge);
        }
        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int i = 0; i < workers.size(); i++) {
            ReachabilityWorkerClient worker = workers.get(i);
            List<Edge> part = parts.get(i);
            calls.add(
                    () -> {
                        worker.load(jobId, part);
                        return Boolean.TRUE;
                    });
        }
        runAll(calls);
    }

    private Traversal traverse(String jobId, int source, int target)
            throws JobFailedException, InterruptedException {
        Set<Integer> visited = new HashSet<>();
        visited.add(source);
        Set<Integer> frontier = new HashSet<>(visited);
        int levels = 0;
        while (!visited.contains(target) && !frontier.isEmpty()) {
            Map<Integer, List<Integer>> byOwner = new HashMap<>();
            for (Integer node : frontier) {
                byOwner.computeIfAbsent(ownerOf(node), owner -> new ArrayList<>()).add(node);
            }
            List<Callable<List<Integer>>> calls = new ArrayList<>();
            for (Map.Entry<Integer, List<Integer>> entry : byOwner.entrySet()) {
                ReachabilityWorkerClient worker = workers.get(entry.getKey());
                List<Integer> nodes = entry.getValue();
                calls.add(() -> worker.expand(jobId, nodes));
            }
            Set<Integer> next = new HashSet<>();
            for (List<Integer> neighbors : runAll(calls)) {
                for (Integer neighbor : neighbors) {
                    if (visited.add(neighbor)) {
                        next.add(neighbor);
                    }
                }
            }
            if (!next.isEmpty()) {
                levels++;
            }
            frontier = next;
        }
        return new Traversal(visited.contains(target), levels, visited.size());
    }

    /** Nettoyage au mieux : un échec est compté, mais n'empêche pas de nettoyer les autres. */
    private void releaseAll(String jobId, AtomicInteger failures) {
        for (ReachabilityWorkerClient worker : workers) {
            try {
                worker.release(jobId);
            } catch (WorkerException e) {
                failures.incrementAndGet();
            }
        }
    }

    private <T> List<T> runAll(List<Callable<T>> calls)
            throws JobFailedException, InterruptedException {
        List<Future<T>> futures = executor.invokeAll(calls);
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                throw new JobFailedException(
                        "A worker failed: " + e.getCause().getMessage(), e.getCause());
            }
        }
        return results;
    }
}
