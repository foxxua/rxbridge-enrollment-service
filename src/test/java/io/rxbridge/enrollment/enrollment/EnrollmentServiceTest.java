package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.medication.MedicationDeactivated;
import io.rxbridge.enrollment.medication.MedicationNotFoundException;
import io.rxbridge.enrollment.medication.MedicationRepository;
import io.rxbridge.enrollment.patient.Patient;
import io.rxbridge.enrollment.patient.PatientDeactivated;
import io.rxbridge.enrollment.patient.PatientNotFoundException;
import io.rxbridge.enrollment.patient.PatientRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    static final UUID PATIENT_ID = UUID.randomUUID();
    static final UUID ANOTHER_PATIENT_ID = UUID.randomUUID();
    static final UUID MEDICATION_ID = UUID.randomUUID();
    static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @Mock PatientRepository patients;
    @Mock MedicationRepository medications;
    @Mock EnrollmentRepository enrollments;
    BusinessCalendar calendar = businessCalendar();

    EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
            patients,
            medications,
            enrollments,
            calendar
        );
    }

    @Nested
    class Enroll {

        @Test
        void createsRequestedEnrollment() {
            Patient patient = givenPatient();
            Medication medication = givenMedication();
            given(enrollments.saveAndFlush(any())).willReturn(enrollmentWithId(ENROLLMENT_ID));

            UUID enrollmentId = service.enroll(PATIENT_ID, MEDICATION_ID);

            ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
            then(enrollments).should().saveAndFlush(saved.capture());
            assertThat(saved.getValue().getPatient()).isEqualTo(patient);
            assertThat(saved.getValue().getMedication()).isEqualTo(medication);
            assertThat(saved.getValue().getStatus()).isEqualTo(EnrollmentStatus.PENDING);
            assertThat(enrollmentId).isEqualTo(ENROLLMENT_ID);
        }

        @Test
        void refusesUnknownPatient() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.enroll(PATIENT_ID, MEDICATION_ID))
                .isInstanceOf(PatientNotFoundException.class);
            then(enrollments).should(never()).saveAndFlush(any());
        }

        @Test
        void refusesUnknownMedication() {
            givenPatient();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.enroll(PATIENT_ID, MEDICATION_ID))
                .isInstanceOf(MedicationNotFoundException.class);
            then(enrollments).should(never()).saveAndFlush(any());
        }

        @Test
        void refusesSecondOpenEnrollmentOfSameMedication() {
            givenPatient();
            givenMedication();
            given(enrollments.hasOpenEnrollment(PATIENT_ID, MEDICATION_ID)).willReturn(true);

            assertThatThrownBy(() -> service.enroll(PATIENT_ID, MEDICATION_ID))
                .isInstanceOf(DuplicateEnrollmentException.class);
            then(enrollments).should(never()).saveAndFlush(any());
        }

        @Test
        void reportsDuplicateWhenRaceIsLostOnIndex() {
            givenPatient();
            givenMedication();
            var violation = violationOf(EnrollmentRepository.OPEN_ENROLLMENT_INDEX);
            given(enrollments.saveAndFlush(any())).willThrow(violation);

            assertThatThrownBy(() -> service.enroll(PATIENT_ID, MEDICATION_ID))
                .isInstanceOf(DuplicateEnrollmentException.class)
                .hasCause(violation);
        }

        @Test
        void passesOtherConstraintViolationThrough() {
            givenPatient();
            givenMedication();
            var violation = violationOf("chk_enrollment_other_reason_null_or_not");
            given(enrollments.saveAndFlush(any())).willThrow(violation);

            assertThatThrownBy(() -> service.enroll(PATIENT_ID, MEDICATION_ID))
                .isSameAs(violation);
        }

    }

    @Nested
    class Transitions {

        @Test
        void approvesPendingEnrollment() {
            Enrollment enrollment = givenEnrollment(anEnrollment());

            service.approve(ENROLLMENT_ID, YESTERDAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getStartDate()).isEqualTo(YESTERDAY);
        }

        @Test
        void refusesApprovingUnknownEnrollment() {
            given(enrollments.findById(ENROLLMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(ENROLLMENT_ID, TODAY))
                .isInstanceOf(EnrollmentNotFoundException.class);
        }

        @Test
        void refusesApprovingWithDateAfterCalendarToday() {
            givenEnrollment(anEnrollment());

            assertThatThrownBy(() -> service.approve(ENROLLMENT_ID, TOMORROW))
                .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsPendingEnrollment() {
            Enrollment enrollment = givenEnrollment(anEnrollment());

            service.reject(ENROLLMENT_ID, REJECT_CLOSURE, YESTERDAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.REJECTED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.NOT_ELIGIBLE);
            assertThat(enrollment.getOtherReason()).isNull();
            assertThat(enrollment.getCloseDate()).isEqualTo(YESTERDAY);
        }

        @Test
        void refusesRejectingWithDateAfterCalendarToday() {
            givenEnrollment(anEnrollment());

            assertThatThrownBy(() -> service.reject(ENROLLMENT_ID, REJECT_CLOSURE, TOMORROW))
                .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void completesApprovedEnrollment() {
            Enrollment enrollment = givenEnrollment(anApprovedEnrollment());

            service.complete(ENROLLMENT_ID, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(TODAY);
        }

        @Test
        void refusesCompletingWithDateAfterCalendarToday() {
            givenEnrollment(anApprovedEnrollment());

            assertThatThrownBy(() -> service.complete(ENROLLMENT_ID, TOMORROW))
                .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void patientCancelsOwnEnrollment() {
            Enrollment enrollment = givenEnrollment(
                anEnrollmentBuilder()
                    .patient(patientWithId(PATIENT_ID))
                    .build()
            );

            service.cancelByPatient(ENROLLMENT_ID, PATIENT_ID, CLOSURE_OTHER, YESTERDAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
            assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
            assertThat(enrollment.getCloseDate()).isEqualTo(YESTERDAY);
        }

        @Test
        void patientCannotCancelSomeoneElsesEnrollment() {
            Enrollment enrollment = givenEnrollment(
                anEnrollmentBuilder()
                    .patient(patientWithId(ANOTHER_PATIENT_ID))
                    .build()
            );

            assertThatThrownBy(() -> service.cancelByPatient(ENROLLMENT_ID, PATIENT_ID, CLOSURE_OTHER, TODAY))
                .isInstanceOf(EnrollmentNotFoundException.class);
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void refusesCancellingWithDateAfterCalendarToday() {
            givenEnrollment(
                anEnrollmentBuilder()
                    .patient(patientWithId(PATIENT_ID))
                    .build()
            );

            assertThatThrownBy(() -> service.cancelByPatient(ENROLLMENT_ID, PATIENT_ID, CLOSURE_OTHER, TOMORROW))
                .isInstanceOf(DomainValidationException.class);
        }

    }

    @Nested
    class PatientDeactivatedEvent {

        @Test
        void cancelsOpenEnrollmentsOfDeactivatedPatient() {
            Patient patient = aPatient();
            Enrollment pended = anEnrollmentBuilder().patient(patient).build();
            Enrollment approved = anEnrollmentBuilder().patient(patient).build();

            approved.approve(YESTERDAY, TODAY);
            patient.deactivate();

            given(enrollments.findOpenByPatientId(PATIENT_ID)).willReturn(List.of(pended, approved));

            service.on(new PatientDeactivated(PATIENT_ID, YESTERDAY));

            assertThat(List.of(pended, approved))
                .extracting(Enrollment::getStatus, Enrollment::getCloseReason, Enrollment::getCloseDate)
                .containsOnly(tuple(EnrollmentStatus.CANCELLED, CloseReason.PATIENT_DEACTIVATED, YESTERDAY));
        }

    }

    @Nested
    class MedicationDeactivatedEvent {

        @Test
        void cancelsOpenEnrollmentsOfDeactivatedMedication() {
            Medication medication = aMedication();
            Enrollment pended = anEnrollmentBuilder().medication(medication).build();
            Enrollment approved = anEnrollmentBuilder().medication(medication).build();

            approved.approve(YESTERDAY, TODAY);
            medication.deactivate();

            given(enrollments.findOpenByMedicationId(MEDICATION_ID)).willReturn(List.of(pended, approved));

            service.on(new MedicationDeactivated(MEDICATION_ID, YESTERDAY));

            assertThat(List.of(pended, approved))
                .extracting(Enrollment::getStatus, Enrollment::getCloseReason, Enrollment::getCloseDate)
                .containsOnly(tuple(EnrollmentStatus.CANCELLED, CloseReason.MEDICATION_DEACTIVATED, YESTERDAY));
        }

    }

    private Patient givenPatient() {
        Patient patient = aPatient();
        given(patients.findById(PATIENT_ID)).willReturn(Optional.of(patient));
        return patient;
    }

    private Medication givenMedication() {
        Medication medication = aMedication();
        given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));
        return medication;
    }

    private Enrollment givenEnrollment(Enrollment enrollment) {
        given(enrollments.findById(ENROLLMENT_ID)).willReturn(Optional.of(enrollment));
        return enrollment;
    }

    private Patient patientWithId(UUID id) {
        Patient patient = aPatient();
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }

    private Enrollment enrollmentWithId(UUID id) {
        Enrollment enrollment = anEnrollment();
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    private static DataIntegrityViolationException violationOf(String constraint) {
        var cause = new ConstraintViolationException(
            "violation",
            new SQLException("violation"),
            constraint
        );

        return new DataIntegrityViolationException("violation", cause);
    }

}
