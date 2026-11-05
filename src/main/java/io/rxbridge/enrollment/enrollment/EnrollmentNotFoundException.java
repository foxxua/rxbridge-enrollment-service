package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.NotFoundException;

import java.util.UUID;

public class EnrollmentNotFoundException extends NotFoundException {

    private static final String TEMPLATE = "Enrollment not found: %s";

    private final UUID enrollmentId;

    public EnrollmentNotFoundException(UUID enrollmentId) {
        super(TEMPLATE.formatted(enrollmentId));
        this.enrollmentId = enrollmentId;
    }

    public UUID enrollmentId() {
        return this.enrollmentId;
    }

}
