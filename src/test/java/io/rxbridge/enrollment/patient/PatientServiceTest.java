package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    private static final UUID PATIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    PatientRepository patients;

    BusinessCalendar calendar = businessCalendar();

    private final List<Object> published = new ArrayList<>();

    private PatientService service;

    @BeforeEach
    void setUp() {
        service = new PatientService(patients, published::add, calendar);
    }

    @Nested
    class Activate {

        @Test
        void activatesPatient() {
            Patient patient = anInactivePatient();
            given(patients.findById(PATIENT_ID)).willReturn(Optional.of(patient));

            service.activate(PATIENT_ID);

            assertThat(patient.isActive()).isTrue();
        }

        @Test
        void rejectsActivatingForUnknownPatient() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.empty());

            assertThatExceptionOfType(PatientNotFoundException.class)
                .isThrownBy(() -> service.activate(PATIENT_ID));
        }

    }

    @Nested
    class Deactivate {

        @Test
        void publishesPatientDeactivatedAfterDeactivating() {
            Patient patient = aPatient();
            given(patients.findById(PATIENT_ID)).willReturn(Optional.of(patient));
            List<Boolean> activeWhenPublished = new ArrayList<>();

            var service = new PatientService(
                patients,
                event -> {
                    published.add(event);
                    activeWhenPublished.add(patient.isActive());
                },
                calendar
            );

            service.deactivate(PATIENT_ID);

            assertThat(published).containsExactly(new PatientDeactivated(PATIENT_ID, TODAY));
            assertThat(activeWhenPublished).containsExactly(false);
        }

        @Test
        void publishesNothingWhenPatientIsAlreadyInactive() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.of(anInactivePatient()));

            service.deactivate(PATIENT_ID);

            assertThat(published).isEmpty();
        }

        @Test
        void publishesNothingForUnknownPatient() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.empty());

            assertThatExceptionOfType(PatientNotFoundException.class)
                .isThrownBy(() -> service.deactivate(PATIENT_ID));
            assertThat(published).isEmpty();
        }

    }

    @Nested
    class ChangeEmail {

        @Test
        void changesEmail() {
            Patient patient = aPatient();
            given(patients.findById(PATIENT_ID)).willReturn(Optional.of(patient));

            service.changeEmail(PATIENT_ID, "another_test_email@example.com");

            assertThat(patient.getEmail()).isEqualTo("another_test_email@example.com");
        }

        @Test
        void rejectsEmailChangeForUnknownPatient() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.empty());

            assertThatExceptionOfType(PatientNotFoundException.class)
                .isThrownBy(() -> service.changeEmail(PATIENT_ID, "another_test_email@example.com"));
        }

    }

    @Nested
    class ChangeAddress {

        @Test
        void changesAddress() {
            Patient patient = aPatient();
            given(patients.findById(PATIENT_ID)).willReturn(Optional.of(patient));

            service.changeAddress(PATIENT_ID, anotherPatientAddress());

            assertThat(patient.getAddress()).isEqualTo(anotherPatientAddress());
        }

        @Test
        void rejectsAddressChangeForUnknownPatient() {
            given(patients.findById(PATIENT_ID)).willReturn(Optional.empty());

            assertThatExceptionOfType(PatientNotFoundException.class)
                .isThrownBy(() -> service.changeAddress(PATIENT_ID, anotherPatientAddress()));
        }

    }

}
