package io.rxbridge.enrollment.medication;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.medication.MedicationConstants.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

class MedicationTest {

    @Nested
    class Creation {

        @Test
        void createsActiveMedication() {
            assertThat(aMedication().isActive()).isTrue();
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
            ' m999999 ' | M999999
            M123456     | M123456
            """)
        void createsValidMedicationStripValues(String input, String expectedCode) {
            Medication medication = aMedicationBuilder()
                .name(" Neuro-Glitchin ")
                .description("A futuristic bio-synthetic stimulant used to temporarily " +
                    "overclock human neural pathways for rapid data processing and cybernetic " +
                    "enhancement.   ")
                .code(input)
                .sideEffects("""
                    \t
                    It can cause severe sensory overload and painful phantom audio glitches in the user's ears.
                    Sudden depletion of neurotransmitters may lead to unexpected emotional crashes and temporary memory wipes.
                    Chronic usage risks triggering permanent desynchronization between the biological brain and digital implants.
                    """)
                .build();

            assertSoftly(softly -> {
                softly.assertThat(medication.getName()).isEqualTo("Neuro-Glitchin");
                softly.assertThat(medication.getDescription()).isEqualTo(
                    "A futuristic bio-synthetic stimulant used to temporarily overclock human " +
                    "neural pathways for rapid data processing and cybernetic enhancement."
                );
                softly.assertThat(medication.getCode()).isEqualTo(expectedCode);
                softly.assertThat(medication.getSideEffects()).isEqualTo(
                    "It can cause severe sensory overload and painful phantom audio glitches in the user's ears.\n" +
                        "Sudden depletion of neurotransmitters may lead to unexpected emotional crashes and temporary memory wipes.\n" +
                        "Chronic usage risks triggering permanent desynchronization between the biological brain and digital implants."
                );
            });
        }

        @Test
        void acceptsMaxLength() {
            Medication medication = aMedicationBuilder()
                .name("a".repeat(NAME_MAX_LENGTH))
                .description("b".repeat(DESCRIPTION_MAX_LENGTH))
                .code("M111111")
                .sideEffects("c".repeat(SIDE_EFFECTS_MAX_LENGTH))
                .build();

            assertSoftly(softly -> {
                softly.assertThat(medication.getName()).hasSize(NAME_MAX_LENGTH);
                softly.assertThat(medication.getDescription()).hasSize(DESCRIPTION_MAX_LENGTH);
                softly.assertThat(medication.getCode()).hasSize(CODE_LENGTH);
                softly.assertThat(medication.getSideEffects()).hasSize(SIDE_EFFECTS_MAX_LENGTH);
            });
        }

    }

    @Nested
    class Name {

        @Test
        void rejectsBlank() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .name("   ")
                    .build())
                .withMessageContaining("name");
        }

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .name("a".repeat(NAME_MAX_LENGTH + 1))
                    .build())
                .withMessageContaining("name");
        }

        @Test
        void renamesWithValidValue() {
            Medication medication = aMedication();
            medication.rename("Analgin");

            assertThat(medication.getName()).isEqualTo("Analgin");
        }

        @Test
        void renamesWithMaxLengthValue() {
            Medication medication = aMedication();
            medication.rename("a".repeat(NAME_MAX_LENGTH));

            assertThat(medication.getName()).hasSize(NAME_MAX_LENGTH);

        }

        @Test
        void keepsOldNameWhenNewOneIsBlank() {
            Medication medication = aMedication();
            String nameBefore = medication.getName();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> medication.rename("   "))
                .withMessageContaining("name");
            assertThat(medication.getName()).isEqualTo(nameBefore);
        }

        @Test
        void keepsOldNameWhenNewOneIsTooLong() {
            Medication medication = aMedication();
            String nameBefore = medication.getName();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> medication.rename("a".repeat(NAME_MAX_LENGTH + 1)))
                .withMessageContaining("name");
            assertThat(medication.getName()).isEqualTo(nameBefore);
        }

    }

    @Nested
    class Code {

        @Test
        void rejectsBlank() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .code("   ")
                    .build())
                .withMessageContaining("code");
        }

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .code("M1234567")
                    .build())
                .withMessageContaining("code");
        }

        @Test
        void rejectsTooShort() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .code("M12345")
                    .build())
                .withMessageContaining("code");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "M123456", "M000000", "M999999"
        })
        void acceptsValidCode(String input) {
            assertThat(aMedicationBuilder()
                    .code(input)
                    .build().getCode())
                .isEqualTo(input);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "1111111", "MMMMMMM", "1MMMMMM", "A123456"
        })
        void rejectsInvalid(String input) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .code(input)
                    .build())
                .withMessageContaining("code");
        }

    }

    @Nested
    class Description {

        @Test
        void acceptsBlank() {
            assertThat(aMedicationBuilder()
                    .description("   ")
                    .build().getDescription())
                .isNull();
        }

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .description("a".repeat(DESCRIPTION_MAX_LENGTH + 1))
                    .build())
                .withMessageContaining("description");
        }

        @Test
        void changesDescriptionWithValidValue() {
            Medication medication = aMedication();
            medication.changeDescription("A non-opioid pain reliever and fever reducer commonly " +
                "used for severe acute or chronic pain.");

            assertThat(medication.getDescription())
                .isEqualTo("A non-opioid pain reliever and fever reducer commonly " +
                    "used for severe acute or chronic pain.");
        }

        @Test
        void changesDescriptionWithBlank() {
            Medication medication = aMedication();
            medication.changeDescription("   ");

            assertThat(medication.getDescription()).isNull();
        }

        @Test
        void changesDescriptionWithMaxLengthValue() {
            Medication medication = aMedication();
            medication.changeDescription("a".repeat(DESCRIPTION_MAX_LENGTH));

            assertThat(medication.getDescription()).hasSize(DESCRIPTION_MAX_LENGTH);
        }

        @Test
        void keepsOldDescriptionWhenNewOneIsTooLong() {
            Medication medication = aMedication();
            String oldDescription = medication.getDescription();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> medication.changeDescription("a".repeat(DESCRIPTION_MAX_LENGTH + 1)))
                .withMessageContaining("description");
            assertThat(medication.getDescription()).isEqualTo(oldDescription);
        }

    }

    @Nested
    class SideEffects {

        @Test
        void acceptsBlank() {
            assertThat(aMedicationBuilder()
                .sideEffects("   ")
                .build().getSideEffects())
                .isNull();
        }

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aMedicationBuilder()
                    .sideEffects("a".repeat(SIDE_EFFECTS_MAX_LENGTH + 1))
                    .build())
                .withMessageContaining("sideEffects");
        }

        @Test
        void changesSideEffectsWithValidValue() {
            Medication medication = aMedication();
            medication.changeSideEffects("""
                Regular intake can lead to heartburn, nausea, or general stomach discomfort.
                It may elevate blood pressure and increase fluid retention in the body.
                Frequent high doses can negatively affect kidney function over time.
                """);

            assertThat(medication.getSideEffects())
                .isEqualTo("Regular intake can lead to heartburn, nausea, or general stomach discomfort.\n" +
                    "It may elevate blood pressure and increase fluid retention in the body.\n" +
                    "Frequent high doses can negatively affect kidney function over time.");
        }

        @Test
        void changesSideEffectsWithBlank() {
            Medication medication = aMedication();
            medication.changeSideEffects("   ");

            assertThat(medication.getSideEffects()).isNull();
        }

        @Test
        void changesSideEffectsWithMaxLengthValue() {
            Medication medication = aMedication();
            medication.changeSideEffects("a".repeat(SIDE_EFFECTS_MAX_LENGTH));

            assertThat(medication.getSideEffects()).hasSize(SIDE_EFFECTS_MAX_LENGTH);
        }

        @Test
        void keepsOldSideEffectsWhenNewOneIsTooLong() {
            Medication medication = aMedication();
            String oldSideEffects = medication.getSideEffects();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> medication.changeSideEffects("a".repeat(SIDE_EFFECTS_MAX_LENGTH + 1)))
                .withMessageContaining("sideEffects");
            assertThat(medication.getSideEffects()).isEqualTo(oldSideEffects);
        }

    }

    @Nested
    class Activation {

        @Test
        void deactivates() {
            Medication medication = aMedication();
            medication.deactivate();

            assertThat(medication.isActive()).isFalse();
        }

        @Test
        void staysInactiveWhenDeactivatedAgain() {
            Medication medication = anInactiveMedication();
            medication.deactivate();

            assertThat(medication.isActive()).isFalse();
        }

        @Test
        void activates() {
            Medication medication = anInactiveMedication();
            medication.activate();

            assertThat(medication.isActive()).isTrue();
        }

        @Test
        void staysActiveWhenActivatedAgain() {
            Medication medication = aMedication();
            medication.activate();

            assertThat(medication.isActive()).isTrue();
        }

    }

}
