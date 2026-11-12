package dev.philosophers.core.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReachabilityCoordinatorTest {

    private static final class InMemoryWorker implements ReachabilityWorkerClient {
        private final String name;
        private final boolean failing;
        private final AdjacencyStore store = new AdjacencyStore();
        private final AtomicInteger releases = new AtomicInteger();

        InMemoryWorker(String name, boolean failing) {
            this.name = name;
            this.failing = failing;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void load(String jobId, List<Edge> edges) throws WorkerException {
            if (failing) {
                throw new WorkerException(name + " is down");
            }
            store.load(jobId, edges);
        }

        @Override
        public List<Integer> expand(String jobId, List<Integer> nodes) throws WorkerException {
            if (failing) {
                throw new WorkerException(name + " is down");
            }
            return store.expand(jobId, nodes);
        }

        @Override
        public void release(String jobId) {
            releases.incrementAndGet();
            store.release(jobId);
        }
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ReachabilityCoordinator coordinator(ReachabilityWorkerClient... workers) {
        return new ReachabilityCoordinator(List.of(workers), executor);
    }

    private static List<ReachabilityWorkerClient> healthy(int count) {
        List<ReachabilityWorkerClient> workers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            workers.add(new InMemoryWorker("w" + i, false));
        }
        return workers;
    }

    private static final List<Edge> CHAIN = List.of(new Edge(1, 2), new Edge(2, 3), new Edge(3, 4));

    @Test
    void chainIsReachableInThreeLevels() throws Exception {
        var result = new ReachabilityCoordinator(healthy(3), executor).run("job", CHAIN, 1, 4);

        assertThat(result.reachable()).isTrue();
        assertThat(result.levels()).isEqualTo(3);
        assertThat(result.visited()).isEqualTo(4);
    }

    @Test
    void directionMatters() throws Exception {
        var result = new ReachabilityCoordinator(healthy(2), executor).run("job", CHAIN, 4, 1);

        assertThat(result.reachable()).isFalse();
        assertThat(result.visited()).isEqualTo(1);
    }

    @Test
    void sourceEqualToTargetIsReachableWithoutTraversal() throws Exception {
        var result = new ReachabilityCoordinator(healthy(2), executor).run("job", CHAIN, 3, 3);

        assertThat(result.reachable()).isTrue();
        assertThat(result.levels()).isZero();
    }

    @Test
    void cyclesTerminate() throws Exception {
        List<Edge> cycle = List.of(new Edge(1, 2), new Edge(2, 1));

        var result = new ReachabilityCoordinator(healthy(2), executor).run("job", cycle, 1, 3);

        assertThat(result.reachable()).isFalse();
        assertThat(result.visited()).isEqualTo(2);
    }

    @Test
    void matchesSequentialBreadthFirstSearch() throws Exception {
        for (long seed = 1; seed <= 20; seed++) {
            List<Edge> graph = RandomGraphGenerator.generate(30, 40, seed);

            var result =
                    new ReachabilityCoordinator(healthy(3), executor)
                            .run("job" + seed, graph, 0, 29);

            assertThat(result.reachable())
                    .as("seed %d", seed)
                    .isEqualTo(sequentialReachable(graph, 0, 29));
        }
    }

    @Test
    void everyWorkerIsReleasedAfterTheJob() throws Exception {
        InMemoryWorker a = new InMemoryWorker("a", false);
        InMemoryWorker b = new InMemoryWorker("b", false);

        coordinator(a, b).run("job", CHAIN, 1, 4);

        assertThat(a.releases.get()).isEqualTo(1);
        assertThat(b.releases.get()).isEqualTo(1);
    }

    @Test
    void jobFailsWhenAWorkerIsDownButOthersAreStillReleased() {
        InMemoryWorker ok = new InMemoryWorker("ok", false);
        InMemoryWorker down = new InMemoryWorker("down", true);
        ReachabilityCoordinator coordinator = coordinator(ok, down);

        assertThatThrownBy(() -> coordinator.run("job", CHAIN, 1, 4))
                .isInstanceOf(JobFailedException.class);
        assertThat(ok.releases.get()).isEqualTo(1);
    }

    private static boolean sequentialReachable(List<Edge> edges, int source, int target) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.from(), n -> new ArrayList<>()).add(edge.to());
        }
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        seen.add(source);
        queue.add(source);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (node == target) {
                return true;
            }
            for (int next : adjacency.getOrDefault(node, List.of())) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
