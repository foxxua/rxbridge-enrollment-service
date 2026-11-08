package io.rxbridge.enrollment.patient;

import java.time.LocalDate;
import java.util.UUID;

public record PatientView(
    UUID id,
    PersonNameView personName,
    String email,
    AddressView address,
    Sex sex,
    LocalDate birthdate,
    boolean active
) {

    static PatientView from(Patient patient) {
        return new PatientView(
            patient.getId(),
            PersonNameView.from(patient.getPersonName()),
            patient.getEmail(),
            AddressView.from(patient.getAddress()),
            patient.getSex(),
            patient.getBirthdate(),
            patient.isActive()
        );
    }

}
