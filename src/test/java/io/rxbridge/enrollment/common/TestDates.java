package io.rxbridge.enrollment.common;

import java.time.LocalDate;

public final class TestDates {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    public static final LocalDate YESTERDAY = TODAY.minusDays(1);
    public static final LocalDate TOMORROW = TODAY.plusDays(1);

    private TestDates() { }

}
