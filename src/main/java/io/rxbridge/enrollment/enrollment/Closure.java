package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;

import java.util.Objects;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;

public record Closure(
    CloseReason closeReason,
    String otherReason
) {

    public Closure {
        Objects.requireNonNull(closeReason, "closeReason");
        otherReason = validateOtherReason(closeReason, otherReason);
    }

    private static String validateOtherReason(CloseReason closeReason, String otherReason) {
        return (closeReason == CloseReason.OTHER)
            ? requireText((otherReason == null) ? "" : otherReason,
                "otherReason",
                OTHER_REASON_MAX_LENGTH)
            : requireAbsent(otherReason, "otherReason");
    }

}
