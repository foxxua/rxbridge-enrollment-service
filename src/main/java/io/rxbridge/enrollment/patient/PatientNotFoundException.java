package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.error.NotFoundException;

import java.util.UUID;

public class PatientNotFoundException extends NotFoundException {

    private static final String TEMPLATE = "Patient not found: %s";

    private final UUID patientId;

    public PatientNotFoundException(UUID patientId) {
        super(TEMPLATE.formatted(patientId));
        this.patientId = patientId;
    }

    public UUID patientId() {
        return this.patientId;
    }

}
