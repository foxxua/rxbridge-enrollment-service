package io.rxbridge.enrollment.enrollment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;

class ClosureTest {

    @Test
    void createsOtherReason() {
        Closure closure = new Closure(
            CloseReason.OTHER,
            "Not eligible"
        );

        assertThat(closure.closeReason()).isEqualTo(CloseReason.OTHER);
        assertThat(closure.otherReason()).isEqualTo("Not eligible");
    }

    @Test
    void stripsOtherReason() {
        Closure closure = new Closure(
            CloseReason.OTHER,
            "   Not eligible  "
        );

        assertThat(closure.closeReason()).isEqualTo(CloseReason.OTHER);
        assertThat(closure.otherReason()).isEqualTo("Not eligible");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void createsNotOtherReason(String input) {
        Closure closure = new Closure(
            CloseReason.NOT_ELIGIBLE,
            input
        );

        assertThat(closure.closeReason()).isEqualTo(CloseReason.NOT_ELIGIBLE);
        assertThat(closure.otherReason()).isEqualTo(null);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requiresOtherReason(String input) {
        assertThatThrownBy(() -> new Closure(
                CloseReason.OTHER,
                input))
            .hasMessageContaining("otherReason");
    }

    @Test
    void rejectsNonOtherWithText() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Closure(
                CloseReason.PATIENT_DEACTIVATED,
                OTHER_REASON))
            .withMessageContaining("otherReason");
    }

}
