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

}
