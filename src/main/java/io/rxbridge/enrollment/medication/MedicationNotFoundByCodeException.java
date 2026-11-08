package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.error.NotFoundException;

public class MedicationNotFoundByCodeException extends NotFoundException {

    private static final String TEMPLATE = "Medication not found by code: %s";

    private final String code;

    public MedicationNotFoundByCodeException(String code) {
        String normalizedCode = Medication.normalizeCode(code);
        super(TEMPLATE.formatted(normalizedCode));
        this.code = normalizedCode;
    }

    public String code() {
        return this.code;
    }

}
