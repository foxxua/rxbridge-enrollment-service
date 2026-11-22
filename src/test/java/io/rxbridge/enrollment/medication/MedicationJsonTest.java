package io.rxbridge.enrollment.medication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.medication.MedicationController.MedicationRegisterRequest;
import static io.rxbridge.enrollment.medication.MedicationController.RenameRequest;
import static io.rxbridge.enrollment.medication.MedicationController.DescriptionRequest;
import static io.rxbridge.enrollment.medication.MedicationController.SideEffectsRequest;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@JsonTest
class MedicationJsonTest {

    @Autowired
    JacksonTester<MedicationView> view;

    @Autowired
    JacksonTester<MedicationRegisterRequest> register;

    @Autowired
    JacksonTester<RenameRequest> rename;

    @Autowired
    JacksonTester<DescriptionRequest> description;

    @Autowired
    JacksonTester<SideEffectsRequest> sideEffects;

    @Test
    void viewKeepsAgreedContract() throws Exception {
        Medication medication = withId(aMedication(), MEDICATION_ID);
        var medicationView = MedicationView.from(medication);

        assertThat(view.write(medicationView))
            .isStrictlyEqualToJson("""
                {
                  "id":"00000000-0000-0000-0000-000000000002",
                  "name":"Aspirin",
                  "code":"M000001",
                  "active":true
                }
                """);
    }

    @Test
    void registerReadsValidValues() throws Exception {
        String registerJson = """
            {
              "name":"Aspirin",
              "description":"Aspirin Description",
              "code":"M000001",
              "sideEffects":"Aspirin Side Effects"
            }
            """;

        assertThat(register.parseObject(registerJson))
            .extracting(
                MedicationRegisterRequest::name,
                MedicationRegisterRequest::description,
                MedicationRegisterRequest::code,
                MedicationRegisterRequest::sideEffects
            ).containsExactly(
                "Aspirin",
                "Aspirin Description",
                "M000001",
                "Aspirin Side Effects"
            );
    }

    @Test
    void registerRefusesInvalidValues() throws Exception {
        String registerJson = """
            {
              "name":1,
              "description":true,
              "code":{},
              "sideEffects":[]
            }
            """;

        assertThatThrownBy(() -> register.parseObject(registerJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void renameReadsValidValue() throws Exception {
        String renameJson = """
            {"name":"Analgin"}
            """;

        assertThat(rename.parseObject(renameJson).name())
            .isEqualTo("Analgin");
    }

    @Test
    void renameRefusesInvalidValue() throws Exception {
        String renameJson = """
            {"name":[]}
            """;

        assertThatThrownBy(() -> rename.parseObject(renameJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void descriptionReadsValidValue() throws Exception {
        String descriptionJson = """
            {"description":"Aspirin Description"}
            """;

        assertThat(description.parseObject(descriptionJson).description())
            .isEqualTo("Aspirin Description");
    }

    @Test
    void descriptionRefusesInvalidValue() throws Exception {
        String descriptionJson = """
            {"description":[]}
            """;

        assertThatThrownBy(() -> description.parseObject(descriptionJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void sideEffectsReadsValidValue() throws Exception {
        String sideEffectsJson = """
            {"sideEffects":"Aspirin Side Effects"}
            """;

        assertThat(sideEffects.parseObject(sideEffectsJson).sideEffects())
            .isEqualTo("Aspirin Side Effects");
    }

    @Test
    void sideEffectsRejectsInvalidValue() throws Exception {
        String sideEffectsJson = """
            {"sideEffects":{}}
            """;

        assertThatThrownBy(() -> sideEffects.parseObject(sideEffectsJson))
            .isInstanceOf(MismatchedInputException.class);
    }

}
