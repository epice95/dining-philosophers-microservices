package dev.philosophers.monitor;

/** Résultat de l'enregistrement d'un événement. */
public enum RecordResult {
    /** Événement nouveau, pris en compte. */
    ACCEPTED,
    /** Événement déjà connu (rejeu), ignoré sans effet. */
    DUPLICATE
}
