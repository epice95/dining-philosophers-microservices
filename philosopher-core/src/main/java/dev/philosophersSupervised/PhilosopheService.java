package dev.philosophersSupervised;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("philosophe")
public class PhilosopheService {

    @Value("${philosophe.id:0}")
    private int id;

    @Value("${superviseur.url:http://localhost:8080}")
    private String superviseurUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @EventListener(ApplicationReadyEvent.class)
    public void demarrer() {
        new Thread(this::run).start();
    }

    private void run() {
        int fGauche = id;
        int fDroite = (id + 1) % 5;

        for (int i = 0; i < 3; i++) {
            afficher("REFLECHIR");
            pause(1000, 2000);

            afficher("ATTENDRE - Demande fourchette gauche (" + fGauche + ")");
            demanderFourchette(fGauche);

            afficher("ATTENDRE - Demande fourchette droite (" + fDroite + ")");
            demanderFourchette(fDroite);

            afficher("MANGER");
            pause(1000, 1500);

            libererFourchette(fGauche);
            libererFourchette(fDroite);
        }
        afficher("FIN");
    }

    private void demanderFourchette(int indexFourchette) {
        boolean obtenue = false;
        while (!obtenue) {
            try {
                Boolean res = restTemplate.postForObject(
                        superviseurUrl + "/fourchettes/" + indexFourchette + "/prendre",
                        null,
                        Boolean.class
                );
                obtenue = Boolean.TRUE.equals(res);
            } catch (Exception ignored) {}

            if (!obtenue) pause(100, 300); // si n'est pas obtenu attente
        }
    }

    private void libererFourchette(int indexFourchette) {
        try {
            restTemplate.postForObject(superviseurUrl + "/fourchettes/" + indexFourchette + "/liberer", null, Void.class);
        } catch (Exception ignored) {}
    }

    private void afficher(String etat) {
        System.out.printf("[Philosophe %d] - État : %s%n", id, etat);
    }

    private void pause(int min, int max) {
        try {
            Thread.sleep(min + (long) (Math.random() * (max - min)));
        } catch (InterruptedException ignored) {}
    }
}