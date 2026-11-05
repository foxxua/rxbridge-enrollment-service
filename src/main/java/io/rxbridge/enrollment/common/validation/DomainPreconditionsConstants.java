package io.rxbridge.enrollment.common.validation;

public class DomainPreconditionsConstants {

    // Exceptions
    public static final String NOT_EMPTY_TEMPLATE = "%s must not be empty";
    public static final String AT_MOST_CHARACTERS_TEMPLATE = "%s must be at most %d characters";
    public static final String MUST_BLANK_TEMPLATE = "%s must be blank";
    public static final String MUST_MATCH_REGEXP_TEMPLATE = "%s must match to regexp";
    public static final String NOT_IN_FUTURE_TEMPLATE = "%s must not be in the future";
    public static final String NOT_BEFORE_TEMPLATE = "%s must not be before %s";

    private DomainPreconditionsConstants() { }

}
