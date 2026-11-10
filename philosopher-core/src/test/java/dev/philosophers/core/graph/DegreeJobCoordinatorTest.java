package dev.philosophers.core.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DegreeJobCoordinatorTest {

    private enum Mode {
        OK,
        DOWN,
        WRONG_TASK
    }

    private static final class FakeWorker implements WorkerClient {
        private final String name;
        private final Mode mode;
        private final AtomicInteger calls = new AtomicInteger();

        FakeWorker(String name, Mode mode) {
            this.name = name;
            this.mode = mode;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public DegreeResult compute(DegreeTask task) throws WorkerException {
            calls.incrementAndGet();
            return switch (mode) {
                case DOWN -> throw new WorkerException(name + " is down");
                case WRONG_TASK ->
                        new DegreeResult(
                                task.jobId(),
                                task.taskId() + 1000,
                                DegreeCalculator.compute(task.edges()));
                case OK ->
                        new DegreeResult(
                                task.jobId(),
                                task.taskId(),
                                DegreeCalculator.compute(task.edges()));
            };
        }
    }

    private static final List<Edge> GRAPH =
            IntStream.range(0, 50).mapToObj(i -> new Edge(i % 10, (i * 7 + 3) % 10)).toList();

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private DegreeJobCoordinator coordinator(WorkerClient... workers) {
        return new DegreeJobCoordinator(List.of(workers), executor, 4, workers.length);
    }

    @Test
    void distributedResultEqualsSequentialResult() throws Exception {
        var result =
                coordinator(
                                new FakeWorker("w1", Mode.OK),
                                new FakeWorker("w2", Mode.OK),
                                new FakeWorker("w3", Mode.OK))
                        .run("job", GRAPH);

        assertThat(result).isEqualTo(DegreeCalculator.compute(GRAPH));
    }

    @Test
    void failedTasksAreRetriedOnAnotherWorker() throws Exception {
        FakeWorker down = new FakeWorker("down", Mode.DOWN);

        var result = coordinator(down, new FakeWorker("ok", Mode.OK)).run("job", GRAPH);

        assertThat(down.calls.get()).isPositive();
        assertThat(result).isEqualTo(DegreeCalculator.compute(GRAPH));
    }

    @Test
    void inconsistentResultIsTreatedAsFailure() throws Exception {
        var result =
                coordinator(new FakeWorker("bad", Mode.WRONG_TASK), new FakeWorker("ok", Mode.OK))
                        .run("job", GRAPH);

        assertThat(result).isEqualTo(DegreeCalculator.compute(GRAPH));
    }

    @Test
    void jobFailsWhenEveryWorkerIsDown() {
        DegreeJobCoordinator coordinator =
                coordinator(new FakeWorker("a", Mode.DOWN), new FakeWorker("b", Mode.DOWN));

        assertThatThrownBy(() -> coordinator.run("job", GRAPH))
                .isInstanceOf(JobFailedException.class);
    }

    @Test
    void emptyGraphGivesEmptyResult() throws Exception {
        assertThat(coordinator(new FakeWorker("w1", Mode.OK)).run("job", List.of())).isEmpty();
    }
}
