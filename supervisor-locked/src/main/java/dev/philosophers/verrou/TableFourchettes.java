package dev.philosophers.verrou;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * État des fourchettes, <strong>avec verrou</strong> (partie 3.2).
 *
 * <p>Le philosophe {@code i} demande ses deux fourchettes : F_i et F_(i+1) mod N.
 *
 * <p>Tout le code qui lit ou modifie {@code proprietaires} est une section critique : il est
 * entouré par {@code verrou.lock()} / {@code verrou.unlock()}. Un seul thread à la fois peut donc «
 * vérifier puis réserver », ce qui corrige la faille de la partie 3.1.
 */
@Component
public class TableFourchettes {

    private static final int LIBRE = -1;

    private final int[] proprietaires; // proprietaires[f] = philosophe qui tient f, ou LIBRE
    private final ReentrantLock verrou = new ReentrantLock();

    public TableFourchettes(@Value("${supervisor.philosophers:5}") int nombrePhilosophes) {
        proprietaires = new int[nombrePhilosophes];
        Arrays.fill(proprietaires, LIBRE);
    }

    /** Donne les deux fourchettes au philosophe si elles sont libres. */
    public boolean prendre(int philosophe) {
        int gauche = philosophe;
        int droite = (philosophe + 1) % proprietaires.length;

        verrou.lock(); // début de la section critique
        try {
            // vérifier F_i et F_(i+1)
            if (proprietaires[gauche] == LIBRE && proprietaires[droite] == LIBRE) {
                // les deux sont libres : réserver les deux
                proprietaires[gauche] = philosophe;
                proprietaires[droite] = philosophe;
                return true;
            }
            return false; // au moins une est prise : refuser
        } finally {
            verrou.unlock(); // fin de la section critique (même en cas d'erreur)
        }
    }

    /** Rend les fourchettes que le philosophe tient. */
    public void rendre(int philosophe) {
        int gauche = philosophe;
        int droite = (philosophe + 1) % proprietaires.length;

        verrou.lock();
        try {
            if (proprietaires[gauche] == philosophe) {
                proprietaires[gauche] = LIBRE;
            }
            if (proprietaires[droite] == philosophe) {
                proprietaires[droite] = LIBRE;
            }
        } finally {
            verrou.unlock();
        }
    }
}
