package dev.philosophers.core.graph;

import java.util.ArrayList;
import java.util.List;

/** Découpe un ensemble d'arêtes en parties disjointes : chaque arête va dans une seule partie. */
public final class EdgePartitioner {

    private EdgePartitioner() {}

    public static List<List<Edge>> split(List<Edge> edges, int parts) {
        if (parts < 1) {
            throw new IllegalArgumentException("parts must be >= 1 but was " + parts);
        }
        List<List<Edge>> result = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            result.add(new ArrayList<>());
        }
        for (int i = 0; i < edges.size(); i++) {
            result.get(i % parts).add(edges.get(i));
        }
        return result;
    }
}
