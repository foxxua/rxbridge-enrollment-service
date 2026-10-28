package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.function.Consumer;

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
                    CLOSURE_OTHER,
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

    }

    @Nested
    class Cancel {

        @Test
        void cancelsPendingEnrollment() {
            Enrollment enrollment = aCancelledEnrollmentFromPending();

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
                softly.assertThat(enrollment.getStartDate()).isNull();
                softly.assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
                softly.assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
                softly.assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
            });
        }

        @Test
        void cancelsApprovedEnrollment() {
            Enrollment enrollment = aCancelledEnrollmentFromApproved();

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
                softly.assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
                softly.assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
                softly.assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
            });
        }

        @Test
        void refusesCloseDateBeforeStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.cancel(
                    CLOSURE_OTHER,
                    START_DATE.minusDays(1),
                    TODAY))
                .withMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void refusesCloseDateInFuture() {
            Enrollment enrollment = anEnrollment();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.cancel(
                    CLOSURE_OTHER,
                    TODAY.plusDays(1),
                    TODAY))
                .withMessageContaining("closeDate");
            assertStillPending(enrollment);
        }

        @Test
        void refusesIncompatibleReason() {
            Enrollment enrollment = anEnrollment();
            Closure closure = new Closure(CloseReason.NOT_ELIGIBLE, null);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("is not allowed");
            assertStillPending(enrollment);
        }

        @Test
        void ignoresRepeatedCancelWithSameCloseDate() {
            Enrollment enrollment = aCancelledEnrollmentFromApproved();
            enrollment.cancel(
                CLOSURE_OTHER,
                CLOSE_DATE,
                TODAY
            );

            assertDefaultCancelled(enrollment);
        }

        @Test
        void refusesRepeatedCancelWithAnotherReason() {
            Enrollment enrollment = aCancelledEnrollmentFromPending();
            Closure closure = new Closure(CloseReason.OTHER, "Another reason");

            assertThatIllegalStateException()
                .isThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("cannot cancel");
            assertDefaultCancelled(enrollment);
        }

        @Test
        void cancelsForInactivePatient() {
            Enrollment enrollment = anApprovedEnrollmentWithInactivePatient();
            Closure closure = new Closure(CloseReason.PATIENT_DEACTIVATED, null);
            enrollment.cancel(closure, CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.PATIENT_DEACTIVATED);
        }

        @Test
        void cancelsForInactiveMedication() {
            Enrollment enrollment = anApprovedEnrollmentWithInactiveMedication();
            Closure closure = new Closure(CloseReason.MEDICATION_DEACTIVATED, null);
            enrollment.cancel(closure, CLOSE_DATE, TODAY);

            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.MEDICATION_DEACTIVATED);
        }

        @Test
        void refusesPatientDeactivatedForActivePatient() {
            Enrollment enrollment = anApprovedEnrollment();
            Closure closure = new Closure(CloseReason.PATIENT_DEACTIVATED, null);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("is active");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void refusesMedicationDeactivatedForActiveMedication() {
            Enrollment enrollment = anApprovedEnrollment();
            Closure closure = new Closure(CloseReason.MEDICATION_DEACTIVATED, null);

            assertThatIllegalArgumentException()
                .isThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .withMessageContaining("is active");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        static void assertDefaultCancelled(Enrollment enrollment) {
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
            assertThat(enrollment.getCloseDate()).isEqualTo(CLOSE_DATE);
            assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
        }

    }

    @Nested
    class Transitions {

        enum Transition {

            APPROVE (e -> e.approve(START_DATE)),
            COMPLETE(e -> e.complete(CLOSE_DATE, TODAY)),
            REJECT  (e -> e.reject(REJECT_CLOSURE, CLOSE_DATE, TODAY)),
            CANCEL  (e -> e.cancel(CLOSURE_OTHER, CLOSE_DATE, TODAY));

            private final Consumer<Enrollment> action;

            Transition(Consumer<Enrollment> action) {
                this.action = action;
            }

            void applyTo(Enrollment enrollment) {
                this.action.accept(enrollment);
            }

        }

        private record Snapshot(
            EnrollmentStatus status,
            LocalDate startDate,
            LocalDate closeDate,
            CloseReason closeReason,
            String otherReason
        ) {

            static Snapshot of(Enrollment enrollment) {
                return new Snapshot(
                    enrollment.getStatus(),
                    enrollment.getStartDate(),
                    enrollment.getCloseDate(),
                    enrollment.getCloseReason(),
                    enrollment.getOtherReason()
                );
            }

        }

        @ParameterizedTest(name = "{1} from {0} -> {2}")
        @CsvSource(delimiter = '|', textBlock = """
            # given    |  transition  |  outcome #
            # ---------------------------------- #
            PENDING    |  APPROVE     |  APPROVED
            PENDING    |  COMPLETE    |  refused
            PENDING    |  REJECT      |  REJECTED
            PENDING    |  CANCEL      |  CANCELLED
            APPROVED   |  APPROVE     |  APPROVED
            APPROVED   |  COMPLETE    |  COMPLETED
            APPROVED   |  REJECT      |  refused
            APPROVED   |  CANCEL      |  CANCELLED
            COMPLETED  |  APPROVE     |  refused
            COMPLETED  |  COMPLETE    |  COMPLETED
            COMPLETED  |  REJECT      |  refused
            COMPLETED  |  CANCEL      |  refused
            REJECTED   |  APPROVE     |  refused
            REJECTED   |  COMPLETE    |  refused
            REJECTED   |  REJECT      |  REJECTED
            REJECTED   |  CANCEL      |  refused
            CANCELLED  |  APPROVE     |  refused
            CANCELLED  |  COMPLETE    |  refused
            CANCELLED  |  REJECT      |  refused
            CANCELLED  |  CANCEL      |  CANCELLED
            """)
        void followsTransitionTable(EnrollmentStatus given, Transition transition, String outcome) {
            Enrollment enrollment = anEnrollmentIn(given);

            if (outcome.equals("refused")) {
                Snapshot before = Snapshot.of(enrollment);

                assertThatIllegalStateException()
                    .isThrownBy(() -> transition.applyTo(enrollment));
                assertThat(Snapshot.of(enrollment)).isEqualTo(before);
            } else {
                transition.applyTo(enrollment);

                assertThat(enrollment.getStatus())
                    .isEqualTo(EnrollmentStatus.valueOf(outcome));
            }
        }

    }

    static void assertStillPending(Enrollment enrollment) {
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollment.getStartDate()).isNull();
        assertThat(enrollment.getCloseDate()).isNull();
        assertThat(enrollment.getCloseReason()).isNull();
        assertThat(enrollment.getOtherReason()).isNull();
    }

}
