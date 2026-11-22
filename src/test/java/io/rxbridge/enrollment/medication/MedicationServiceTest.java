package io.rxbridge.enrollment.medication;

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

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    private static final UUID MEDICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    MedicationRepository medications;

    BusinessCalendar calendar = businessCalendar();

    private final List<Object> published = new ArrayList<>();

    private MedicationService service;

    @BeforeEach
    void setUp() {
        service = new MedicationService(medications, published::add, calendar);
    }

    @Nested
    class Rename {

        @Test
        void renamesMedication() {
            Medication medication = aMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));

            service.rename(MEDICATION_ID, "New Medication Name");

            assertThat(medication.getName()).isEqualTo("New Medication Name");
        }

        @Test
        void rejectsRenamingForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.rename(MEDICATION_ID, "New Medication Name"))
                .isInstanceOfSatisfying(
                    MedicationNotFoundException.class,
                    e -> assertThat(e.medicationId()).isEqualTo(MEDICATION_ID)
                );
        }

    }

    @Nested
    class Activate {

        @Test
        void activatesMedication() {
            Medication medication = anInactiveMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));

            service.activate(MEDICATION_ID);

            assertThat(medication.isActive()).isTrue();
        }

        @Test
        void rejectsActivatingForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.activate(MEDICATION_ID))
                .isInstanceOfSatisfying(
                    MedicationNotFoundException.class,
                    e -> assertThat(e.medicationId()).isEqualTo(MEDICATION_ID)
                );
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

            service.deactivate(MEDICATION_ID);

            assertThat(published).isEmpty();
        }

        @Test
        void publishesNothingForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(MEDICATION_ID))
                .isInstanceOfSatisfying(
                    MedicationNotFoundException.class,
                    e -> assertThat(e.medicationId()).isEqualTo(MEDICATION_ID)
                );
            assertThat(published).isEmpty();
        }

    }

    @Nested
    class ChangeDescription {

        @Test
        void changesDescriptionOfMedication() {
            Medication medication = aMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));

            service.changeDescription(MEDICATION_ID, "New Medication Description");

            assertThat(medication.getDescription()).isEqualTo("New Medication Description");
        }

        @Test
        void rejectsChangingDescriptionForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeDescription(MEDICATION_ID, "New Medication Description"))
                .isInstanceOfSatisfying(
                    MedicationNotFoundException.class,
                    e -> assertThat(e.medicationId()).isEqualTo(MEDICATION_ID)
                );
        }

    }

    @Nested
    class ChangeSideEffects {

        @Test
        void changesSideEffectsOfMedication() {
            Medication medication = aMedication();
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.of(medication));

            service.changeSideEffects(MEDICATION_ID, "New Medication Side Effects");

            assertThat(medication.getSideEffects()).isEqualTo("New Medication Side Effects");
        }

        @Test
        void rejectsChangingSideEffectsForUnknownMedication() {
            given(medications.findById(MEDICATION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeSideEffects(MEDICATION_ID, "New Medication Side Effects"))
                .isInstanceOfSatisfying(
                    MedicationNotFoundException.class,
                    e -> assertThat(e.medicationId()).isEqualTo(MEDICATION_ID)
                );
        }

    }

}
