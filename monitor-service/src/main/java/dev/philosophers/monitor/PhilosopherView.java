package dev.philosophers.monitor;

import java.util.List;

/**
 * Vue instantanée d'un philosophe, exposée par l'API.
 *
 * @param id identifiant du philosophe
 * @param state état déduit de l'événement d'horloge la plus élevée
 * @param lastClock horloge de cet événement
 * @param lastEventAt instant physique (ISO-8601) de cet événement
 * @param eventCount nombre d'événements distincts reçus
 * @param missingClocks horloges manquantes (trous), tronquées à 100 valeurs
 */
public record PhilosopherView(
        String id,
        PhilosopherState state,
        long lastClock,
        String lastEventAt,
        long eventCount,
        List<Long> missingClocks) {

    public PhilosopherView {
        missingClocks = List.copyOf(missingClocks);
    }
}
