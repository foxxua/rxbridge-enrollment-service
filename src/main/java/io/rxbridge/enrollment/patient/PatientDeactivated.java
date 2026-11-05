package io.rxbridge.enrollment.patient;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PatientDeactivated(
    UUID patientId,
    LocalDate deactivationDate
) {

    public PatientDeactivated {
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(deactivationDate, "deactivationDate");
    }

}
