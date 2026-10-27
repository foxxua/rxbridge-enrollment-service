package io.rxbridge.enrollment.enrollment;

import java.util.Objects;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;

public record Closure(
    CloseReason closeReason,
    String otherReason
) {

    public Closure {
        Objects.requireNonNull(closeReason, "closeReason");
        otherReason = (closeReason == CloseReason.OTHER)
            ? requireText(otherReason, "otherReason", OTHER_REASON_MAX_LENGTH)
            : requireAbsent(otherReason, "otherReason");
    }

}
