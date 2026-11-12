package dev.philosophers.core.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class RandomGraphGeneratorTest {

    @Test
    void sameSeedGivesSameGraph() {
        assertThat(RandomGraphGenerator.generate(100, 500, 42))
                .isEqualTo(RandomGraphGenerator.generate(100, 500, 42));
    }

    @Test
    void hasRequestedSizeAndNodesInRange() {
        List<Edge> graph = RandomGraphGenerator.generate(10, 200, 1);

        assertThat(graph).hasSize(200);
        assertThat(graph)
                .allSatisfy(
                        e -> {
                            assertThat(e.from()).isBetween(0, 9);
                            assertThat(e.to()).isBetween(0, 9);
                        });
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> RandomGraphGenerator.generate(0, 10, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RandomGraphGenerator.generate(10, -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
