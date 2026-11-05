package io.rxbridge.enrollment.enrollment;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentView(
    UUID id,
    String medicationName,
    EnrollmentStatus status,
    LocalDate startDate,
    LocalDate closeDate
) {

    static EnrollmentView from(Enrollment enrollment) {
        return new EnrollmentView(
            enrollment.getId(),
            enrollment.getMedication().getName(),
            enrollment.getStatus(),
            enrollment.getStartDate(),
            enrollment.getCloseDate()
        );
    }

}
