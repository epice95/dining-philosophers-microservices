package dev.philosophers.core.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Rôle de Master : découpe les arêtes en tâches, les répartit sur les Workers, relance sur un autre
 * Worker en cas d'échec, puis fusionne les degrés partiels.
 *
 * <p>Chaque tâche ne produit qu'un seul résultat retenu : aucun double comptage possible, même si
 * une tâche a été tentée sur plusieurs Workers.
 */
public final class DegreeJobCoordinator {

    private final List<WorkerClient> workers;
    private final ExecutorService executor;
    private final int tasksPerWorker;
    private final int maxAttempts;

    public DegreeJobCoordinator(
            List<WorkerClient> workers,
            ExecutorService executor,
            int tasksPerWorker,
            int maxAttempts) {
        if (workers.isEmpty()) {
            throw new IllegalArgumentException("at least one worker is required");
        }
        if (tasksPerWorker < 1) {
            throw new IllegalArgumentException("tasksPerWorker must be >= 1");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.workers = List.copyOf(workers);
        this.executor = executor;
        this.tasksPerWorker = tasksPerWorker;
        this.maxAttempts = maxAttempts;
    }

    /** Calcule le degré de chaque nœud du graphe (non orienté) défini par ses arêtes. */
    public Map<Integer, Integer> run(String jobId, List<Edge> edges)
            throws JobFailedException, InterruptedException {
        List<Future<DegreeResult>> futures = new ArrayList<>();
        for (DegreeTask task : buildTasks(jobId, edges)) {
            futures.add(executor.submit(() -> runTask(task)));
        }
        List<Map<Integer, Integer>> partials = new ArrayList<>();
        try {
            for (Future<DegreeResult> future : futures) {
                partials.add(future.get().partialDegrees());
            }
        } catch (ExecutionException e) {
            cancelAll(futures);
            if (e.getCause() instanceof JobFailedException failure) {
                throw failure;
            }
            throw new JobFailedException("Job " + jobId + " failed", e.getCause());
        } catch (InterruptedException e) {
            cancelAll(futures);
            throw e;
        }
        return DegreeCalculator.merge(partials);
    }

    private List<DegreeTask> buildTasks(String jobId, List<Edge> edges) {
        List<DegreeTask> tasks = new ArrayList<>();
        for (List<Edge> part : EdgePartitioner.split(edges, workers.size() * tasksPerWorker)) {
            if (!part.isEmpty()) {
                tasks.add(new DegreeTask(jobId, tasks.size(), part));
            }
        }
        return tasks;
    }

    /** Essaie les Workers à tour de rôle, en partant de taskId % n. */
    private DegreeResult runTask(DegreeTask task) throws JobFailedException {
        WorkerException last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            WorkerClient worker = workers.get((task.taskId() + attempt) % workers.size());
            try {
                DegreeResult result = worker.compute(task);
                if (result.jobId().equals(task.jobId()) && result.taskId() == task.taskId()) {
                    return result;
                }
                last = new WorkerException("Inconsistent result from " + worker.name());
            } catch (WorkerException e) {
                last = e;
            }
        }
        throw new JobFailedException(
                "Task %d of job %s failed after %d attempts"
                        .formatted(task.taskId(), task.jobId(), maxAttempts),
                last);
    }

    private static void cancelAll(List<Future<DegreeResult>> futures) {
        for (Future<DegreeResult> future : futures) {
            future.cancel(true);
        }
    }
}
