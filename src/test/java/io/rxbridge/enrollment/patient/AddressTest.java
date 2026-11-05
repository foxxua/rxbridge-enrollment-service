package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.patient.PatientConstants.*;

class AddressTest {

    static final String STREET = "31 Milk Street";
    static final String CITY = "Boston";
    static final String COUNTRY = "US";
    static final String REGION = "MA";
    static final String ZIPCODE = "02196";

    @Nested
    class Creation {

        @Test
        void createsValidAddress() {
            Address address = new Address(
                STREET,
                CITY,
                COUNTRY,
                REGION,
                ZIPCODE
            );

            assertSoftly(softly -> {
                softly.assertThat(address.street()).isEqualTo("31 Milk Street");
                softly.assertThat(address.city()).isEqualTo("Boston");
                softly.assertThat(address.country()).isEqualTo("US");
                softly.assertThat(address.region()).isEqualTo("MA");
                softly.assertThat(address.zipcode()).isEqualTo("02196");
            });

        }

        @Test
        void acceptsMaxLengthInEveryField() {
            Address address = new Address(
                "a".repeat(STREET_MAX_LENGTH),
                "b".repeat(CITY_MAX_LENGTH),
                "US",
                "c".repeat(REGION_MAX_LENGTH),
                "d".repeat(ZIPCODE_MAX_LENGTH)
            );

            assertSoftly(softly -> {
                softly.assertThat(address.street()).hasSize(STREET_MAX_LENGTH);
                softly.assertThat(address.city()).hasSize(CITY_MAX_LENGTH);
                softly.assertThat(address.country()).hasSize(COUNTRY_MAX_LENGTH);
                softly.assertThat(address.region()).hasSize(REGION_MAX_LENGTH);
                softly.assertThat(address.zipcode()).hasSize(ZIPCODE_MAX_LENGTH);
            });
        }

        @Test
        void normalizesAllFields() {
            assertThat(new Address(
                    " 31 Milk Street ",
                    " Boston ",
                    "us",
                    " MA ",
                    " 02196 "))
                .isEqualTo(new Address(
                    "31 Milk Street",
                    "Boston",
                    "US",
                    "MA",
                    "02196"));
        }

    }

    @Nested
    class Street {

        @Test
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Address(
                    "a".repeat(STREET_MAX_LENGTH + 1),
                    CITY,
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("street");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Address(
                    null,
                    CITY,
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .withMessageContaining("street");
        }

        @Test
        void rejectsBlank() {
            assertThatThrownBy(() -> new Address(
                    "   ",
                    CITY,
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("street");
        }

    }

    @Nested
    class City {

        @Test
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    "a".repeat(CITY_MAX_LENGTH + 1),
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("city");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Address(
                    STREET,
                    null,
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .withMessageContaining("city");
        }

        @Test
        void rejectsBlank() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    "   ",
                    COUNTRY,
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("city");
        }

    }

    @Nested
    class Country {

        @ParameterizedTest
        @CsvSource({
            "au, AU",
            "FR, FR",
            "dE, DE",
            "' CA ', CA"
        })
        void normalizesToUpperCase(String country, String expected) {
            assertThat(new Address(
                    STREET,
                    CITY,
                    country,
                    REGION,
                    ZIPCODE).country())
                .isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"фр", "A1", "F"})
        void rejectsInvalidCode(String input) {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    input,
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("country");
        }

        @Test
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    "a".repeat(COUNTRY_MAX_LENGTH + 1),
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("country");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    null,
                    REGION,
                    ZIPCODE))
                .withMessageContaining("country");
        }

        @Test
        void rejectsBlank() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    "  ",
                    REGION,
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("country");
        }

    }

    @Nested
    class Region {

        @Test
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    "a".repeat(REGION_MAX_LENGTH + 1),
                    ZIPCODE))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("region");
        }

        @Test
        void acceptsNull() {
            assertThat(new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    null,
                    ZIPCODE).region())
                .isNull();
        }

        @Test
        void acceptsBlankAsNull() {
            assertThat(new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    "   ",
                    ZIPCODE).region())
                .isNull();
        }

    }

    @Nested
    class Zipcode {

        @Test
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    REGION,
                    "a".repeat(ZIPCODE_MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("zipcode");
        }

        @Test
        void rejectsNull() {
            assertThatNullPointerException()
                .isThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    REGION,
                    null))
                .withMessageContaining("zipcode");
        }

        @Test
        void rejectsBlank() {
            assertThatThrownBy(() -> new Address(
                    STREET,
                    CITY,
                    COUNTRY,
                    REGION,
                    "   "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("zipcode");
        }

    }

}
