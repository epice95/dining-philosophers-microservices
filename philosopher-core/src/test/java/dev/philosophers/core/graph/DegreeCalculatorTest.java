package dev.philosophers.core.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DegreeCalculatorTest {

    private static final List<Edge> GRAPH =
            List.of(new Edge(1, 2), new Edge(2, 3), new Edge(3, 1), new Edge(3, 4), new Edge(4, 4));

    @Test
    void partitionedResultEqualsSequentialResult() {
        Map<Integer, Integer> expected = DegreeCalculator.compute(GRAPH);

        for (int parts = 1; parts <= 6; parts++) {
            List<Map<Integer, Integer>> partials =
                    EdgePartitioner.split(GRAPH, parts).stream()
                            .map(DegreeCalculator::compute)
                            .toList();
            assertThat(DegreeCalculator.merge(partials)).isEqualTo(expected);
        }
    }

    @Test
    void everyEdgeGoesToExactlyOnePart() {
        List<List<Edge>> parts = EdgePartitioner.split(GRAPH, 3);
        assertThat(parts.stream().mapToInt(List::size).sum()).isEqualTo(GRAPH.size());
    }
}
