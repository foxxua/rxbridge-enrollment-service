package io.rxbridge.enrollment.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends Repository<Enrollment, UUID> {

    String OPEN_ENROLLMENT_INDEX = "uq_enrollment_patient_medication_active";

    Enrollment saveAndFlush(Enrollment enrollment);

    Optional<Enrollment> findById(UUID id);

    @EntityGraph(attributePaths = "medication")
    Page<Enrollment> findByPatientId(UUID patientId, Pageable pageable);

    boolean existsByPatientIdAndMedicationIdAndStatusIn(
        UUID patientId,
        UUID medicationId,
        Collection<EnrollmentStatus> statuses
    );

    default boolean hasOpenEnrollment(UUID patientId, UUID medicationId) {
        return existsByPatientIdAndMedicationIdAndStatusIn(
            patientId,
            medicationId,
            EnrollmentStatus.openStatuses()
        );
    }

}
