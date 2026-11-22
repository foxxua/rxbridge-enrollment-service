package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;

class AddressViewTest {

    @Test
    void addressViewHasNecessaryValues() {
        Address address = aPatientAddress();

        AddressView addressView = AddressView.from(address);

        assertThat(addressView)
            .isEqualTo(new AddressView(
                address.street(),
                address.city(),
                address.country(),
                address.region(),
                address.zipcode()
            ));
    }

}
