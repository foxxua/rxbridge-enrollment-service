package io.rxbridge.enrollment.enrollment;

public enum CloseReason {

    MEDICATION_DEACTIVATED,
    NOT_ELIGIBLE,
    PATIENT_DEACTIVATED,
    THERAPY_COMPLETED,
    OTHER;

    public boolean isAllowedFor(EnrollmentStatus status) {
        return switch (this) {
            case PATIENT_DEACTIVATED, MEDICATION_DEACTIVATED -> status == EnrollmentStatus.CANCELLED;
            case NOT_ELIGIBLE                                -> status == EnrollmentStatus.REJECTED;
            case THERAPY_COMPLETED                           -> status == EnrollmentStatus.COMPLETED;
            case OTHER                                       -> status == EnrollmentStatus.CANCELLED
                                                             || status == EnrollmentStatus.REJECTED;
        };
    }

}
