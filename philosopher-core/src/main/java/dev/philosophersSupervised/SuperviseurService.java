package dev.philosophersSupervised;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.Arrays;

@Service
@Profile("superviseur")
public class SuperviseurService {
    // false = libre, true = occupée
    private final boolean[] fourchettes = new boolean[5];

    public synchronized boolean prendreFourchette(int index) {
        if (!fourchettes[index]) {
            fourchettes[index] = true; // Fourchette donner
            return true;
        }
        return false; // Fait patienter
    }

    public synchronized void libererFourchette(int index) {
        fourchettes[index] = false;
    }
}