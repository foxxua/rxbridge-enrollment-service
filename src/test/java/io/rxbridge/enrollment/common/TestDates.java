package io.rxbridge.enrollment.common;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class TestDates {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    public static final LocalDate YESTERDAY = TODAY.minusDays(1);
    public static final LocalDate TOMORROW = TODAY.plusDays(1);

    // 22:30 in New York, already the 15th in UTC
    public static final Instant NOW = Instant.parse("2026-10-15T02:30:00Z");

    public static BusinessCalendar businessCalendar() {
        return new BusinessCalendar(Clock.fixed(NOW, ZoneId.of("America/New_York")));
    }

    private TestDates() { }

}
