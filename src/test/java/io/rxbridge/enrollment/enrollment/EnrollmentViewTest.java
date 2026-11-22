package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.medication.MedicationView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

public class EnrollmentViewTest {

    @Test
    void approvedEnrollmentHasNoClosingDate() {
        Enrollment enrollment = withId(
            anApprovedEnrollment(),
            ENROLLMENT_ID
        );

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
        Enrollment enrollment = withId(
            anApprovedEnrollment(),
            ENROLLMENT_ID
        );
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
        Enrollment enrollment = withId(
            aRejectedEnrollmentOtherReason(),
            ENROLLMENT_ID
        );

        EnrollmentView view = EnrollmentView.from(enrollment);

        assertThat(view.closure()).isEqualTo(
            new ClosureView(
                CloseReason.OTHER,
                OTHER_REASON,
                CLOSE_DATE
            )
        );
    }

}
