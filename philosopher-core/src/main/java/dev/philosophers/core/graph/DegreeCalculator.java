package dev.philosophers.core.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Calcul des degrés (graphe non orienté) et fusion des résultats partiels. */
public final class DegreeCalculator {

    private DegreeCalculator() {}

    /** Chaque arête ajoute 1 au degré de ses deux extrémités (une boucle compte 2). */
    public static Map<Integer, Integer> compute(Collection<Edge> edges) {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (Edge e : edges) {
            degrees.merge(e.from(), 1, Integer::sum);
            degrees.merge(e.to(), 1, Integer::sum);
        }
        return degrees;
    }

    /** Additionne les degrés partiels. */
    public static Map<Integer, Integer> merge(Collection<Map<Integer, Integer>> partials) {
        Map<Integer, Integer> total = new HashMap<>();
        for (Map<Integer, Integer> partial : partials) {
            partial.forEach((node, degree) -> total.merge(node, degree, Integer::sum));
        }
        return total;
    }
}
