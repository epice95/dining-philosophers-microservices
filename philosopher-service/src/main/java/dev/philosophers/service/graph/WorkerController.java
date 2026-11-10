package dev.philosophers.service.graph;

import dev.philosophers.core.graph.DegreeCalculator;
import dev.philosophers.core.graph.DegreeResult;
import dev.philosophers.core.graph.DegreeTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Worker du calcul de degrés : reçoit un sous-graphe, renvoie les degrés partiels.
 *
 * <p>Actif uniquement si {@code philosopher.graph.role=worker}. Sans état : recalculer une même
 * tâche donne le même résultat, ce qui rend la relance par le Master sans danger.
 */
@RestController
@ConditionalOnProperty(name = "philosopher.graph.role", havingValue = "worker")
public class WorkerController {

    @PostMapping("/graph/tasks")
    public DegreeResult handle(@RequestBody DegreeTask task) {
        return new DegreeResult(
                task.jobId(), task.taskId(), DegreeCalculator.compute(task.edges()));
    }
}
