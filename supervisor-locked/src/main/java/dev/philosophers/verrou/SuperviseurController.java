package dev.philosophers.verrou;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Même API que la partie 3.1 : seule la table (maintenant verrouillée) change. */
@RestController
@RequestMapping("/forks")
public class SuperviseurController {

    private final TableFourchettes table;

    public SuperviseurController(TableFourchettes table) {
        this.table = table;
    }

    @PostMapping("/{philosophe}/acquire")
    public boolean prendre(@PathVariable("philosophe") int philosophe) {
        return table.prendre(philosophe);
    }

    @PostMapping("/{philosophe}/release")
    public void rendre(@PathVariable("philosophe") int philosophe) {
        table.rendre(philosophe);
    }
}
