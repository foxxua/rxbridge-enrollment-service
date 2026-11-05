package io.rxbridge.enrollment.common.calendar;

import io.rxbridge.enrollment.configuration.TimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BusinessCalendar {

    @Qualifier(TimeConfig.BUSINESS_CLOCK)
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

}
