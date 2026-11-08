package io.rxbridge.enrollment.medication;

import java.util.UUID;

public record MedicationView(
    UUID id,
    String name,
    String code,
    boolean active
) {

    public static MedicationView from(Medication medication) {
        return new MedicationView(
            medication.getId(),
            medication.getName(),
            medication.getCode(),
            medication.isActive()
        );
    }

}
