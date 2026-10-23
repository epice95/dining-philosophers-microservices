package dev.philosophers.monitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MonitorConfig {

    @Bean
    MonitorState monitorState(@Value("${monitor.history-capacity:200}") int historyCapacity) {
        return new MonitorState(historyCapacity);
    }
}
