package dev.philosophers.core.graph;

import java.util.List;

/** Sous-graphe confié à un Worker. */
public record DegreeTask(String jobId, int taskId, List<Edge> edges) {

    public DegreeTask {
        edges = List.copyOf(edges);
    }
}
