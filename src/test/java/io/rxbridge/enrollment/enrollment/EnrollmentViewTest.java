package io.rxbridge.enrollment.enrollment;

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
                enrollment.getMedication().getName(),
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
        assertThat(view.closeDate()).isEqualTo(CLOSE_DATE);
    }

    private Enrollment withId(Enrollment enrollment) {
        ReflectionTestUtils.setField(enrollment, "id", ENROLLMENT_ID);
        return enrollment;
    }

}
