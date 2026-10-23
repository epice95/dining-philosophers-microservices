package dev.philosophers.service;

import dev.philosophers.common.clock.LamportClock;
import dev.philosophers.core.DurationRange;
import dev.philosophers.core.DurationSource;
import dev.philosophers.core.EventPublisher;
import dev.philosophers.core.Pause;
import dev.philosophers.core.PhilosopherCycle;
import dev.philosophers.core.RandomDurationSource;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PhilosopherConfig {

    @Bean
    RestClient monitorClient(PhilosopherProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeout());
        factory.setReadTimeout(props.readTimeout());
        return RestClient.builder().baseUrl(props.monitorUrl()).requestFactory(factory).build();
    }

    @Bean
    EventPublisher eventPublisher(RestClient monitorClient) {
        return new HttpEventPublisher(monitorClient);
    }

    @Bean
    DurationSource durationSource(PhilosopherProperties props) {
        return new RandomDurationSource(
                props.effectiveSeed(),
                new DurationRange(props.thinkMin(), props.thinkMax()),
                new DurationRange(props.eatMin(), props.eatMax()));
    }

    @Bean
    PhilosopherCycle philosopherCycle(
            PhilosopherProperties props, EventPublisher publisher, DurationSource durations) {
        return new PhilosopherCycle(
                props.id(),
                new LamportClock(),
                publisher,
                Clock.systemUTC(),
                durations,
                Pause.THREAD);
    }

    @Bean
    PhilosopherLoop philosopherLoop(PhilosopherCycle cycle) {
        return new PhilosopherLoop(cycle);
    }
}
