package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.patient.Patient;

import java.time.LocalDate;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

public final class EnrollmentFixtures {

    public static final LocalDate START_DATE = LocalDate.of(2025, 8, 10);
    public static final LocalDate CLOSE_DATE = LocalDate.of(2026, 9, 1);
    public static final String OTHER_REASON = "Other Reason Text";
    public static final Closure REJECT_CLOSURE = new Closure(
        CloseReason.NOT_ELIGIBLE,
        null
    );
    public static final Closure REJECT_CLOSURE_OTHER = new Closure(
        CloseReason.OTHER,
        OTHER_REASON
    );

    private EnrollmentFixtures() { }

    public static EnrollmentBuilder anEnrollmentBuilder() {
        return new EnrollmentBuilder();
    }

    public static Enrollment anEnrollment() {
        return anEnrollmentBuilder().build();
    }

    public static Enrollment anEnrollmentWithInactivePatient() {
        Enrollment enrollment = anEnrollment();
        enrollment.getPatient().deactivate();

        return enrollment;
    }

    public static Enrollment anEnrollmentWithInactiveMedication() {
        Enrollment enrollment = anEnrollment();
        enrollment.getMedication().deactivate();

        return enrollment;
    }

    public static Enrollment anEnrollmentWithInactiveRelations() {
        Enrollment enrollment = anEnrollment();
        enrollment.getPatient().deactivate();
        enrollment.getMedication().deactivate();

        return enrollment;
    }

    public static Enrollment anApprovedEnrollment() {
        Enrollment enrollment = anEnrollment();
        enrollment.approve(START_DATE);

        return enrollment;
    }

    public static Enrollment anApprovedEnrollmentWithInactivePatient() {
        Enrollment enrollment = anApprovedEnrollment();
        enrollment.getPatient().deactivate();

        return enrollment;
    }

    public static Enrollment anApprovedEnrollmentWithInactiveMedication() {
        Enrollment enrollment = anApprovedEnrollment();
        enrollment.getMedication().deactivate();

        return enrollment;
    }

    public static Enrollment anApprovedEnrollmentWithInactiveRelations() {
        Enrollment enrollment = anApprovedEnrollment();
        enrollment.getPatient().deactivate();
        enrollment.getMedication().deactivate();

        return enrollment;
    }

    public static Enrollment aCompletedEnrollment() {
        Enrollment enrollment = anApprovedEnrollment();
        enrollment.complete(CLOSE_DATE, TODAY);

        return enrollment;
    }

    public static Enrollment aRejectedEnrollment() {
        Enrollment enrollment = anEnrollment();
        enrollment.reject(REJECT_CLOSURE, CLOSE_DATE, TODAY);

        return enrollment;
    }

    public static Enrollment aRejectedEnrollmentOtherReason() {
        Enrollment enrollment = anEnrollment();
        enrollment.reject(REJECT_CLOSURE_OTHER, CLOSE_DATE, TODAY);

        return enrollment;
    }

    public static final class EnrollmentBuilder {

        private Patient patient = aPatient();
        private Medication medication = aMedication();

        private EnrollmentBuilder() { }

        public EnrollmentBuilder patient(Patient patient) {
            this.patient = patient;
            return this;
        }

        public EnrollmentBuilder medication(Medication medication) {
            this.medication = medication;
            return this;
        }

        public  Enrollment build() {
            return new Enrollment(
                this.patient,
                this.medication
            );
        }

    }

}
