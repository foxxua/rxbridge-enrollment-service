package io.rxbridge.enrollment.medication;

import static io.rxbridge.enrollment.medication.MedicationConstants.*;

public final class MedicationFixtures {

    private static final String DEFAULT_NAME         = "Aspirin";
    private static final String DEFAULT_DESCRIPTION  = "A nonsteroidal anti-inflammatory drug " +
        "used to reduce pain, fever, and inflammation, as well as to prevent blood clots.";
    private static final String DEFAULT_CODE         = "M000001";
    private static final String DEFAULT_SIDE_EFFECTS = """
        It can cause irritation and bleeding in the stomach lining.
        Long-term use may increase the risk of gastrointestinal ulcers.
        It can trigger allergic reactions or asthma attacks in sensitive individuals.
        """;

    private MedicationFixtures() { }

    public static MedicationBuilder aMedicationBuilder() {
        return new MedicationBuilder();
    }

    public static Medication aMedication() {
        return aMedicationBuilder().build();
    }

    public static Medication anInactiveMedication() {
        Medication medication = aMedication();
        medication.deactivate();

        return medication;
    }

    public static Medication anAnalginMedication() {
        return aMedicationBuilder()
            .name("Analgin")
            .description("A non-opioid pain reliever and fever reducer commonly " +
                "used for severe acute or chronic pain.")
            .code("M000002")
            .sideEffects("""
                It carries a rare but severe risk of causing agranulocytosis, which dangerously lowers white blood cell counts.
                It can cause unexpected drops in blood pressure when administered too quickly.
                Skin rashes and other hypersensitivity reactions may occur after consumption.
                """)
            .build();
    }

    public static Medication anInactiveIbuprofenMedication() {
        Medication ibuprofen = aMedicationBuilder()
            .name("Ibuprofen")
            .description("A widely used medication that helps relieve pain, " +
                "lower fever, and reduce swelling caused by various conditions.")
            .code("M000003")
            .sideEffects("""
                Regular intake can lead to heartburn, nausea, or general stomach discomfort.
                It may elevate blood pressure and increase fluid retention in the body.
                Frequent high doses can negatively affect kidney function over time.
                """)
            .build();
        ibuprofen.deactivate();

        return ibuprofen;
    }

    public static Medication aMedicationWithMaxLengthFields() {
        return aMedicationBuilder()
            .name("a".repeat(NAME_MAX_LENGTH))
            .description("b".repeat(DESCRIPTION_MAX_LENGTH))
            .code("M123456")
            .sideEffects("c".repeat(SIDE_EFFECTS_MAX_LENGTH))
            .build();
    }

    public static Medication aMedicationWithNullableFields() {
        return aMedicationBuilder()
            .description(null)
            .sideEffects(null)
            .build();
    }

    public static String aMedicationName() {
        return DEFAULT_NAME;
    }

    public static String aMedicationDescription() {
        return DEFAULT_DESCRIPTION;
    }

    public static String aMedicationCode() {
        return DEFAULT_CODE;
    }

    public static String aMedicationSideEffects() {
        return DEFAULT_SIDE_EFFECTS;
    }

    public static final class MedicationBuilder {

        private String name        = aMedicationName();
        private String description = aMedicationDescription();
        private String code        = aMedicationCode();
        private String sideEffects = aMedicationSideEffects();

        private MedicationBuilder() { }

        public MedicationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MedicationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MedicationBuilder code(String code) {
            this.code = code;
            return this;
        }

        public MedicationBuilder sideEffects(String sideEffects) {
            this.sideEffects = sideEffects;
            return this;
        }

        public Medication build() {
            return new Medication(
                this.name,
                this.description,
                this.code,
                this.sideEffects
            );
        }

    }
}
