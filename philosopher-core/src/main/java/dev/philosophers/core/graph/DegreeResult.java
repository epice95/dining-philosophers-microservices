package dev.philosophers.core.graph;

import java.util.Map;

/** Degrés partiels calculés par un Worker pour une tâche. */
public record DegreeResult(String jobId, int taskId, Map<Integer, Integer> partialDegrees) {

    public DegreeResult {
        partialDegrees = Map.copyOf(partialDegrees);
    }
}
