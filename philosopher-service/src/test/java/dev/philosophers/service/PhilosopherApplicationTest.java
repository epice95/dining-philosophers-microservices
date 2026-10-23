package dev.philosophers.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "philosopher.id=test-philosopher",
            "philosopher.monitor-url=http://localhost:1",
            "philosopher.think-min=1ms",
            "philosopher.think-max=2ms",
            "philosopher.eat-min=1ms",
            "philosopher.eat-max=2ms"
        })
class PhilosopherApplicationTest {

    @Autowired private PhilosopherLoop loop;

    @Test
    void startsAndKeepsRunningWhenMonitorIsUnreachable() {
        assertThat(loop.isRunning()).isTrue();
    }
}
