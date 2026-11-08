package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.error.ConflictException;

public class DuplicateMedicationException extends ConflictException {

    private static final String TEMPLATE = "Medication with code %s already exists";

    private final String code;

    public DuplicateMedicationException(String code) {
        this(code, null);
    }

    public DuplicateMedicationException(String code, Throwable cause) {
        super(TEMPLATE.formatted(code), cause);
        this.code = code;
    }

    public String code() {
        return this.code;
    }

}
