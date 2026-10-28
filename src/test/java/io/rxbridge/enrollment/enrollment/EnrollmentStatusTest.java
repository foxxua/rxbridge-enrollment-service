package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class EnrollmentStatusTest {

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource(delimiter = '|', textBlock = """
        PENDING   | true
        APPROVED  | true
        COMPLETED | false
        CANCELLED | false
        REJECTED  | false
        """)
    void checksIsOpenState(EnrollmentStatus status, boolean expected) {
        assertThat(status.isOpen()).isEqualTo(expected);
    }

}
