package io.rxbridge.enrollment.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    public static final String BUSINESS_CLOCK = "businessClock";

    private final ZoneId zone;

    public TimeConfig(
        @Value("${rxbridge.time-zone}") ZoneId zone
    ) {
        this.zone = zone;
    }

    @Bean
    @Qualifier(TimeConfig.BUSINESS_CLOCK)
    Clock clockNY() {
        return Clock.system(zone);
    }

}
