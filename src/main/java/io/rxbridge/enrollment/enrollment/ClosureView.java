package io.rxbridge.enrollment.enrollment;

import java.time.LocalDate;

public record ClosureView(
    CloseReason closeReason,
    String otherReason,
    LocalDate closeDate
) { }
