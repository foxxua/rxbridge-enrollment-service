package io.rxbridge.enrollment.medication;

public class MedicationConstants {

    // Name
    public static final int NAME_MAX_LENGTH = 100;

    // Description
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    // Code
    public static final int CODE_LENGTH = 7;

    // Side effects
    public static final int SIDE_EFFECTS_MAX_LENGTH = 10000;

    // Regex patterns
    public static final String CODE_PATTERN = "M\\d{6}";

    private MedicationConstants() { }

}
