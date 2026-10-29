package io.rxbridge.enrollment.enrollment;

public enum EnrollmentStatus {

    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public boolean isOpen() {
        return switch (this) {
            case PENDING, APPROVED              -> true;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };
    }

    public boolean canMoveTo(EnrollmentStatus target) {
        return switch (this) {
            case PENDING                         -> target == APPROVED || target == CANCELLED || target == REJECTED;
            case APPROVED                        -> target == CANCELLED || target == COMPLETED;
            case COMPLETED, CANCELLED, REJECTED  -> false;
        };
    }

}
