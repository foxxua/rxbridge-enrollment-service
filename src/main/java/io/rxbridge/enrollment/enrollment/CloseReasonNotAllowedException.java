package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;

public class CloseReasonNotAllowedException extends DomainValidationException {

    private static final String TEMPLATE = "close reason %s is not allowed for status %s";

    private final CloseReason reason;
    private final EnrollmentStatus status;

    public CloseReasonNotAllowedException(
        CloseReason reason,
        EnrollmentStatus status
    ) {
        super(TEMPLATE.formatted(reason, status));
        this.reason = reason;
        this.status = status;
    }

    public CloseReason reason() {
        return this.reason;
    }

    public EnrollmentStatus status() {
        return this.status;
    }

}
