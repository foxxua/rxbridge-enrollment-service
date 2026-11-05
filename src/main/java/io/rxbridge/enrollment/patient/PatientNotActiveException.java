package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.error.ConflictException;

import java.util.UUID;

public class PatientNotActiveException extends ConflictException {

    private static final String TEMPLATE = "Patient %s is not active";

    private final UUID patientId;

    public PatientNotActiveException(UUID patientId) {
        super(TEMPLATE.formatted(patientId));
        this.patientId = patientId;
    }

    public UUID patientId() {
        return this.patientId;
    }

}
