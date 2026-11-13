package dev.philosophers.unlocked;

/**
 * Point d'exécution situé entre la vérification des fourchettes et leur réservation. En production
 * il ne fait rien ; les tests y placent une barrière pour forcer deux requêtes à se croiser
 * exactement à cet endroit, ce qui rend la course reproductible.
 */
@FunctionalInterface
public interface RaceWindow {

    RaceWindow NONE = philosophe -> {};

    /** Appelé après avoir constaté que les deux fourchettes sont libres, avant de les réserver. */
    void between(int philosophe);
}
