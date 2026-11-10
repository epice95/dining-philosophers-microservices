package dev.philosophers.service.graph;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration du calcul distribué (préfixe {@code philosopher.graph}).
 *
 * @param role {@code none} (défaut), {@code master} ou {@code worker}
 * @param workers URLs de base des Workers, utilisées par le Master
 * @param tasksPerWorker nombre de tâches générées par Worker
 */
@ConfigurationProperties(prefix = "philosopher.graph")
public record GraphProperties(
        @DefaultValue("none") String role,
        @DefaultValue List<String> workers,
        @DefaultValue("4") int tasksPerWorker,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {

    public GraphProperties {
        workers = List.copyOf(workers);
    }
}
