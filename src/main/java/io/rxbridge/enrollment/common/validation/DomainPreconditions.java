package io.rxbridge.enrollment.common.validation;

import java.time.LocalDate;
import java.util.Objects;

public final class DomainPreconditions {

    private DomainPreconditions() { }

    public static String requireText(String text, String field, int maxLength) {
        Objects.requireNonNull(text, field);

        text = text.strip();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                "%s must not be empty".formatted(field)
            );
        }

        if (text.length() > maxLength) {
            throw new IllegalArgumentException(
                "%s must be at most %d characters"
                    .formatted(field, maxLength)
            );
        }

        return text;
    }

    public static String optionalText(String text, String field, int maxLength) {
        if (text == null || text.isBlank()) {
            return null;
        }

        return requireText(text, field, maxLength);
    }

    public static String requireMatch(String text, String field, String regexp) {
        Objects.requireNonNull(text, field);

        if (!text.matches(regexp)) {
            throw new IllegalArgumentException(
                "%s must match to regexp %s"
                    .formatted(field, regexp)
            );
        }

        return text;
    }

    public static LocalDate requireNotFuture(LocalDate date, LocalDate today, String field) {
        Objects.requireNonNull(date, field);
        Objects.requireNonNull(today, field + " today");

        if (date.isAfter(today)) {
            throw new IllegalArgumentException(
                "%s must not be in the future".formatted(field)
            );
        }

        return date;
    }

    public static LocalDate requireNotBefore(LocalDate date, LocalDate lowerBound, String field) {
        Objects.requireNonNull(date, field);
        Objects.requireNonNull(lowerBound, field + " lowerBound");

        if (date.isBefore(lowerBound)) {
            throw new IllegalArgumentException(
                "%s must not be before %s".formatted(field, lowerBound)
            );
        }

        return date;
    }

}
