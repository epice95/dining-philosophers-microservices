package dev.philosophers.core.graph;

import java.util.List;

/** Accès à un Worker pour le calcul d'accessibilité. L'implémentation HTTP est dans le service. */
public interface ReachabilityWorkerClient {

    /** Nom lisible du Worker, pour les logs et les messages d'erreur. */
    String name();

    /** Confie au Worker les arêtes dont il est propriétaire pour ce calcul. */
    void load(String jobId, List<Edge> edges) throws WorkerException;

    /** Renvoie les voisins (successeurs) des nœuds donnés, que ce Worker possède. */
    List<Integer> expand(String jobId, List<Integer> nodes) throws WorkerException;

    /** Demande au Worker d'oublier ce calcul. */
    void release(String jobId) throws WorkerException;
}
