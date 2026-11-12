package com.example.philosophesupervise.superviseur;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/repas")
@Profile("superviseur")
public class SuperviseurController {

    private final SuperviseurService superviseurService;

    public SuperviseurController(SuperviseurService superviseurService) {
        this.superviseurService = superviseurService;
    }

    @PostMapping("/demander")
    public ResponseEntity<Boolean> demander(@RequestBody Map<String, Integer> payload) {
        return ResponseEntity.ok(superviseurService.demanderBaguettes(payload.get("id")));
    }

    @PostMapping("/liberer")
    public ResponseEntity<Void> liberer(@RequestBody Map<String, Integer> payload) {
        superviseurService.libererBaguettes(payload.get("id"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fin")
    public ResponseEntity<Void> fin(@RequestBody Map<String, Integer> payload) {
        superviseurService.enregistrerFin(payload.get("id"));
        return ResponseEntity.ok().build();
    }
}
