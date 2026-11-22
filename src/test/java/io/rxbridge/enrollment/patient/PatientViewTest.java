package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

class PatientViewTest {

    @Test
    void patientViewHasNecessaryValues() {
        Patient patient = withId(
            aPatient(),
            PATIENT_ID
        );

        PatientView patientView = PatientView.from(patient);

        assertThat(patientView)
            .isEqualTo(new PatientView(
                PATIENT_ID,
                PersonNameView.from(patient.getPersonName()),
                patient.getEmail(),
                AddressView.from(patient.getAddress()),
                patient.getSex(),
                patient.getBirthdate(),
                true
            ));
    }

}
