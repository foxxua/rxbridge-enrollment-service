package io.rxbridge.enrollment.patient;

import java.time.LocalDate;

import static io.rxbridge.enrollment.patient.PatientConstants.*;
import static io.rxbridge.enrollment.common.TestDates.*;

public final class PatientFixtures {

    private static final String    DEFAULT_FIRST_NAME = "William";
    private static final String    DEFAULT_LAST_NAME  = "Shakespeare";
    private static final String    DEFAULT_EMAIL      = "william_shakespeare@example.com";
    private static final String    DEFAULT_STREET     = "31 Milk Street";
    private static final String    DEFAULT_CITY       = "Boston";
    private static final String    DEFAULT_COUNTRY    = "US";
    private static final String    DEFAULT_REGION     = "MA";
    private static final String    DEFAULT_ZIPCODE    = "02196";
    private static final Sex       DEFAULT_SEX        = Sex.MALE;
    public static final LocalDate  BIRTHDATE = LocalDate.of(1991, 4, 11);

    private PatientFixtures() { }

    public static PatientBuilder aPatientBuilder() {
        return new PatientBuilder();
    }

    public static Patient aPatient() {
        return aPatientBuilder().build();
    }

    public static Patient anInactivePatient() {
        Patient patient = aPatientBuilder().build();
        patient.deactivate();

        return patient;
    }

    public static Patient aFemalePatientWithoutRegion() {
        return aPatientBuilder()
            .personName(new PersonName("Agatha", "Christie"))
            .email("agatha_christie@example.com")
            .address(new Address(
                "Schönhauser Allee 36-37",
                "Berlin",
                "DE",
                null,
                "D-10435"))
            .sex(Sex.FEMALE)
            .birthdate(LocalDate.of(1995, 12, 4))
            .build();
    }

    public static Patient anInactiveUnknownSexPatient() {
        Patient patient = aPatientBuilder()
            .sex(Sex.UNKNOWN)
            .build();
        patient.deactivate();

        return patient;
    }

    public static Patient aPatientWithMaxLengthFields() {
        return aPatientBuilder()
            .personName(new PersonName(
                "a".repeat(NAME_MAX_LENGTH),
                "b".repeat(NAME_MAX_LENGTH)))
            .email("c".repeat(EMAIL_MAX_LENGTH - "@example.com".length()) + "@example.com")
            .address(new Address(
                "d".repeat(STREET_MAX_LENGTH),
                "e".repeat(CITY_MAX_LENGTH),
                "AU",
                "f".repeat(REGION_MAX_LENGTH),
                "g".repeat(ZIPCODE_MAX_LENGTH)))
            .build();
    }

    public static PersonName aPatientPersonName() {
        return new PersonName(DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME);
    }

    public static String aPatientEmail() {
        return DEFAULT_EMAIL;
    }

    public static Address aPatientAddress() {
        return new Address(
            DEFAULT_STREET,
            DEFAULT_CITY,
            DEFAULT_COUNTRY,
            DEFAULT_REGION,
            DEFAULT_ZIPCODE
        );
    }

    public static Sex aPatientSex() {
        return DEFAULT_SEX;
    }

    public static final class PatientBuilder {

        private PersonName personName = aPatientPersonName();
        private String email          = aPatientEmail();
        private Address address       = aPatientAddress();
        private Sex sex               = aPatientSex();
        private LocalDate birthdate   = BIRTHDATE;
        private LocalDate today       = TODAY;

        private PatientBuilder() { }

        public PatientBuilder personName(PersonName personName) {
            this.personName = personName;
            return this;
        }

        public PatientBuilder email(String email) {
            this.email = email;
            return this;
        }

        public PatientBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public PatientBuilder sex(Sex sex) {
            this.sex = sex;
            return this;
        }

        public PatientBuilder birthdate(LocalDate birthdate) {
            this.birthdate = birthdate;
            return this;
        }

        public PatientBuilder today(LocalDate today) {
            this.today = today;
            return this;
        }

        public  Patient build() {
            return new Patient(
                personName,
                email,
                address,
                sex,
                birthdate,
                today
            );
        }

    }

}
