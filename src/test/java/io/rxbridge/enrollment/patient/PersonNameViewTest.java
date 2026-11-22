package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;

class PersonNameViewTest {

    @Test
    void personNameViewHasNecessaryValues() {
        PersonName personName = aPatientPersonName();

        PersonNameView personNameView = PersonNameView.from(personName);

        assertThat(personNameView)
            .isEqualTo(new PersonNameView(
                personName.firstName(),
                personName.lastName()
            ));
    }

}
