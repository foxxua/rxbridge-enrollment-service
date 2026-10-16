package io.rxbridge.enrollment.patient;

public final class PatientConstants {

    // Person Name
    public static final int NAME_MAX_LENGTH = 100;

    // Address
    public static final int STREET_MAX_LENGTH = 100;
    public static final int CITY_MAX_LENGTH = 100;
    public static final int COUNTRY_MAX_LENGTH = 2;
    public static final int REGION_MAX_LENGTH = 100;
    public static final int ZIPCODE_MAX_LENGTH = 16;

    // Patient
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int SEX_MAX_LENGTH = 20;

    // Regex patterns
    public static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    public static final String COUNTRY_PATTERN = "[A-Z]{2}";

    private PatientConstants() { }

}
