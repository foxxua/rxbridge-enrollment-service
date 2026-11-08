package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.medication.MedicationDeactivated;
import io.rxbridge.enrollment.medication.MedicationNotFoundException;
import io.rxbridge.enrollment.medication.MedicationRepository;
import io.rxbridge.enrollment.patient.Patient;
import io.rxbridge.enrollment.patient.PatientDeactivated;
import io.rxbridge.enrollment.patient.PatientNotFoundException;
import io.rxbridge.enrollment.patient.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static io.rxbridge.enrollment.common.persistence.DatabaseConstraints.violates;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final PatientRepository patients;
    private final MedicationRepository medications;
    private final EnrollmentRepository enrollments;
    private final BusinessCalendar calendar;

    @Transactional
    public UUID enroll(UUID patientId, UUID medicationId) {
        Patient patient = patients.findById(patientId)
            .orElseThrow(() -> new PatientNotFoundException(patientId));
        Medication medication = medications.findById(medicationId)
            .orElseThrow(() -> new MedicationNotFoundException(medicationId));

        if (enrollments.hasOpenEnrollment(patientId, medicationId)) {
            throw new DuplicateEnrollmentException(patientId, medicationId);
        }

        try {
            return enrollments.saveAndFlush(new Enrollment(patient, medication)).getId();
        } catch (DataIntegrityViolationException e) {
            if (violates(e, EnrollmentRepository.OPEN_ENROLLMENT_INDEX)) {
                throw new DuplicateEnrollmentException(patientId, medicationId, e);
            }
            throw e;
        }
    }

    @Transactional
    public void approve(UUID enrollmentId, LocalDate startDate) {
        find(enrollmentId).approve(startDate, calendar.today());
    }

    @Transactional
    public void complete(UUID enrollmentId, LocalDate closeDate) {
        find(enrollmentId).complete(closeDate, calendar.today());
    }

    @Transactional
    public void reject(UUID enrollmentId, Closure closure, LocalDate closeDate) {
        find(enrollmentId).reject(closure, closeDate, calendar.today());
    }

    @Transactional
    public void cancelByPatient(UUID enrollmentId, UUID patientId, Closure closure, LocalDate closeDate) {
        Enrollment enrollment = enrollments.findById(enrollmentId)
            .filter(e -> e.getPatient().getId().equals(patientId))
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
        enrollment.cancel(closure, closeDate, calendar.today());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(PatientDeactivated event) {
        Closure closure = new Closure(CloseReason.PATIENT_DEACTIVATED, null);
        enrollments.findOpenByPatientId(event.patientId())
            .forEach(e -> e.cancel(closure, event.deactivationDate(), calendar.today()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(MedicationDeactivated event) {
        Closure closure = new Closure(CloseReason.MEDICATION_DEACTIVATED, null);
        enrollments.findOpenByMedicationId(event.medicationId())
            .forEach(e -> e.cancel(closure, event.deactivationDate(), calendar.today()));
    }

    private Enrollment find(UUID enrollmentId) {
        return enrollments.findById(enrollmentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    }

}
