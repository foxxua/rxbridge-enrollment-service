package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.error.ConflictException;

import java.util.UUID;

public class MedicationNotActiveException extends ConflictException {

    private static final String TEMPLATE = "Medication %s is not active";

    private final UUID medicationId;

    public MedicationNotActiveException(UUID medicationId) {
        super(TEMPLATE.formatted(medicationId));
        this.medicationId = medicationId;
    }

    public UUID medicationId() {
        return this.medicationId;
    }

}
