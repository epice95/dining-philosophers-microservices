package dev.philosophers.service.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.philosophers.core.graph.Edge;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ReachabilityWorkerControllerTest {

    private final ReachabilityWorkerController controller = new ReachabilityWorkerController();

    @Test
    void expandReturnsSuccessorsOfLoadedEdges() {
        controller.load(
                "j1",
                new ReachabilityWorkerController.LoadRequest(
                        List.of(new Edge(1, 2), new Edge(1, 3), new Edge(2, 4))));

        var response =
                controller.expand("j1", new ReachabilityWorkerController.ExpandRequest(List.of(1)));

        assertThat(response.neighbors()).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void unknownJobGivesConflict() {
        var request = new ReachabilityWorkerController.ExpandRequest(List.of(1));

        assertThatThrownBy(() -> controller.expand("missing", request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void releasedJobIsForgotten() {
        controller.load(
                "j1", new ReachabilityWorkerController.LoadRequest(List.of(new Edge(1, 2))));
        controller.release("j1");
        var request = new ReachabilityWorkerController.ExpandRequest(List.of(1));

        assertThatThrownBy(() -> controller.expand("j1", request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
