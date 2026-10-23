package dev.philosophers.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhilosopherApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhilosopherApplication.class, args);
    }
}
