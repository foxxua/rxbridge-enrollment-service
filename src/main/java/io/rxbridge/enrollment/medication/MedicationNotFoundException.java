package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.error.NotFoundException;

import java.util.UUID;

public class MedicationNotFoundException extends NotFoundException {

    private static final String TEMPLATE = "Medication not found: %s";

    private final UUID medicationId;

    public MedicationNotFoundException(UUID medicationId) {
        super(TEMPLATE.formatted(medicationId));
        this.medicationId = medicationId;
    }

    public UUID medicationId() {
        return this.medicationId;
    }

}
