package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientConstants.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;

class PatientTest {

    @Nested
    class Creation {

        @Test
        void createsActivePatient() {
            assertThat(aPatient().isActive()).isTrue();
        }

        @Test
        void rejectsMissingPersonName() {
            assertThatNullPointerException()
                .isThrownBy(() -> aPatientBuilder()
                    .personName(null)
                    .build())
                .withMessageContaining("personName");
        }

        @Test
        void rejectsMissingAddress() {
            assertThatNullPointerException()
                .isThrownBy(() -> aPatientBuilder()
                    .address(null)
                    .build())
                .withMessageContaining("address");
        }

        @Test
        void rejectsMissingSex() {
            assertThatNullPointerException()
                .isThrownBy(() -> aPatientBuilder()
                    .sex(null)
                    .build())
                .withMessageContaining("sex");
        }

        @Test
        void rejectsMissingBirthdate() {
            assertThatNullPointerException()
                .isThrownBy(() -> aPatientBuilder()
                    .birthdate(null)
                    .build())
                .withMessageContaining("birthdate");
        }

        @Test
        void rejectsBirthdateInFuture() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aPatientBuilder()
                    .birthdate(TODAY.plusDays(1))
                    .build())
                .withMessageContaining("birthdate");
        }

        @Test
        void newPatientsAreNotEqualEvenWithSameData() {
            assertThat(aPatient()).isNotEqualTo(aPatient());
        }

    }

    @Nested
    class Email {

        @Test
        void acceptsMaxLength() {
            String email = "a".repeat(EMAIL_MAX_LENGTH - "@example.com".length()) + "@example.com";
            assertThat(aPatientBuilder()
                    .email(email)
                    .build()
                    .getEmail())
                .hasSize(EMAIL_MAX_LENGTH);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "valid@example.com", "valid.email@example.com",
            "valid_email@example.com", "valid+email@example.com",
            "valid@email.example.com", "valid@example.co"
        })
        void acceptsValidEmail(String input) {
            assertThat(aPatientBuilder()
                    .email(input)
                    .build()
                    .getEmail())
                .isEqualTo(input);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalidemail", "1inv@1lid.e", "invalid@@example.com",
            "inv@lid", "12@34.56", " @. ", "in valid@example.com",
            "інвалід@example.com"
        })
        void rejectsInvalidEmail(String input) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aPatientBuilder()
                    .email(input)
                    .build())
                .withMessageContaining("email");
        }

        @Test
        void stripsEmailWhitespace() {
            assertThat(aPatientBuilder()
                .email(" \n \t william_shakespeare@example.com \t \n  ")
                .build().getEmail())
                .isEqualTo("william_shakespeare@example.com");
        }

        @Test
        void lowercasesEmail() {
            assertThat(aPatientBuilder()
                .email("WILLiam_shakespeare@example.COM")
                .build().getEmail())
                .isEqualTo("william_shakespeare@example.com");
        }

        @Test
        void rejectsMissingEmail() {
            assertThatNullPointerException()
                .isThrownBy(() -> aPatientBuilder()
                    .email(null)
                    .build())
                .withMessageContaining("email");
        }

        @Test
        void rejectsBlankEmail() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aPatientBuilder()
                    .email("   ")
                    .build())
                .withMessageContaining("email");
        }

        @Test
        void rejectsTooLong() {
            String longEmail = "a".repeat(EMAIL_MAX_LENGTH - "@example.com".length() + 1) + "@example.com";
            assertThatIllegalArgumentException()
                .isThrownBy(() -> aPatientBuilder()
                    .email(longEmail)
                    .build())
                .withMessageContaining("email");
        }

        @Test
        void changesEmail() {
            Patient patient = aPatient();
            patient.changeEmail("new.email@example.com");

            assertThat(patient.getEmail()).isEqualTo("new.email@example.com");
        }

        @Test
        void keepsOldEmailWhenNewOneIsNull() {
            Patient patient = aPatient();
            String emailBefore = patient.getEmail();

            assertThatNullPointerException()
                .isThrownBy(() -> patient.changeEmail(null))
                .withMessageContaining("email");
            assertThat(patient.getEmail()).isEqualTo(emailBefore);
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"   ", "123"})
        void keepsOldEmailWhenNewOneIsInvalid(String input) {
            Patient patient = aPatient();
            String emailBefore = patient.getEmail();

            assertThatIllegalArgumentException()
                .isThrownBy(() -> patient.changeEmail(input))
                .withMessageContaining("email");
            assertThat(patient.getEmail()).isEqualTo(emailBefore);
        }

    }

    @Nested
    class ChangingAddress {

        @Test
        void changesAddress() {
            Patient patient = aPatient();
            Address newAddress = new Address(
                "Schönhauser Allee 36-37",
                "Berlin",
                "DE",
                null,
                "D-10435"
            );
            patient.changeAddress(newAddress);

            assertThat(patient.getAddress()).isEqualTo(newAddress);
        }

        @Test
        void keepsOldAddressWhenNewOneIsNull() {
            Patient patient = aPatient();
            Address addressBefore = patient.getAddress();

            assertThatNullPointerException()
                .isThrownBy(() -> patient.changeAddress(null))
                .withMessageContaining("address");
            assertThat(patient.getAddress()).isEqualTo(addressBefore);
        }

    }

    @Nested
    class Activation {

        @Test
        void deactivatesPatient() {
            Patient patient = aPatient();
            patient.deactivate();

            assertThat(patient.isActive()).isFalse();
        }

        @Test
        void activatesPatient() {
            Patient patient = anInactivePatient();
            patient.activate();

            assertThat(patient.isActive()).isTrue();
        }

    }

}
