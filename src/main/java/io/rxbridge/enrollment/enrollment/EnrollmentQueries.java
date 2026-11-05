package io.rxbridge.enrollment.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentQueries {

    private final EnrollmentRepository enrollments;

    public Page<EnrollmentView> enrollmentsOf(UUID patientId, Pageable pageable) {
        return enrollments.findByPatientId(patientId, pageable)
            .map(EnrollmentView::from);
    }

    public EnrollmentView enrollment(UUID enrollmentId) {
        return enrollments.findById(enrollmentId)
            .map(EnrollmentView::from)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    }

}
