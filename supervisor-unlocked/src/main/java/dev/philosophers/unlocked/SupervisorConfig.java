package dev.philosophers.unlocked;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupervisorConfig {

    @Bean
    RaceWindow raceWindow() {
        return RaceWindow.NONE;
    }

    @Bean
    UnlockedForkTable forkTable(
            @Value("${supervisor.philosophers:5}") int nombrePhilosophes, RaceWindow fenetre) {
        return new UnlockedForkTable(nombrePhilosophes, fenetre);
    }
}
