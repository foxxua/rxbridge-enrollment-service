package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.ConflictException;

import java.util.UUID;

public class DuplicateEnrollmentException extends ConflictException {

    private static final String TEMPLATE = "Patient %s already has an open enrollment for medication %s";

    private final UUID patientId;
    private final UUID medicationId;

    public DuplicateEnrollmentException(UUID patientId, UUID medicationId) {
        this(patientId, medicationId, null);
    }

    public DuplicateEnrollmentException(UUID patientId, UUID medicationId, Throwable cause) {
        super(TEMPLATE.formatted(patientId, medicationId), cause);
        this.patientId = patientId;
        this.medicationId = medicationId;
    }

    public UUID patientId() {
        return this.patientId;
    }

    public UUID medicationId() {
        return this.medicationId;
    }

}
