package io.rxbridge.enrollment.enrollment;

public class EnrollmentConstants {

    // Status
    public static final int STATUS_MAX_LENGTH = 20;

    // Close reason
    public static final int CLOSE_REASON_MAX_LENGTH = 40;

    // Other reason
    public static final int OTHER_REASON_MAX_LENGTH = 2000;

    // Error messages
    public static final String PATIENT_NOT_ACTIVE_TEMPLATE = "patient %s is not active";
    public static final String MEDICATION_NOT_ACTIVE_TEMPLATE = "medication %s is not active";
    public static final String CANNOT_APPROVE_TEMPLATE = "cannot approve a %s enrollment";
    public static final String CANNOT_COMPLETE_TEMPLATE = "cannot complete a %s enrollment";

    private EnrollmentConstants() { }

}
