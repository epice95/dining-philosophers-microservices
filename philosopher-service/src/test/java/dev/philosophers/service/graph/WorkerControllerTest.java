package dev.philosophers.service.graph;

import static org.assertj.core.api.Assertions.assertThat;

import dev.philosophers.core.graph.DegreeResult;
import dev.philosophers.core.graph.DegreeTask;
import dev.philosophers.core.graph.Edge;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkerControllerTest {

    private final WorkerController controller = new WorkerController();

    @Test
    void computesPartialDegreesAndEchoesIdentifiers() {
        DegreeTask task = new DegreeTask("job-1", 7, List.of(new Edge(1, 2), new Edge(2, 3)));

        DegreeResult result = controller.handle(task);

        assertThat(result.jobId()).isEqualTo("job-1");
        assertThat(result.taskId()).isEqualTo(7);
        assertThat(result.partialDegrees()).isEqualTo(Map.of(1, 1, 2, 2, 3, 1));
    }

    @Test
    void emptyTaskGivesEmptyResult() {
        DegreeResult result = controller.handle(new DegreeTask("job-1", 0, List.of()));

        assertThat(result.partialDegrees()).isEmpty();
    }

    @Test
    void sameTaskTwiceGivesSameResult() {
        DegreeTask task = new DegreeTask("job-1", 1, List.of(new Edge(5, 6), new Edge(6, 6)));

        assertThat(controller.handle(task)).isEqualTo(controller.handle(task));
    }
}
