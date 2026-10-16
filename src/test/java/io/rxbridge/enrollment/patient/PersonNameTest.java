package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.patient.PatientConstants.*;

class PersonNameTest {

    static final String FIRST_NAME = "William";
    static final String LAST_NAME = "Shakespeare";

    @Nested
    class Creation {

        @Test
        void createsValidPersonName() {
            PersonName personName = new PersonName(FIRST_NAME, LAST_NAME);

            assertSoftly(softly -> {
                softly.assertThat(personName.firstName()).isEqualTo("William");
                softly.assertThat(personName.lastName()).isEqualTo("Shakespeare");
                softly.assertThat(personName.fullName()).isEqualTo("William Shakespeare");
            });
        }

        @Test
        void acceptsMaxLengthInEveryField() {
            PersonName personName = new PersonName(
                "a".repeat(NAME_MAX_LENGTH),
                "b".repeat(NAME_MAX_LENGTH)
            );

            assertSoftly(softly -> {
                softly.assertThat(personName.firstName()).hasSize(NAME_MAX_LENGTH);
                softly.assertThat(personName.lastName()).hasSize(NAME_MAX_LENGTH);
            });
        }

        @Test
        void normalizesAllFields() {
            assertThat(new PersonName(" \t \n William \n \t ", " \n \t Shakespeare \t \n "))
                .isEqualTo(new PersonName("William", "Shakespeare"));
        }

        @Test
        void personNamesWithSameValuesAreEqual() {
            assertThat(new PersonName(FIRST_NAME, LAST_NAME))
                .isEqualTo(new PersonName("William", "Shakespeare"))
                .hasSameHashCodeAs(new PersonName("William", "Shakespeare"));
        }

    }

    @Nested
    class FirstName {

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new PersonName(
                    "a".repeat(NAME_MAX_LENGTH + 1),
                    LAST_NAME))
                .withMessageContaining("firstName");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new PersonName(null, LAST_NAME))
                .withMessageContaining("firstName");
        }

        @Test
        void rejectsBlank() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new PersonName("   ", LAST_NAME))
                .withMessageContaining("firstName");
        }

    }

    @Nested
    class LastName {

        @Test
        void rejectsTooLong() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new PersonName(
                    FIRST_NAME,
                    "a".repeat(NAME_MAX_LENGTH + 1)))
                .withMessageContaining("lastName");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new PersonName(FIRST_NAME, null))
                .withMessageContaining("lastName");
        }

        @Test
        void rejectsBlank() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new PersonName(FIRST_NAME, "   "))
                .withMessageContaining("lastName");
        }

    }

    @Nested
    class FullName {

        @ParameterizedTest
        @CsvSource({
            "William, Shakespeare, William Shakespeare",
            "Дар'я, Сироїд, Дар'я Сироїд",
            "Honoré, de Balzac, Honoré de Balzac",
            "Antoine, de Saint-Exupéry, Antoine de Saint-Exupéry"
        })
        void joinsFirstAndLastNameWithSpace(String firstName, String lastName, String expected) {
            assertThat(new PersonName(firstName, lastName).fullName())
                .isEqualTo(expected);
        }

    }

}
