package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
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

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    private static final UUID MEDICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    MedicationRepository medications;

    BusinessCalendar calendar = businessCalendar();

    private final List<Object> published = new ArrayList<>();

    @Nested
    class Activate {

        @Test
        void activatesMedication() {
            Medication medication = anInactiveMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));
            var service = createMedicationService();

            service.activate(MEDICATION_ID);

            assertThat(medication.isActive()).isTrue();
        }

        @Test
        void rejectsActivatingForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());
            var service = createMedicationService();

            assertThatExceptionOfType(MedicationNotFoundException.class)
                .isThrownBy(() -> service.activate(MEDICATION_ID));
        }

    }

    @Nested
    class Deactivate {

        @Test
        void publishesMedicationDeactivatedAfterDeactivating() {
            Medication medication = aMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));
            List<Boolean> activeWhenPublished = new ArrayList<>();

            var service = new MedicationService(
                medications,
                event -> {
                    published.add(event);
                    activeWhenPublished.add(medication.isActive());
                },
                calendar
            );

            service.deactivate(MEDICATION_ID);

            assertThat(published).containsExactly(new MedicationDeactivated(MEDICATION_ID, TODAY));
            assertThat(activeWhenPublished).containsExactly(false);
        }

        @Test
        void publishesNothingWhenMedicationIsAlreadyInactive() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(anInactiveMedication()));

            createMedicationService().deactivate(MEDICATION_ID);

            assertThat(published).isEmpty();
        }

        @Test
        void publishesNothingForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());
            var service = createMedicationService();

            assertThatExceptionOfType(MedicationNotFoundException.class)
                .isThrownBy(() -> service.deactivate(MEDICATION_ID));
            assertThat(published).isEmpty();
        }

    }

    private MedicationService createMedicationService() {
        return new MedicationService(medications, published::add, calendar);
    }

}
