package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.medication.MedicationView;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentView(
    UUID id,
    MedicationView medication,
    EnrollmentStatus status,
    LocalDate startDate,
    ClosureView closure
) {

    static EnrollmentView from(Enrollment enrollment) {
        return new EnrollmentView(
            enrollment.getId(),
            MedicationView.from(enrollment.getMedication()),
            enrollment.getStatus(),
            enrollment.getStartDate(),
            (enrollment.getCloseDate() == null)
                ? null
                : new ClosureView(
                    enrollment.getCloseReason(),
                    enrollment.getOtherReason(),
                    enrollment.getCloseDate()
                )
        );
    }

}
