package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.ConflictException;

public class InvalidEnrollmentTransitionException extends ConflictException {

    private static final String TEMPLATE = "Cannot %s a %s enrollment";

    private final String action;
    private final EnrollmentStatus status;

    public InvalidEnrollmentTransitionException(String action, EnrollmentStatus status) {
        super(TEMPLATE.formatted(action, status));
        this.action = action;
        this.status = status;
    }

    public String action() {
        return this.action;
    }

    public EnrollmentStatus status() {
        return this.status;
    }

}
