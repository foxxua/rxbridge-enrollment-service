package io.rxbridge.enrollment.patient;

import java.time.LocalDate;

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
    private static final LocalDate DEFAULT_BIRTHDATE  = LocalDate.of(1991, 4, 11);

    public static final LocalDate  TODAY              = LocalDate.of(2026, 10, 14);

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

    public static PersonName aPersonName() {
        return new PersonName(DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME);
    }

    public static Address anAddress() {
        return new Address(
            DEFAULT_STREET,
            DEFAULT_CITY,
            DEFAULT_COUNTRY,
            DEFAULT_REGION,
            DEFAULT_ZIPCODE
        );
    }

    public static final class PatientBuilder {

        private PersonName personName = aPersonName();
        private String email          = DEFAULT_EMAIL;
        private Address address       = anAddress();
        private Sex sex               = DEFAULT_SEX;
        private LocalDate birthdate   = DEFAULT_BIRTHDATE;
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
