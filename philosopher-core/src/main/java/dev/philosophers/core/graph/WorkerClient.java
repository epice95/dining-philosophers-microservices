package dev.philosophers.core.graph;

/** Accès à un Worker. L'implémentation HTTP est dans philosopher-service. */
public interface WorkerClient {

    /** Nom lisible du Worker, pour les logs et les messages d'erreur. */
    String name();

    /** Calcule les degrés partiels d'une tâche. */
    DegreeResult compute(DegreeTask task) throws WorkerException;
}
