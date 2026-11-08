package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.medication.MedicationView;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

public class EnrollmentViewTest {

    private static final UUID ENROLLMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Test
    void approvedEnrollmentHasNoClosingDate() {
        Enrollment enrollment = withId(anApprovedEnrollment());

        EnrollmentView view = EnrollmentView.from(enrollment);

        assertThat(view).isEqualTo(
            new EnrollmentView(
                ENROLLMENT_ID,
                MedicationView.from(enrollment.getMedication()),
                EnrollmentStatus.APPROVED,
                START_DATE,
                null
            )
        );
    }

    @Test
    void completedEnrollmentKeepsBothDates() {
        Enrollment enrollment = withId(anApprovedEnrollment());
        enrollment.complete(CLOSE_DATE, TODAY);

        EnrollmentView view = EnrollmentView.from(enrollment);

        assertThat(view.startDate()).isEqualTo(START_DATE);
        assertThat(view.closure()).isEqualTo(
            new ClosureView(
                CloseReason.THERAPY_COMPLETED,
                null,
                CLOSE_DATE
            )
        );
    }

    @Test
    void rejectedEnrollmentKeepsItsOtherReason() {
        Enrollment enrollment = withId(aRejectedEnrollmentOtherReason());

        EnrollmentView view = EnrollmentView.from(enrollment);

        assertThat(view.closure()).isEqualTo(
            new ClosureView(
                CloseReason.OTHER,
                OTHER_REASON,
                CLOSE_DATE
            )
        );
    }

    private Enrollment withId(Enrollment enrollment) {
        ReflectionTestUtils.setField(enrollment, "id", ENROLLMENT_ID);
        return enrollment;
    }

}
