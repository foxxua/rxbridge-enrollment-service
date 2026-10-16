package io.rxbridge.enrollment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    private final ZoneId zone;

    public TimeConfig(
        @Value("${rxbridge.time-zone}") ZoneId zone
    ) {
        this.zone = zone;
    }

    @Bean
    Clock clock() {
        return Clock.system(zone);
    }

}
