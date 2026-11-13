package dev.philosophers.unlocked;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API du superviseur. Tomcat traite chaque requête dans un thread de son pool : des demandes
 * simultanées s'exécutent donc en parallèle sur la même {@link UnlockedForkTable}.
 */
@RestController
@RequestMapping("/forks")
public class SupervisorController {

    private final UnlockedForkTable table;

    public SupervisorController(UnlockedForkTable table) {
        this.table = table;
    }

    /** Demande des deux fourchettes du philosophe : {@code true} si accordées. */
    @PostMapping("/{philosopher}/acquire")
    public boolean acquire(@PathVariable("philosopher") int philosophe) {
        return table.acquire(philosophe);
    }

    /** Libération des fourchettes du philosophe. */
    @PostMapping("/{philosopher}/release")
    public void release(@PathVariable("philosopher") int philosophe) {
        table.release(philosophe);
    }
}
