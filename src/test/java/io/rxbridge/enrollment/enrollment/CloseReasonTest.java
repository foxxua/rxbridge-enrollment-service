package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class CloseReasonTest {

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource(delimiter = '|', textBlock = """
        MEDICATION_DEACTIVATED | PENDING   | false
        MEDICATION_DEACTIVATED | APPROVED  | false
        MEDICATION_DEACTIVATED | COMPLETED | false
        MEDICATION_DEACTIVATED | CANCELLED | true
        MEDICATION_DEACTIVATED | REJECTED  | false
        NOT_ELIGIBLE           | PENDING   | false
        NOT_ELIGIBLE           | APPROVED  | false
        NOT_ELIGIBLE           | COMPLETED | false
        NOT_ELIGIBLE           | CANCELLED | false
        NOT_ELIGIBLE           | REJECTED  | true
        PATIENT_DEACTIVATED    | PENDING   | false
        PATIENT_DEACTIVATED    | APPROVED  | false
        PATIENT_DEACTIVATED    | COMPLETED | false
        PATIENT_DEACTIVATED    | CANCELLED | true
        PATIENT_DEACTIVATED    | REJECTED  | false
        THERAPY_COMPLETED      | PENDING   | false
        THERAPY_COMPLETED      | APPROVED  | false
        THERAPY_COMPLETED      | COMPLETED | true
        THERAPY_COMPLETED      | CANCELLED | false
        THERAPY_COMPLETED      | REJECTED  | false
        OTHER                  | PENDING   | false
        OTHER                  | APPROVED  | false
        OTHER                  | COMPLETED | false
        OTHER                  | CANCELLED | true
        OTHER                  | REJECTED  | true
        """)
    void allowsCloseReasonForStatus(
        CloseReason reason,
        EnrollmentStatus status,
        boolean expected
    ) {
        assertThat(reason.isAllowedFor(status)).isEqualTo(expected);
    }

}
