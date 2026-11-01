package io.rxbridge.enrollment.enrollment;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum EnrollmentStatus {

    PENDING,
    APPROVED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    private static final Set<EnrollmentStatus> OPEN = Arrays.stream(values())
        .filter(EnrollmentStatus::isOpen)
        .collect(Collectors.toUnmodifiableSet());

    public static Set<EnrollmentStatus> openStatuses() {
        return OPEN;
    }

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
