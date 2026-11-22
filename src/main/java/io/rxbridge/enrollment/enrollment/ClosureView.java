package io.rxbridge.enrollment.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record ClosureView(
    CloseReason closeReason,

    @Schema(nullable = true)
    String otherReason,

    LocalDate closeDate
) { }
