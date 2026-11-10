package dev.philosophers.service.graph;

import dev.philosophers.core.graph.DegreeJobCoordinator;
import dev.philosophers.core.graph.WorkerClient;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Assemble le Master. Actif uniquement si {@code philosopher.graph.role=master}. */
@Configuration
@ConditionalOnProperty(name = "philosopher.graph.role", havingValue = "master")
@EnableConfigurationProperties(GraphProperties.class)
class MasterConfig {

    @Bean
    ExecutorService graphExecutor(GraphProperties props) {
        return Executors.newFixedThreadPool(Math.max(1, props.workers().size()));
    }

    @Bean
    DegreeJobCoordinator degreeJobCoordinator(
            GraphProperties props, ExecutorService graphExecutor) {
        if (props.workers().isEmpty()) {
            throw new IllegalStateException(
                    "philosopher.graph.workers must list at least one worker URL");
        }
        List<WorkerClient> workers =
                props.workers().stream()
                        .map(
                                url ->
                                        (WorkerClient)
                                                HttpWorkerClient.create(
                                                        url,
                                                        props.connectTimeout(),
                                                        props.readTimeout()))
                        .toList();
        return new DegreeJobCoordinator(
                workers, graphExecutor, props.tasksPerWorker(), workers.size());
    }
}
