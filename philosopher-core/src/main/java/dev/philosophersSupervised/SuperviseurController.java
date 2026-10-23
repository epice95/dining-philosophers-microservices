package dev.philosophersSupervised;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fourchettes")
@Profile("superviseur")
public class SuperviseurController {

    private final SuperviseurService service;

    public SuperviseurController(SuperviseurService service) {
        this.service = service;
    }

    @PostMapping("/{id}/prendre")
    public boolean prendre(@PathVariable int id) {
        return service.prendreFourchette(id);
    }

    @PostMapping("/{id}/liberer")
    public void liberer(@PathVariable int id) {
        service.libererFourchette(id);
    }
}