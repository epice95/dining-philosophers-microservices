package com.example.philosophesupervise.superviseur;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("superviseur") // Actif : --spring.profiles.active=superviseur
public class SuperviseurService {
    private final String[] etats =
            new String[] {"REFLECHIR", "REFLECHIR", "REFLECHIR", "REFLECHIR", "REFLECHIR"};
    private final List<Integer> ordreService = Collections.synchronizedList(new ArrayList<>());
    private int philosophesTermines = 0;

    public synchronized boolean demanderBaguettes(int id) {
        int gauche = id;
        int droite = (id + 1) % 5;
        if (!"MANGER".equals(etats[gauche]) && !"MANGER".equals(etats[droite])) {
            etats[id] = "MANGER";
            ordreService.add(id);
            return true;
        }
        etats[id] = "ATTENDRE";
        return false;
    }

    public synchronized void libererBaguettes(int id) {
        etats[id] = "REFLECHIR";
    }

    public synchronized void enregistrerFin(int id) {
        etats[id] = "FIN";
        philosophesTermines++;
        if (philosophesTermines == 5) {
            System.out.println("TOUS LES PHILOSOPHES ONT FINI DE MANGER");
            System.out.println("Ordre d'accès : " + ordreService);
        }
    }
}
