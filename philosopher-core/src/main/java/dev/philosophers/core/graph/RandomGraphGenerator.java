package dev.philosophers.core.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Génère un graphe aléatoire reproductible (même graine, même graphe). */
public final class RandomGraphGenerator {

    private RandomGraphGenerator() {}

    public static List<Edge> generate(int nodes, int edges, long seed) {
        if (nodes < 1) {
            throw new IllegalArgumentException("nodes must be >= 1 but was " + nodes);
        }
        if (edges < 0) {
            throw new IllegalArgumentException("edges must be >= 0 but was " + edges);
        }
        Random random = new Random(seed);
        List<Edge> result = new ArrayList<>(edges);
        for (int i = 0; i < edges; i++) {
            result.add(new Edge(random.nextInt(nodes), random.nextInt(nodes)));
        }
        return result;
    }
}
