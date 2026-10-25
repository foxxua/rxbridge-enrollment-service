package io.rxbridge.enrollment.enrollment;

public enum EnrollmentStatus {

    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public boolean isStart() {
        return this == PENDING;
    }

    public boolean isClose() {
        return this == COMPLETED
            || this == CANCELLED
            || this == REJECTED;
    }

}
