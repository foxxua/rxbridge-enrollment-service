package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import static io.rxbridge.enrollment.patient.PatientConstants.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper mapper;

    @MockitoBean
    PatientService patients;

    @MockitoBean
    PatientQueries queries;

    @Nested
    class Registration {

        @Test
        void registerAPatient() throws Exception {
            Patient patient = aPatient();

            given(patients.register(
                    patient.getPersonName(),
                    patient.getEmail(),
                    patient.getAddress(),
                    patient.getSex(),
                    patient.getBirthdate()))
                .willReturn(PATIENT_ID);

            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of());

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/patients/" + PATIENT_ID))
                .andExpect(content().string(""));

            then(patients).should().register(
                patient.getPersonName(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getSex(),
                patient.getBirthdate()
            );
        }

        @Test
        void messingRegisterRequestFieldsNamesTheFields() throws Exception {
            mvc.perform(postJson("/patients", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "personName",
                    "email",
                    "address",
                    "sex",
                    "birthdate"
                )));

            then(patients).should(never()).register(any(), any(), any(), any(), any());
        }

        @Test
        void missingPersonNameFieldsNamesTheFields() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of(
                "personName", Map.of()
            ));

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "personName.firstName", "personName.lastName"
                )));

            then(patients).should(never()).register(any(), any(), any(), any(), any());
        }

        @Test
        void missingAddressFieldsNamesTheFields() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of(
                "address", Map.of()
            ));

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "address.street",
                    "address.city",
                    "address.country",
                    "address.zipcode"
                )));

            then(patients).should(never()).register(any(), any(), any(), any(), any());
        }

        @Test
        void tooLongValuesNamesTheFields() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeRegisterRequest(mapper, patient,
                Map.of(
                    "personName", Map.of(
                            "firstName", "a".repeat(NAME_MAX_LENGTH + 1),
                            "lastName", "b".repeat(NAME_MAX_LENGTH + 1)
                        ),
                    "email", "testemail@"
                        + "c".repeat(EMAIL_MAX_LENGTH - "testemail@".length() - ".bad".length() - 1)
                        + ".bad",
                    "address", Map.of(
                            "street", "d".repeat(STREET_MAX_LENGTH + 1),
                            "city", "e".repeat(CITY_MAX_LENGTH + 1),
                            "country", "f".repeat(COUNTRY_MAX_LENGTH + 1),
                            "region", "g".repeat(REGION_MAX_LENGTH + 1),
                            "zipcode", "h".repeat(ZIPCODE_MAX_LENGTH + 1)
                        )
                )
            );

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "personName.firstName",
                    "personName.lastName",
                    "email",
                    "address.street",
                    "address.city",
                    "address.country",
                    "address.region",
                    "address.zipcode"
                )));

            then(patients).should(never()).register(any(), any(), any(), any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalidemail", "invalid@@example.com",
            " @. ", "in valid@example.com"
        })
        void wrongEmailFormatNamesAField(String input) throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of(
                "email", input
            ));

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(patients).should(never()).register(any(), eq(input), any(), any(), any());
        }

        @Test
        void approvesNonRequiredFieldIsEmpty() throws Exception {
            Address address = aPatientAddress();
            Patient patient = aPatientBuilder()
                .address(new Address(
                    address.street(),
                    address.city(),
                    address.country(),
                    "",
                    address.zipcode()
                )).build();

            given(patients.register(
                    patient.getPersonName(),
                    patient.getEmail(),
                    patient.getAddress(),
                    patient.getSex(),
                    patient.getBirthdate()))
                .willReturn(PATIENT_ID);

            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of());

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/patients/" + PATIENT_ID))
                .andExpect(content().string(""));

            then(patients).should().register(
                patient.getPersonName(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getSex(),
                patient.getBirthdate()
            );
        }

        @Test
        void approvesNonRequiredFieldIsNull() throws Exception {
            Address address = aPatientAddress();
            Patient patient = aPatientBuilder()
                .address(new Address(
                    address.street(),
                    address.city(),
                    address.country(),
                    null,
                    address.zipcode()
                )).build();

            given(patients.register(
                    patient.getPersonName(),
                    patient.getEmail(),
                    patient.getAddress(),
                    patient.getSex(),
                    patient.getBirthdate()))
                .willReturn(PATIENT_ID);

            String requestBodyJson = """
                {
                  "personName":{
                    "firstName":"%s",
                    "lastName":"%s"
                  },
                  "email":"%s",
                  "address":{
                    "street":"%s",
                    "city":"%s",
                    "country":"%s",
                    "region":null,
                    "zipcode":"%s"
                  },
                  "sex":"%s",
                  "birthdate":"%s"
                }
                """.formatted(
                    patient.getPersonName().firstName(),
                    patient.getPersonName().lastName(),
                    patient.getEmail(),
                    patient.getAddress().street(),
                    patient.getAddress().city(),
                    patient.getAddress().country(),
                    patient.getAddress().zipcode(),
                    patient.getSex(),
                    patient.getBirthdate()
                );

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/patients/" + PATIENT_ID))
                .andExpect(content().string(""));

            then(patients).should().register(
                patient.getPersonName(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getSex(),
                patient.getBirthdate()
            );
        }

        @Test
        void unexpectedExceptionIsInternalServerError() throws Exception {
            Patient patient = aPatient();

            willThrow(new IllegalStateException())
                .given(patients)
                .register(
                    patient.getPersonName(),
                    patient.getEmail(),
                    patient.getAddress(),
                    patient.getSex(),
                    patient.getBirthdate()
                );

            String requestBodyJson = serializeRegisterRequest(mapper, patient, Map.of());

            mvc.perform(postJson("/patients", requestBodyJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error"));

            then(patients).should().register(
                patient.getPersonName(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getSex(),
                patient.getBirthdate()
            );
        }

    }

    @Nested
    class GettingById {

        @Test
        void patientIsReturnedAsJson() throws Exception {
            PatientView patientView = PatientView.from(
                withId(aPatient(), PATIENT_ID)
            );

            given(queries.patient(PATIENT_ID))
                .willReturn(new PatientView(
                    PATIENT_ID,
                    patientView.personName(),
                    patientView.email(),
                    patientView.address(),
                    patientView.sex(),
                    patientView.birthdate(),
                    true
                ));

            mvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.personName.firstName").value(patientView.personName().firstName()))
                .andExpect(jsonPath("$.personName.lastName").value(patientView.personName().lastName()))
                .andExpect(jsonPath("$.email").value(patientView.email()))
                .andExpect(jsonPath("$.address.street").value(patientView.address().street()))
                .andExpect(jsonPath("$.address.city").value(patientView.address().city()))
                .andExpect(jsonPath("$.address.country").value(patientView.address().country()))
                .andExpect(jsonPath("$.address.region").value(patientView.address().region()))
                .andExpect(jsonPath("$.address.zipcode").value(patientView.address().zipcode()))
                .andExpect(jsonPath("$.sex").value(patientView.sex().name()))
                .andExpect(jsonPath("$.birthdate").value(patientView.birthdate().toString()))
                .andExpect(jsonPath("$.active").value(true));

            then(queries).should().patient(PATIENT_ID);
        }

        @Test
        void identifierThatIsNotAUuidInThePathIsABadRequest() throws Exception {
            mvc.perform(get("/patients/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(queries).should(never()).patient(anyValidUUID());
        }

        @Test
        void unknownPatientIdIsNotFound() throws Exception {
            willThrow(new PatientNotFoundException(PATIENT_ID))
                .given(queries)
                .patient(PATIENT_ID);

            mvc.perform(get("/patients/{id}", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Patient not found: " + PATIENT_ID));

            then(queries).should().patient(PATIENT_ID);
        }

    }

    @Nested
    class Duplicates {

        @ParameterizedTest(name = "limit - {0}")
        @ValueSource(ints = {1, 20, 50})
        void findsDuplicatesOfPatients(int limit) throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeDuplicatesRequest(
                mapper,
                patient.getPersonName(),
                patient.getBirthdate(),
                Map.of()
            );
            List<PatientView> patientViews = getPatientViewsList(limit);

            given(queries.duplicatesOf(
                    patient.getPersonName(),
                    patient.getBirthdate(),
                    Limit.of(limit)))
                .willReturn(patientViews);

            mvc.perform(postJson("/patients/duplicates", requestBodyJson)
                    .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]").value(hasSize(limit)))
                .andExpect(content().json(mapper.writeValueAsString(patientViews)));

            then(queries).should().duplicatesOf(
                patient.getPersonName(),
                patient.getBirthdate(),
                Limit.of(limit)
            );
        }

        @Test
        void acceptsDefaultLimit() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeDuplicatesRequest(
                mapper,
                patient.getPersonName(),
                patient.getBirthdate(),
                Map.of()
            );

            int defaultLimit = 10;
            List<PatientView> patientViews = getPatientViewsList(defaultLimit);

            given(queries.duplicatesOf(
                    patient.getPersonName(),
                    patient.getBirthdate(),
                    Limit.of(defaultLimit)))
                .willReturn(patientViews);

            mvc.perform(postJson("/patients/duplicates", requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]").value(hasSize(defaultLimit)))
                .andExpect(content().json(mapper.writeValueAsString(patientViews)));

            then(queries).should().duplicatesOf(
                patient.getPersonName(),
                patient.getBirthdate(),
                Limit.of(defaultLimit)
            );
        }

        @ParameterizedTest(name = "limit - {0}")
        @ValueSource(ints = {
            -1, 0, 51
        })
        void illegalLimitsAreABadRequest(int limit) throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeDuplicatesRequest(
                mapper,
                patient.getPersonName(),
                patient.getBirthdate(),
                Map.of()
            );

            mvc.perform(postJson("/patients/duplicates", requestBodyJson)
                    .param("limit", String.valueOf(limit)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(queries).should(never()).duplicatesOf(any(), any(), eq(Limit.of(limit)));
        }

        @Test
        void refuseFindingDuplicatesWithEmptyRequestBody() throws Exception {
            mvc.perform(postJson("/patients/duplicates", "{}")
                    .param("limit", String.valueOf(10)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(queries).should(never()).duplicatesOf(any(), any(), any());
        }

        @Test
        void tooLongValuesInRequestBodyFieldsIsABadRequest() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeDuplicatesRequest(
                mapper,
                patient.getPersonName(),
                patient.getBirthdate(),
                Map.of(
                    "personName", Map.of(
                            "firstName", "a".repeat(NAME_MAX_LENGTH + 1),
                            "lastName", "b".repeat(NAME_MAX_LENGTH + 1)
                        )
                )
            );

            mvc.perform(postJson("/patients/duplicates", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "personName.firstName",
                    "personName.lastName"
                )));

            then(queries).should(never()).duplicatesOf(any(), any(), any());
        }

    }

    @Nested
    class FindPatients {

        @Test
        void pageHasContentAndPageBlock() throws Exception {
            PatientView patientView = PatientView.from(
                withId(aPatient(), PATIENT_ID)
            );

            Pageable expected = PageRequest.of(
                1, 5,
                Sort.by(Sort.Order.asc("personName.lastName"), Sort.Order.asc("id"))
            );

            String requestBodyJson = serializeEmailRequest(mapper, patientView.email());

            given(queries.patientsOf(any(), any()))
                .willReturn(
                    new PageImpl<>(
                        List.of(patientView),
                        expected,
                        6
                    )
                );

            mvc.perform(postJson("/patients/find", requestBodyJson)
                    .param("page", "1")
                    .param("size", "5")
                    .param("sort", "personName.lastName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].personName.lastName").value(patientView.personName().lastName()))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.totalPages").value(2));

            then(queries).should().patientsOf(patientView.email(), expected);
        }

        @Test
        void listWithoutParametersUsesTheDefaultPaging() throws Exception {
            Patient patient = aPatient();

            Pageable expected = PageRequest.of(
                0, 20,
                Sort.by(
                    Sort.Order.asc("personName.lastName"),
                    Sort.Order.asc("personName.firstName"),
                    Sort.Order.asc("id")
                )
            );

            String requestBodyJson = serializeEmailRequest(mapper, patient.getEmail());

            given(queries.patientsOf(any(), any()))
                .willReturn(
                    new PageImpl<>(List.of(), expected, 0)
                );

            mvc.perform(postJson("/patients/find", requestBodyJson))
                .andExpect(status().isOk());

            then(queries).should().patientsOf(patient.getEmail(), expected);
        }

        @ParameterizedTest(name = "cannot sort by - {0}")
        @ValueSource(strings = {
            "email",
            "address.region",
            "address.zipcode",
            "birthdate"
        })
        void refusesFindingPatientsWhenPassesNonSupportedSortingField(String input) throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeEmailRequest(mapper, patient.getEmail());

            mvc.perform(postJson("/patients/find", requestBodyJson)
                    .param("sort", input + ",asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(queries).should(never()).patientsOf(any(), any());
        }

        @Test
        void refusesFindingPatientsWithEmptyRequestBody() throws Exception {
            mvc.perform(postJson("/patients/find", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(queries).should(never()).patientsOf(any(), any());
        }

        @Test
        void tooLongValuesInRequestBodyIsABadRequest() throws Exception {
            String requestBodyJson = serializeEmailRequest(
                mapper,
                "a".repeat(EMAIL_MAX_LENGTH + 1)
            );

            mvc.perform(postJson("/patients/find", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(queries).should(never()).patientsOf(any(), any());
        }

    }

    @Nested
    class Deactivate {

        @Test
        void deactivatingPassesAndAnswersNoContent() throws Exception {
            mvc.perform(post("/patients/{id}/deactivate", PATIENT_ID))
                .andExpect(status().isNoContent());

            then(patients).should().deactivate(PATIENT_ID);
        }

        @Test
        void nonUuidIdentifierInThePathIsABadRequest() throws Exception {
            mvc.perform(post("/patients/{id}/deactivate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(patients).should(never()).deactivate(anyValidUUID());
        }

        @Test
        void unknownPatientIdIsNotFound() throws Exception {
            willThrow(new PatientNotFoundException(PATIENT_ID))
                .given(patients)
                .deactivate(PATIENT_ID);

            mvc.perform(post("/patients/{id}/deactivate", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Patient not found: " + PATIENT_ID));

            then(patients).should().deactivate(PATIENT_ID);
        }

        @Test
        void concurrentDeactivatingIsAConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(patients)
                .deactivate(PATIENT_ID);

            mvc.perform(post("/patients/{id}/deactivate", PATIENT_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(patients).should().deactivate(PATIENT_ID);
        }

    }

    @Nested
    class Activate {

        @Test
        void activatingPassesAndAnswersNoContent() throws Exception {
            mvc.perform(post("/patients/{id}/activate", PATIENT_ID))
                .andExpect(status().isNoContent());

            then(patients).should().activate(PATIENT_ID);
        }

        @Test
        void nonUuidIdentifierInThePathIsABadRequest() throws Exception {
            mvc.perform(post("/patients/{id}/activate", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(patients).should(never()).activate(anyValidUUID());
        }

        @Test
        void unknownPatientIdIsNotFound() throws Exception {
            willThrow(new PatientNotFoundException(PATIENT_ID))
                .given(patients)
                .activate(PATIENT_ID);

            mvc.perform(post("/patients/{id}/activate", PATIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Patient not found: " + PATIENT_ID));

            then(patients).should().activate(PATIENT_ID);
        }

        @Test
        void concurrentActivatingIsAConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(patients)
                .activate(PATIENT_ID);

            mvc.perform(post("/patients/{id}/activate", PATIENT_ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(patients).should().activate(PATIENT_ID);
        }

    }

    @Nested
    class ChangeEmail {

        @Test
        void changingEmailPassesAndAnswersNoContent() throws Exception {
            Patient patient = aPatient();
            String requestBodyJson = serializeEmailRequest(mapper, patient.getEmail());

            mvc.perform(patchJson("/patients/{id}/email",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(patients).should().changeEmail(PATIENT_ID, patient.getEmail());
        }

        @Test
        void refusesChangingEmailWithEmptyValue() throws Exception {
            String requestBodyJson = serializeEmailRequest(mapper, "");

            mvc.perform(patchJson("/patients/{id}/email",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(patients).should(never()).changeEmail(anyValidUUID(), any());
        }

        @Test
        void refusesChangingEmailWithNullValue() throws Exception {
            String requestBodyJson = """
                {"email":null}
                """;

            mvc.perform(patchJson("/patients/{id}/email",
                PATIENT_ID,
                requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(patients).should(never()).changeEmail(anyValidUUID(), any());
        }

        @Test
        void tooLongValueNamesTheField() throws Exception {
            String requestBodyJson = serializeEmailRequest(
                mapper,
                "a".repeat(EMAIL_MAX_LENGTH + 1)
            );

            mvc.perform(patchJson("/patients/{id}/email",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

            then(patients).should(never()).changeEmail(anyValidUUID(), any());
        }

        @Test
        void concurrentChangingEmailIsAConflict() throws Exception {
            Patient patient = aPatient();

            String requestBodyJson = serializeEmailRequest(mapper, patient.getEmail());

            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(patients)
                .changeEmail(PATIENT_ID, patient.getEmail());

            mvc.perform(patchJson("/patients/{id}/email",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(patients).should().changeEmail(PATIENT_ID, patient.getEmail());
        }

    }

    @Nested
    class ChangeAddress {

        @Test
        void changingAddressPassesAndAnswersNoContent() throws Exception {
            Address address = aPatientAddress();
            String requestBodyJson = serializeAddressRequest(
                mapper,
                address,
                Map.of()
            );

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(patients).should().changeAddress(PATIENT_ID, address);
        }

        @Test
        void refusesChangingAddressWithEmptyValuesForRequiredFields() throws Exception {
            Address address = aPatientAddress();
            String requestBodyJson = serializeAddressRequest(mapper, address, Map.of(
                "street", "",
                "city", "",
                "country", "",
                "zipcode", ""
            ));

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "street",
                    "city",
                    "country",
                    "zipcode"
                )));

            then(patients).should(never()).changeAddress(anyValidUUID(), any());
        }

        @Test
        void refusesChangingAddressWithNullValueForRequiredFields() throws Exception {
            String requestBodyJson = """
                {"region":"Some Region"}
                """;

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "street",
                    "city",
                    "country",
                    "zipcode"
                )));

            then(patients).should(never()).changeAddress(anyValidUUID(), any());
        }

        @Test
        void passesChangingAddressWithEmptyValueForNonRequiredField() throws Exception {
            Address address = aPatientAddress();
            Address addressToUpdate = new Address(
                address.street(),
                address.city(),
                address.country(),
                "",
                address.zipcode()
            );
            String requestBodyJson = serializeAddressRequest(mapper, addressToUpdate, Map.of());

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(patients).should().changeAddress(PATIENT_ID, addressToUpdate);
        }

        @Test
        void passesChangingAddressWithNullValueForNonRequiredField() throws Exception {
            Address address = aPatientAddress();
            Address addressToUpdate = new Address(
                address.street(),
                address.city(),
                address.country(),
                null,
                address.zipcode()
            );
            String requestBodyJson = """
                {
                  "street":"%s",
                  "city":"%s",
                  "country":"%s",
                  "region":null,
                  "zipcode":"%s"
                }
                """.formatted(
                    address.street(),
                    address.city(),
                    address.country(),
                    address.zipcode()
                );

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(patients).should().changeAddress(PATIENT_ID, addressToUpdate);
        }

        @Test
        void tooLongValueNamesTheField() throws Exception {
            Address address = aPatientAddress();
            String requestBodyJson = serializeAddressRequest(mapper, address, Map.of(
                "street", "a".repeat(STREET_MAX_LENGTH + 1),
                "city", "b".repeat(CITY_MAX_LENGTH + 1),
                "country", "c".repeat(COUNTRY_MAX_LENGTH + 1),
                "region", "d".repeat(REGION_MAX_LENGTH + 1),
                "zipcode", "e".repeat(ZIPCODE_MAX_LENGTH + 1)
            ));

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                    "street",
                    "city",
                    "country",
                    "region",
                    "zipcode"
                )));

            then(patients).should(never()).changeAddress(anyValidUUID(), any());
        }

        @Test
        void concurrentChangingAddressIsAConflict() throws Exception {
            Address address = aPatientAddress();
            String requestBodyJson = serializeAddressRequest(mapper, address, Map.of());

            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(patients)
                .changeAddress(PATIENT_ID, address);

            mvc.perform(patchJson("/patients/{id}/address",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(patients).should().changeAddress(PATIENT_ID, address);
        }

    }

    private static String serializeRegisterRequest(
        JsonMapper mapper,
        Patient patient,
        Map<String, Object> overrides
    ) {
        Map<String, Object> personNameMap = new HashMap<>();
        personNameMap.put("firstName", patient.getPersonName().firstName());
        personNameMap.put("lastName",  patient.getPersonName().lastName());

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("street",  patient.getAddress().street());
        addressMap.put("city",    patient.getAddress().city());
        addressMap.put("country", patient.getAddress().country());
        addressMap.put("region",  patient.getAddress().region());
        addressMap.put("zipcode", patient.getAddress().zipcode());

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("personName", personNameMap);
        registerRequest.put("email",      patient.getEmail());
        registerRequest.put("address",    addressMap);
        registerRequest.put("sex",        patient.getSex());
        registerRequest.put("birthdate",  patient.getBirthdate());
        registerRequest.putAll(overrides);

        return jsonSerialize(mapper, registerRequest);
    }

    private static String serializeAddressRequest(
        JsonMapper mapper,
        Address address,
        Map<String, Object> overrides
    ) {
        Map<String, Object> addressRequest = new HashMap<>();
        addressRequest.put("street",  address.street());
        addressRequest.put("city",    address.city());
        addressRequest.put("country", address.country());
        addressRequest.put("region",  address.region());
        addressRequest.put("zipcode", address.zipcode());
        addressRequest.putAll(overrides);

        return jsonSerialize(mapper, addressRequest);
    }

    private static String serializeDuplicatesRequest(
        JsonMapper mapper,
        PersonName personName,
        LocalDate birthdate,
        Map<String, Object> overrides
    ) {
        Map<String, Object> personNameMap = new HashMap<>();
        personNameMap.put("firstName", personName.firstName());
        personNameMap.put("lastName",  personName.lastName());

        Map<String, Object> duplicatesRequest = new HashMap<>();
        duplicatesRequest.put("personName", personNameMap);
        duplicatesRequest.put("birthdate",  birthdate);
        duplicatesRequest.putAll(overrides);

        return jsonSerialize(mapper, duplicatesRequest);
    }

    private static String serializeEmailRequest(
        JsonMapper mapper,
        String email
    ) {
        Map<String, Object> emailRequest = Map.of(
            "email", email
        );

        return jsonSerialize(mapper, emailRequest);
    }

    private List<PatientView> getPatientViewsList(int count) {
        return IntStream.range(0, count)
            .mapToObj(item -> PatientView.from(withId(aPatient(), PATIENT_ID)))
            .toList();
    }

}
