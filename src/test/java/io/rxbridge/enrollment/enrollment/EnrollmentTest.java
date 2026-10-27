package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

class EnrollmentTest {

    @Nested
    class Creation {

        @Test
        void enrollsWithInitialStatus() {
            Enrollment enrollment = anEnrollment();

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
                softly.assertThat(enrollment.getStartDate()).isNull();
                softly.assertThat(enrollment.getCloseDate()).isNull();
                softly.assertThat(enrollment.getCloseReason()).isNull();
                softly.assertThat(enrollment.getOtherReason()).isNull();
            });
        }

        @Test
        void rejectsNullPatient() {
            assertThatNullPointerException()
                .isThrownBy(() -> anEnrollmentBuilder()
                    .patient(null)
                    .build())
                .withMessageContaining("patient");
        }

        @Test
        void rejectsInactivePatient() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> anEnrollmentBuilder()
                    .patient(anInactivePatient())
                    .build())
                .withMessageContaining("patient");
        }

        @Test
        void rejectsNullMedication() {
            assertThatNullPointerException()
                .isThrownBy(() -> anEnrollmentBuilder()
                    .medication(null)
                    .build())
                .withMessageContaining("medication");
        }

        @Test
        void rejectsInactiveMedication() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> anEnrollmentBuilder()
                    .medication(anInactiveMedication())
                    .build())
                .withMessageContaining("medication");
        }

    }

    @Nested
    class Approve {

        @Test
        void approvesEnrollment() {
            final LocalDate startDate = LocalDate.of(2026, 9, 25);

            Enrollment enrollment = anEnrollment();
            enrollment.approve(startDate);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(startDate);
                softly.assertThat(enrollment.getCloseDate()).isNull();
                softly.assertThat(enrollment.getCloseReason()).isNull();
                softly.assertThat(enrollment.getOtherReason()).isNull();
            });
        }

        @Test
        void approvesWithPastStartDate() {
            final LocalDate pastStartDate = LocalDate.of(2025, 9, 25);

            Enrollment enrollment = anEnrollment();
            enrollment.approve(pastStartDate);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(pastStartDate);
            });
        }

        @Test
        void ignoresRepeatedApproveWithSameStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            enrollment.approve(START_DATE);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
            });
        }

        @Test
        void rejectsAnotherStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.approve(START_DATE.plusDays(1)))
                .withMessageContaining("cannot approve");
            assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
        }

        @Test
        void rejectsNullStartDate() {
            Enrollment enrollment = anEnrollment();

            assertThatNullPointerException()
                .isThrownBy(() -> enrollment.approve(null))
                .withMessageContaining("startDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void rejectsInactivePatient() {
            Enrollment enrollment = anEnrollment();
            enrollment.getPatient().deactivate();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.approve(START_DATE))
                .withMessageContaining("patient");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void rejectsInactiveMedication() {
            Enrollment enrollment = anEnrollment();
            enrollment.getMedication().deactivate();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.approve(START_DATE))
                .withMessageContaining("medication");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void rejectsApproveForCompleted() {
            Enrollment enrollment = aCompletedEnrollment();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.approve(START_DATE))
                .withMessageContaining("cannot approve");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        }

    }

    @Nested
    class Complete {

        @Test
        void completesEnrollment() {
            final LocalDate closeDate = LocalDate.of(2026, 3, 17);
            final LocalDate today = closeDate.plusDays(1);

            Enrollment enrollment = anApprovedEnrollment();
            enrollment.complete(closeDate, today);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
                softly.assertThat(enrollment.getCloseDate()).isEqualTo(closeDate);
                softly.assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.THERAPY_COMPLETED);
                softly.assertThat(enrollment.getOtherReason()).isNull();
            });
        }

        @Test
        void completesOnStartDate() {
            Enrollment enrollment = anApprovedEnrollment();
            enrollment.complete(START_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(START_DATE);
        }

        @Test
        void completesToday() {
            Enrollment enrollment = anApprovedEnrollment();

            enrollment.complete(TODAY, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(TODAY);
        }

        @Test
        void ignoresRepeatedCompleteWithSameCloseDate() {
            Enrollment enrollment = aCompletedEnrollment();

            enrollment.complete(CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
        }

        @Test
        void rejectsCloseDateBeforeStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.complete(
                    START_DATE.minusDays(1),
                    TODAY))
                .withMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void rejectsCloseDateInFuture() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.complete(
                    TODAY.plusDays(1),
                    TODAY))
                .withMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void rejectsNullCloseDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatNullPointerException()
                .isThrownBy(() -> enrollment.complete(null, TODAY))
                .withMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void rejectsNullToday() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatNullPointerException()
                .isThrownBy(() -> enrollment.complete(CLOSE_DATE, null))
                .withMessageContaining("today");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void rejectsAnotherCloseDate() {
            final LocalDate anotherCloseDate = CLOSE_DATE.minusDays(1);

            Enrollment enrollment = aCompletedEnrollment();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.complete(
                    anotherCloseDate,
                    TODAY))
                .withMessageContaining("cannot complete");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
        }

        @Test
        void completesWithInactivePatient() {
            Enrollment enrollment = anApprovedEnrollmentWithInactivePatient();

            enrollment.complete(CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
        }

        @Test
        void completesWithInactiveMedication() {
            Enrollment enrollment = anApprovedEnrollmentWithInactiveMedication();

            enrollment.complete(CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
        }

        @Test
        void completesWithInactiveRelations() {
            Enrollment enrollment = anApprovedEnrollmentWithInactiveRelations();

            enrollment.complete(CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
        }

        @Test
        void rejectsCompleteForPending() {
            Enrollment enrollment = anEnrollment();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.complete(
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("cannot complete");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

    }

    @Nested
    class Reject {

        @Test
        void rejectsPendingEnrollment() {
            Enrollment enrollment = aRejectedEnrollment();

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.REJECTED);
                softly.assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
                softly.assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.NOT_ELIGIBLE);
                softly.assertThat(enrollment.getOtherReason()).isNull();
            });
        }

        @Test
        void rejectsWithOtherReason() {
            Enrollment enrollment = aRejectedEnrollmentOtherReason();

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.REJECTED);
                softly.assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
                softly.assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
                softly.assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
            });
        }

        @Test
        void refusesIncompatibleReason() {
            Enrollment enrollment = anEnrollment();
            Closure closure = new Closure(CloseReason.THERAPY_COMPLETED, null);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.reject(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("is not allowed");
            assertStillPending(enrollment);
        }

        @Test
        void ignoresRepeatedRejectWithSameCloseDate() {
            Enrollment enrollment = aRejectedEnrollment();
            enrollment.reject(
                REJECT_CLOSURE,
                CLOSE_DATE,
                TODAY
            );

            assertDefaultRejected(enrollment);
        }

        @Test
        void refusesRepeatedRejectWithAnotherReason() {
            Enrollment enrollment = aRejectedEnrollment();

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.reject(
                    REJECT_CLOSURE_OTHER,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("cannot reject");
            assertDefaultRejected(enrollment);
        }

        @Test
        void refusesCloseDateInFuture() {
            Enrollment enrollment = anEnrollment();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.reject(
                    REJECT_CLOSURE,
                    TODAY.plusDays(1),
                    TODAY))
                .withMessageContaining("closeDate")
                .withMessageContaining("future");
            assertStillPending(enrollment);
        }

        @Test
        void rejectsForInactivePatient() {
            Enrollment enrollment = anEnrollmentWithInactivePatient();
            enrollment.reject(REJECT_CLOSURE, CLOSE_DATE, TODAY);

            assertDefaultRejected(enrollment);
        }

        @Test
        void rejectsForInactiveMedication() {
            Enrollment enrollment = anEnrollmentWithInactiveMedication();
            enrollment.reject(REJECT_CLOSURE, CLOSE_DATE, TODAY);

            assertDefaultRejected(enrollment);
        }

        static void assertDefaultRejected(Enrollment enrollment) {
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.REJECTED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.NOT_ELIGIBLE);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
            assertThat(enrollment.getOtherReason()).isNull();
        }

        static void assertStillPending(Enrollment enrollment) {
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
            assertThat(enrollment.getStartDate()).isNull();
            assertThat(enrollment.getCloseDate()).isNull();
            assertThat(enrollment.getCloseReason()).isNull();
            assertThat(enrollment.getOtherReason()).isNull();
        }

    }

}
