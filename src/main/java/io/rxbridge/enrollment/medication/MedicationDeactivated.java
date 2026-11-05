package io.rxbridge.enrollment.medication;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record MedicationDeactivated(
    UUID medicationId,
    LocalDate deactivationDate
) {

    public MedicationDeactivated {
        Objects.requireNonNull(medicationId, "medicationId");
        Objects.requireNonNull(deactivationDate, "deactivationDate");
    }

}
