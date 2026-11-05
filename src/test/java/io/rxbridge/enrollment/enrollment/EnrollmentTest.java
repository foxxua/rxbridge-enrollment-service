package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.rxbridge.enrollment.medication.MedicationNotActiveException;
import io.rxbridge.enrollment.patient.PatientNotActiveException;
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
            assertThatThrownBy(() -> anEnrollmentBuilder()
                    .patient(anInactivePatient())
                    .build())
                .isInstanceOf(PatientNotActiveException.class)
                .hasMessageContaining("Patient")
                .hasMessageContaining("not active");
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
            assertThatThrownBy(() -> anEnrollmentBuilder()
                    .medication(anInactiveMedication())
                    .build())
                .isInstanceOf(MedicationNotActiveException.class)
                .hasMessageContaining("Medication")
                .hasMessageContaining("not active");
        }

    }

    @Nested
    class Approve {

        @Test
        void approvesEnrollment() {
            final LocalDate startDate = LocalDate.of(2026, 9, 25);

            Enrollment enrollment = anEnrollment();
            enrollment.approve(startDate, TODAY);

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
            enrollment.approve(pastStartDate, TODAY);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(pastStartDate);
            });
        }

        @Test
        void approvesWithStartDateEqualsToday() {
            Enrollment enrollment = anEnrollment();
            enrollment.approve(TODAY, TODAY);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(TODAY);
            });
        }

        @Test
        void rejectsStartDateInFuture() {
            Enrollment enrollment = anEnrollment();

            assertThatThrownBy(() -> enrollment.approve(TODAY.plusDays(1), TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("startDate");
            assertStillPending(enrollment);
        }

        @Test
        void ignoresRepeatedApproveWithSameStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            enrollment.approve(START_DATE, TODAY);

            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
            });
        }

        @Test
        void ignoresRepeatedApproveWithSameStartDateAndAnotherToday() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatCode(() -> enrollment.approve(START_DATE, START_DATE.minusDays(1)))
                .doesNotThrowAnyException();
            assertSoftly(softly -> {
                softly.assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
                softly.assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
            });
        }

        @Test
        void rejectsAnotherStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatThrownBy(() -> enrollment.approve(START_DATE.plusDays(1), TODAY))
                .isInstanceOf(InvalidEnrollmentTransitionException.class)
                .hasMessageContaining("Cannot approve");
            assertThat(enrollment.getStartDate()).isEqualTo(START_DATE);
        }

        @Test
        void rejectsNullStartDate() {
            Enrollment enrollment = anEnrollment();

            assertThatNullPointerException()
                .isThrownBy(() -> enrollment.approve(null, TODAY))
                .withMessageContaining("startDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void rejectsInactivePatient() {
            Enrollment enrollment = anEnrollment();
            enrollment.getPatient().deactivate();

            assertThatThrownBy(() -> enrollment.approve(START_DATE, TODAY))
                .isInstanceOf(PatientNotActiveException.class)
                .hasMessageContaining("Patient")
                .hasMessageContaining("not active");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        void rejectsInactiveMedication() {
            Enrollment enrollment = anEnrollment();
            enrollment.getMedication().deactivate();

            assertThatThrownBy(() -> enrollment.approve(START_DATE, TODAY))
                .isInstanceOf(MedicationNotActiveException.class)
                .hasMessageContaining("Medication")
                .hasMessageContaining("not active");
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

            assertThatThrownBy(() -> enrollment.complete(
                    START_DATE.minusDays(1),
                    TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void rejectsCloseDateInFuture() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatThrownBy(() -> enrollment.complete(
                    TOMORROW,
                    TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("closeDate");
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

            assertThatThrownBy(() -> enrollment.complete(
                    anotherCloseDate,
                    TODAY))
                .isInstanceOf(InvalidEnrollmentTransitionException.class)
                .hasMessageContaining("Cannot complete");
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

            assertThatThrownBy(() -> enrollment.reject(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(CloseReasonNotAllowedException.class)
                .hasMessageContaining("is not allowed");
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

            assertThatThrownBy(() -> enrollment.reject(
                    CLOSURE_OTHER,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(InvalidEnrollmentTransitionException.class)
                .hasMessageContaining("Cannot reject");
            assertDefaultRejected(enrollment);
        }

        @Test
        void refusesCloseDateInFuture() {
            Enrollment enrollment = anEnrollment();

            assertThatThrownBy(() -> enrollment.reject(
                    REJECT_CLOSURE,
                    TOMORROW,
                    TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("closeDate")
                .hasMessageContaining("future");
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
        void approvesAndCancelsInSameDate() {
            Enrollment enrollment = anEnrollment();

            assertThatCode(() -> {
                    enrollment.approve(TODAY, TODAY);
                    enrollment.cancel(CLOSURE_OTHER, TODAY, TODAY);
            }).doesNotThrowAnyException();
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
            assertThat(enrollment.getCloseReason()).isEqualTo(CloseReason.OTHER);
            assertThat(enrollment.getCloseDate()).isEqualTo(TODAY);
            assertThat(enrollment.getOtherReason()).isEqualTo(OTHER_REASON);
        }

        @Test
        void refusesCloseDateBeforeStartDate() {
            Enrollment enrollment = anApprovedEnrollment();

            assertThatThrownBy(() -> enrollment.cancel(
                    CLOSURE_OTHER,
                    START_DATE.minusDays(1),
                    TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("closeDate");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void refusesCloseDateInFuture() {
            Enrollment enrollment = anEnrollment();

            assertThatThrownBy(() -> enrollment.cancel(
                    CLOSURE_OTHER,
                    TOMORROW,
                    TODAY))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("closeDate");
            assertStillPending(enrollment);
        }

        @Test
        void refusesIncompatibleReason() {
            Enrollment enrollment = anEnrollment();
            Closure closure = new Closure(CloseReason.NOT_ELIGIBLE, null);

            assertThatThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(CloseReasonNotAllowedException.class)
                .hasMessageContaining("is not allowed");
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

            assertThatThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(InvalidEnrollmentTransitionException.class)
                .hasMessageContaining("Cannot cancel");
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

            assertThatThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(CloseReasonMismatchException.class)
                .hasMessageContaining("is active");
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
            assertThat(enrollment.getCloseDate()).isNull();
        }

        @Test
        void refusesMedicationDeactivatedForActiveMedication() {
            Enrollment enrollment = anApprovedEnrollment();
            Closure closure = new Closure(CloseReason.MEDICATION_DEACTIVATED, null);

            assertThatThrownBy(() -> enrollment.cancel(
                    closure,
                    CLOSE_DATE,
                    TODAY))
                .isInstanceOf(CloseReasonMismatchException.class)
                .hasMessageContaining("is active");
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

            APPROVE (e -> e.approve(START_DATE, TODAY)),
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

                assertThatThrownBy(() -> transition.applyTo(enrollment))
                    .isInstanceOf(InvalidEnrollmentTransitionException.class);
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
