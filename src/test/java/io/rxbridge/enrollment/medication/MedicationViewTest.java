package io.rxbridge.enrollment.medication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

class MedicationViewTest {

    @Test
    void medicationViewHasNecessaryValues() {
        Medication medication = withId(
            aMedication(),
            MEDICATION_ID
        );

        MedicationView medicationView = MedicationView.from(medication);

        assertThat(medicationView)
            .isEqualTo(new MedicationView(
                MEDICATION_ID,
                medication.getName(),
                medication.getCode(),
                true
            ));
    }

}
