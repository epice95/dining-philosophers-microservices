package dev.philosophers.unlocked;

import java.util.Arrays;
import java.util.Objects;

/**
 * État partagé des fourchettes, <strong>sans verrou</strong> (partie 3.1).
 *
 * <p>Le philosophe {@code i} demande en une seule requête ses deux fourchettes : {@code F_i} et
 * {@code F_(i+1) mod N}. Le superviseur vérifie qu'elles sont libres, puis les réserve.
 *
 * <p>Cette classe est volontairement <strong>non thread-safe</strong>. Chaque requête web est
 * traitée dans son propre thread : la séquence « vérifier puis réserver » n'est pas atomique, donc
 * deux philosophes voisins peuvent recevoir la même fourchette.
 *
 * <pre>
 *   T1 (P0) : F0 libre ? F1 libre ? oui ─┐
 *   T2 (P1) : F1 libre ? F2 libre ? oui ─┤  les deux tests passent avant toute écriture
 *   T1 (P0) : réserve F0, F1            │
 *   T2 (P1) : réserve F1, F2            ┘  F1 est attribuée deux fois
 * </pre>
 */
public final class UnlockedForkTable {

    private static final int FREE = -1;

    private final int[] proprietaires;
    private final RaceWindow fenetre;

    public UnlockedForkTable(int nombrePhilosophes, RaceWindow fenetre) {
        this.proprietaires = new int[nombrePhilosophes];
        Arrays.fill(proprietaires, FREE);
        this.fenetre = Objects.requireNonNull(fenetre, "fenetre");
    }

    /**
     * Demande les deux fourchettes de {@code philosophe} d'un coup.
     *
     * @return {@code true} si elles sont accordées, {@code false} si l'une est occupée
     */
    public boolean acquire(int philosophe) {
        int gauche = philosophe;
        int droite = droite(philosophe);

        // section critique NON protégée
        if (proprietaires[gauche] == FREE && proprietaires[droite] == FREE) { // vérifier
            fenetre.between(philosophe); // un autre thread peut passer ici
            proprietaires[gauche] = philosophe; // réserver
            proprietaires[droite] = philosophe;
            return true;
        }
        return false;
    }

    /** Rend les fourchettes que {@code philosophe} détient. */
    public void release(int philosophe) {
        int gauche = philosophe;
        int droite = droite(philosophe);
        if (proprietaires[gauche] == philosophe) {
            proprietaires[gauche] = FREE;
        }
        if (proprietaires[droite] == philosophe) {
            proprietaires[droite] = FREE;
        }
    }

    private int droite(int philosophe) {
        return (philosophe + 1) % proprietaires.length;
    }
}
