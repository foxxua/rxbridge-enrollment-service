package io.rxbridge.enrollment.common.validation;

import io.rxbridge.enrollment.common.error.DomainValidationException;

import java.time.LocalDate;
import java.util.Objects;

import static io.rxbridge.enrollment.common.validation.DomainPreconditionsConstants.*;

public final class DomainPreconditions {

    private DomainPreconditions() { }

    public static String requireText(String text, String field, int maxLength) {
        Objects.requireNonNull(text, field);

        text = text.strip();

        if (text.isEmpty()) {
            throw new DomainValidationException(
                NOT_EMPTY_TEMPLATE.formatted(field)
            );
        }

        if (text.length() > maxLength) {
            throw new DomainValidationException(
                AT_MOST_CHARACTERS_TEMPLATE.formatted(field, maxLength)
            );
        }

        return text;
    }

    public static String requireAbsent(String text, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }

        throw new DomainValidationException(
            MUST_BLANK_TEMPLATE.formatted(field)
        );
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
            throw new DomainValidationException(
                MUST_MATCH_REGEXP_TEMPLATE.formatted(field)
            );
        }

        return text;
    }

    public static LocalDate requireNotFuture(LocalDate date, LocalDate today, String field) {
        Objects.requireNonNull(date, field);
        Objects.requireNonNull(today, field + " today");

        if (date.isAfter(today)) {
            throw new DomainValidationException(
                NOT_IN_FUTURE_TEMPLATE.formatted(field)
            );
        }

        return date;
    }

    public static LocalDate requireNotBefore(LocalDate date, LocalDate lowerBound, String field) {
        Objects.requireNonNull(date, field);
        Objects.requireNonNull(lowerBound, field + " lowerBound");

        if (date.isBefore(lowerBound)) {
            throw new DomainValidationException(
                NOT_BEFORE_TEMPLATE.formatted(field, lowerBound)
            );
        }

        return date;
    }

}
