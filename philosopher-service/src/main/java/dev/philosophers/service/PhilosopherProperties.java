package dev.philosophers.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration d'un philosophe (préfixe {@code philosopher}).
 *
 * @param id identifiant unique du philosophe
 * @param monitorUrl URL de base du monitor-service
 * @param seed graine des durées aléatoires ; si absente, dérivée de l'identifiant
 */
@ConfigurationProperties(prefix = "philosopher")
public record PhilosopherProperties(
        @DefaultValue("philosopher-1") String id,
        @DefaultValue("http://localhost:8080") String monitorUrl,
        Long seed,
        @DefaultValue("1s") Duration thinkMin,
        @DefaultValue("3s") Duration thinkMax,
        @DefaultValue("1s") Duration eatMin,
        @DefaultValue("2s") Duration eatMax,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout) {

    public PhilosopherProperties {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("philosopher.id must not be blank");
        }
        requireValidRange("think", thinkMin, thinkMax);
        requireValidRange("eat", eatMin, eatMax);
    }

    public long effectiveSeed() {
        return seed != null ? seed : id.hashCode();
    }

    private static void requireValidRange(String name, Duration min, Duration max) {
        if (min.isNegative()) {
            throw new IllegalArgumentException(
                    "philosopher.%s-min must be >= 0 but was %s".formatted(name, min));
        }
        if (max.compareTo(min) < 0) {
            throw new IllegalArgumentException(
                    "philosopher.%s-max (%s) must be >= %s-min (%s)"
                            .formatted(name, max, name, min));
        }
    }
}
