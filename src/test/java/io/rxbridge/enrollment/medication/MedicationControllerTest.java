package io.rxbridge.enrollment.medication;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import static io.rxbridge.enrollment.medication.MedicationConstants.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

@WebMvcTest(MedicationController.class)
class MedicationControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper mapper;

    @MockitoBean
    MedicationService medications;

    @MockitoBean
    MedicationQueries queries;

    @Nested
    class Registration {

        @Test
        void registersAMedication() throws Exception {
            Medication medication = aMedication();

            given(medications.register(
                    medication.getName(),
                    medication.getDescription(),
                    medication.getCode(),
                    medication.getSideEffects()))
                .willReturn(MEDICATION_ID);

            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of());

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/medications/" + MEDICATION_ID))
                .andExpect(content().string(""));

            then(medications).should().register(
                medication.getName(),
                medication.getDescription(),
                medication.getCode(),
                medication.getSideEffects()
            );
        }

        @Test
        void messingRegisterRequestFieldsNamesTheFields() throws Exception {
            mvc.perform(postJson("/medications", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "code")));

            then(medications).should(never()).register(any(), any(), any(), any());
        }

        @Test
        void tooLongValuesNamesTheFields() throws Exception {
            Medication medication = aMedication();
            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of(
                "name", "a".repeat(NAME_MAX_LENGTH + 1),
                "description", "b".repeat(DESCRIPTION_MAX_LENGTH + 1),
                "code", "M1234567",
                "sideEffects", "c".repeat(SIDE_EFFECTS_MAX_LENGTH + 1)
            ));

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field")
                    .value(containsInAnyOrder("name", "description", "code", "sideEffects")));

            then(medications).should(never()).register(any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "M123", "1111111", "MMMMMMM", "1MMMMMM", "A123456"
        })
        void invalidMedicationCodeIsABadRequest(String input) throws Exception {
            Medication medication = aMedication();
            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of(
                "code", input
            ));

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("code"));

            then(medications).should(never()).register(any(), any(), any(), any());
        }

        @Test
        void approvesNonRequiredFieldAreEmpty() throws Exception {
            Medication medication = aMedication();
            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of(
                "description", "",
                "sideEffects", ""
            ));

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isCreated());

            then(medications).should().register(
                medication.getName(),
                "",
                medication.getCode(),
                ""
            );
        }

        @Test
        void approvesNonRequiredFieldsAreNull() throws Exception {
            Medication medication = aMedication();
            String requestBodyJson = """
                {"name":"%s", "code":"%s"}
                """.formatted(medication.getName(), medication.getCode());

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isCreated());

            then(medications).should().register(
                medication.getName(),
                null,
                medication.getCode(),
                null
            );
        }

        @Test
        void duplicateByMedicationCodeIsAConflict() throws Exception {
            Medication medication = aMedication();
            String medicationCode = medication.getCode();

            willThrow(new DuplicateMedicationException(medicationCode))
                .given(medications)
                .register(
                    medication.getName(),
                    medication.getDescription(),
                    medicationCode,
                    medication.getSideEffects());

            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of());

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

            then(medications).should().register(
                medication.getName(),
                medication.getDescription(),
                medicationCode,
                medication.getSideEffects()
            );
        }

        @Test
        void unexpectedExceptionIsInternalServerError() throws Exception {
            Medication medication = aMedication();

            willThrow(new IllegalStateException())
                .given(medications)
                .register(
                    medication.getName(),
                    medication.getDescription(),
                    medication.getCode(),
                    medication.getSideEffects());

            String requestBodyJson = serializeRegisterRequest(mapper, medication, Map.of());

            mvc.perform(postJson("/medications", requestBodyJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error"));

            then(medications).should().register(
                medication.getName(),
                medication.getDescription(),
                medication.getCode(),
                medication.getSideEffects()
            );
        }

    }

    @Nested
    class GettingById {

        @Test
        void medicationIsReturnedAsJson() throws Exception {
            MedicationView medicationView = MedicationView.from(
                withId(aMedication(), MEDICATION_ID)
            );

            given(queries.medication(MEDICATION_ID))
                .willReturn(new MedicationView(
                    MEDICATION_ID,
                    medicationView.name(),
                    medicationView.code(),
                    true
                ));

            mvc.perform(get("/medications/{id}", MEDICATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEDICATION_ID.toString()))
                .andExpect(jsonPath("$.name").value(medicationView.name()))
                .andExpect(jsonPath("$.code").value(medicationView.code()))
                .andExpect(jsonPath("$.active").value(true));

            then(queries).should().medication(MEDICATION_ID);
        }

        @Test
        void identifierThatIsNotAUuidInThePathIsABadRequest() throws Exception {
            mvc.perform(get("/medications/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(queries).should(never()).medication(anyValidUUID());
        }

        @Test
        void unknownMedicationIdIsNotFound() throws Exception {
            willThrow(new MedicationNotFoundException(MEDICATION_ID))
                .given(queries)
                .medication(MEDICATION_ID);

            mvc.perform(get("/medications/{id}", MEDICATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Medication not found: " + MEDICATION_ID));

            then(queries).should().medication(MEDICATION_ID);
        }

    }

    @Nested
    class GettingByCode {

        @Test
        void returnsMedicationViewAsJsonByCode() throws Exception {
            MedicationView medicationView = MedicationView.from(
                withId(aMedication(), MEDICATION_ID)
            );

            String medicationCode = medicationView.code();

            given(queries.medication(medicationCode))
                .willReturn(new MedicationView(
                    MEDICATION_ID,
                    medicationView.name(),
                    medicationCode,
                    true
                ));

            mvc.perform(get("/medications/by-code/{code}", medicationCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEDICATION_ID.toString()))
                .andExpect(jsonPath("$.name").value(medicationView.name()))
                .andExpect(jsonPath("$.code").value(medicationCode))
                .andExpect(jsonPath("$.active").value(true));

            then(queries).should().medication(medicationCode);
        }

        @Test
        void unknownMedicationCodeIsNotFound() throws Exception {
            String medicationCode = "M000000";

            willThrow(new MedicationNotFoundByCodeException(medicationCode))
                .given(queries)
                .medication(medicationCode);

            mvc.perform(get("/medications/by-code/{code}", medicationCode))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Medication not found by code: " + medicationCode));

            then(queries).should().medication(medicationCode);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "M123", "1111111", "MMMMMMM", "1MMMMMM", "A123456"
        })
        void invalidMedicationCodeInThePathIsABadRequest(String input) throws Exception {
            mvc.perform(get("/medications/by-code/{code}", input))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("code"));

            then(queries).should(never()).medication(input);
        }

    }

    @Nested
    class Rename {

        @Test
        void renamingPassesAndAnswersNoContent() throws Exception {
            String name = "Renamed";

            willDoNothing().given(medications)
                .rename(MEDICATION_ID, name);

            String requestBodyJson = serializeRenameRequest(mapper, name);

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().rename(MEDICATION_ID, name);
        }

        @Test
        void tooLongNameRefusesRenaming() throws Exception {
            String requestBodyJson = serializeRenameRequest(
                mapper,
                "a".repeat(NAME_MAX_LENGTH + 1)
            );

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

            then(medications).should(never()).rename(anyValidUUID(), any());
        }

        @Test
        void refusesRenamingWithEmptyName() throws Exception {
            String requestBodyJson = serializeRenameRequest(
                mapper,
                ""
            );

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

            then(medications).should(never()).rename(anyValidUUID(), any());
        }

        @Test
        void refusesRenamingWithNullName() throws Exception {
            String requestBodyJson = """
                {"name":null}
                """;

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

            then(medications).should(never()).rename(anyValidUUID(), any());
        }

        @Test
        void unknownMedicationIdIsNotFound() throws Exception {
            String name = "Another name";
            String requestBodyJson = serializeRenameRequest(mapper, name);

            willThrow(new MedicationNotFoundException(MEDICATION_ID))
                .given(medications)
                .rename(MEDICATION_ID, name);

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Medication not found: " + MEDICATION_ID));

            then(medications).should().rename(MEDICATION_ID, name);
        }

        @Test
        void concurrentRenamingIsAConflict() throws Exception {
            String name = "Another Name";

            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(medications)
                .rename(MEDICATION_ID, name);

            String requestBodyJson = serializeRenameRequest(mapper, name);

            mvc.perform(patchJson(
                    "/medications/{id}/name",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(medications).should().rename(MEDICATION_ID, name);
        }

    }

    @Nested
    class Deactivate {

        @Test
        void deactivatingPassesAndAnswersNoContent() throws Exception {
            mvc.perform(post("/medications/{id}/deactivate", MEDICATION_ID))
                .andExpect(status().isNoContent());

            then(medications).should().deactivate(MEDICATION_ID);
        }

        @Test
        void nonUuidIdentifierInThePathIsABadRequest() throws Exception {
            mvc.perform(post("/medications/{id}/deactivate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(medications).should(never()).deactivate(anyValidUUID());
        }

        @Test
        void unknownMedicationIdIsNotFound() throws Exception {
            willThrow(new MedicationNotFoundException(MEDICATION_ID))
                .given(medications)
                .deactivate(MEDICATION_ID);

            mvc.perform(post("/medications/{id}/deactivate", MEDICATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Medication not found: " + MEDICATION_ID));

            then(medications).should().deactivate(MEDICATION_ID);
        }

        @Test
        void concurrentDeactivatingIsAConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(medications)
                .deactivate(MEDICATION_ID);

            mvc.perform(post("/medications/{id}/deactivate", MEDICATION_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(medications).should().deactivate(MEDICATION_ID);
        }

    }

    @Nested
    class Activate {

        @Test
        void activatingPassesAndAnswersNoContent() throws Exception {
            mvc.perform(post("/medications/{id}/activate", MEDICATION_ID))
                .andExpect(status().isNoContent());

            then(medications).should().activate(MEDICATION_ID);
        }

        @Test
        void nonUuidIdentifierInThePathIsABadRequest() throws Exception {
            mvc.perform(post("/medications/{id}/activate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(medications).should(never()).activate(anyValidUUID());
        }

        @Test
        void unknownMedicationIdIsNotFound() throws Exception {
            willThrow(new MedicationNotFoundException(MEDICATION_ID))
                .given(medications)
                .activate(MEDICATION_ID);

            mvc.perform(post("/medications/{id}/activate", MEDICATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Medication not found: " + MEDICATION_ID));

            then(medications).should().activate(MEDICATION_ID);
        }

        @Test
        void concurrentActivatingIsAConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(medications)
                .activate(MEDICATION_ID);

            mvc.perform(post("/medications/{id}/activate", MEDICATION_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(medications).should().activate(MEDICATION_ID);
        }

    }

    @Nested
    class ChangeDescription {

        @Test
        void changingDescriptionPassesAndAnswersNoContent() throws Exception {
            String description = "New Description";

            String requestBodyJson = serializeDescriptionRequest(mapper, description);

            mvc.perform(patchJson("/medications/{id}/description",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeDescription(MEDICATION_ID, description);
        }

        @Test
        void passesChangingDescriptionWithEmptyValue() throws Exception {
            String requestBodyJson = serializeDescriptionRequest(mapper, "");

            mvc.perform(patchJson("/medications/{id}/description",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeDescription(MEDICATION_ID, "");
        }

        @Test
        void passesChangingDescriptionWithNullValue() throws Exception {
            String requestBodyJson = """
                {"description":null}
                """;

            mvc.perform(patchJson("/medications/{id}/description",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeDescription(MEDICATION_ID, null);
        }

        @Test
        void tooLongValueNamesTheField() throws Exception {
            String requestBodyJson = serializeDescriptionRequest(
                mapper,
                "a".repeat(DESCRIPTION_MAX_LENGTH + 1)
            );

            mvc.perform(patchJson("/medications/{id}/description",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("description"));

            then(medications).should(never()).changeDescription(anyValidUUID(), any());
        }

        @Test
        void concurrentChangingDescriptionIsAConflict() throws Exception {
            String description = "New Description";

            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(medications)
                .changeDescription(MEDICATION_ID, description);

            String descriptionRequestBody = serializeDescriptionRequest(
                mapper,
                description
            );

            mvc.perform(patchJson("/medications/{id}/description",
                    MEDICATION_ID,
                    descriptionRequestBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(medications).should().changeDescription(MEDICATION_ID, description);
        }

    }

    @Nested
    class ChangeSideEffects {

        @Test
        void changingSideEffectsPassesAndAnswersNoContent() throws Exception {
            String sideEffects = "New Side Effects";

            String requestBodyJson = serializeSideEffectsRequest(
                mapper,
                sideEffects
            );

            mvc.perform(patchJson("/medications/{id}/side-effects",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeSideEffects(MEDICATION_ID, sideEffects);
        }

        @Test
        void passesChangingSideEffectsWithEmptyValue() throws Exception {
            String requestBodyJson = serializeSideEffectsRequest(mapper, "");

            mvc.perform(patchJson("/medications/{id}/side-effects",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeSideEffects(MEDICATION_ID, "");
        }

        @Test
        void passesChangingSideEffectsWithNullValue() throws Exception {
            String requestBodyJson = """
                {"sideEffects":null}
                """;

            mvc.perform(patchJson("/medications/{id}/side-effects",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(medications).should().changeSideEffects(MEDICATION_ID, null);
        }

        @Test
        void tooLongValueNamesTheField() throws Exception {
            String requestBodyJson = serializeSideEffectsRequest(
                mapper,
                "a".repeat(SIDE_EFFECTS_MAX_LENGTH + 1)
            );

            mvc.perform(patchJson("/medications/{id}/side-effects",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("sideEffects"));

            then(medications).should(never()).changeSideEffects(anyValidUUID(), any());
        }

        @Test
        void concurrentChangingSideEffectsIsAConflict() throws Exception {
            String sideEffects = "New Side Effects";

            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(medications)
                .changeSideEffects(MEDICATION_ID, sideEffects);

            String requestBodyJson = serializeSideEffectsRequest(
                mapper,
                sideEffects
            );

            mvc.perform(patchJson("/medications/{id}/side-effects",
                    MEDICATION_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(medications).should().changeSideEffects(MEDICATION_ID, sideEffects);
        }

    }

    private static String serializeRegisterRequest(
        JsonMapper mapper,
        Medication medication,
        Map<String, Object> overrides
    ) {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("name",         medication.getName());
        registerRequest.put("description",  medication.getDescription());
        registerRequest.put("code",         medication.getCode());
        registerRequest.put("sideEffects",  medication.getSideEffects());
        registerRequest.putAll(overrides);

        return jsonSerialize(mapper, registerRequest);
    }

    private static String serializeRenameRequest(JsonMapper mapper, String name) {
        Map<String, Object> renameRequest = Map.of(
            "name", name
        );

        return jsonSerialize(mapper, renameRequest);
    }

    private static String serializeDescriptionRequest(JsonMapper mapper, String description) {
        Map<String, Object> descriptionRequest = Map.of(
            "description", description
        );

        return jsonSerialize(mapper, descriptionRequest);
    }

    private static String serializeSideEffectsRequest(JsonMapper mapper, String sideEffects) {
        Map<String, Object> sideEffectsRequest = Map.of(
            "sideEffects", sideEffects
        );

        return jsonSerialize(mapper, sideEffectsRequest);
    }

}
