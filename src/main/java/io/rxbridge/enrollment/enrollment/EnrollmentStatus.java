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

}
