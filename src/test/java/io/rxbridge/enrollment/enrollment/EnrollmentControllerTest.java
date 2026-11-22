package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.error.DomainValidationException;
import io.rxbridge.enrollment.medication.MedicationView;
import io.rxbridge.enrollment.patient.PatientNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;
import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper mapper;

    @MockitoBean
    EnrollmentService enrollments;

    @MockitoBean
    EnrollmentQueries queries;

    @Nested
    class Enroll {

        @Test
        void enrollingAnswersCreatedWithLocation() throws Exception {
            given(enrollments.enroll(PATIENT_ID, MEDICATION_ID))
                .willReturn(ENROLLMENT_ID);

            String requestBodyJson = serializeEnrollRequest(mapper, MEDICATION_ID);

            mvc.perform(postJson("/patients/{patientId}/enrollments",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/enrollments/" + ENROLLMENT_ID))
                .andExpect(content().string(""));

            then(enrollments).should().enroll(PATIENT_ID, MEDICATION_ID);
        }

        @Test
        void missingMedicationIdNamesTheField() throws Exception {
            mvc.perform(postJson("/patients/{patientId}/enrollments", PATIENT_ID, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("medicationId"));

            then(enrollments).should(never()).enroll(anyValidUUID(), any());
        }

        @Test
        void identifierThatIsNotAUuidInThePathIsABadRequest() throws Exception {
            String requestBodyJson = serializeEnrollRequest(mapper, MEDICATION_ID);

            mvc.perform(postJson("/patients/{patientId}/enrollments", "abc", requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(enrollments).should(never()).enroll(anyValidUUID(), anyValidUUID());
        }

        @Test
        void malformedJsonIsABadRequest() throws Exception {
            mvc.perform(postJson("/patients/{patientId}/enrollments", PATIENT_ID, "{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

            then(enrollments).should(never()).enroll(anyValidUUID(), anyValidUUID());
        }

        @Test
        void unknownPatientIsNotFound() throws Exception {
            willThrow(new PatientNotFoundException(PATIENT_ID))
                .given(enrollments)
                .enroll(PATIENT_ID, MEDICATION_ID);

            String requestBodyJson = serializeEnrollRequest(mapper, MEDICATION_ID);

            mvc.perform(postJson("/patients/{patientId}/enrollments",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Patient not found: " + PATIENT_ID));

            then(enrollments).should().enroll(PATIENT_ID, MEDICATION_ID);
        }

        @Test
        void duplicateIsAConflict() throws Exception {
            willThrow(new DuplicateEnrollmentException(PATIENT_ID, MEDICATION_ID))
                .given(enrollments)
                .enroll(PATIENT_ID, MEDICATION_ID);

            String requestBodyJson = serializeEnrollRequest(mapper, MEDICATION_ID);

            mvc.perform(postJson("/patients/{patientId}/enrollments",
                    PATIENT_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

            then(enrollments).should().enroll(PATIENT_ID, MEDICATION_ID);
        }

        @Test
        void pageHasContentAndPageBlock() throws Exception {
            EnrollmentView enrollmentView = new EnrollmentView(
                ENROLLMENT_ID,
                MedicationView.from(
                    withId(aMedicationWithUniqueCode(), MEDICATION_ID)
                ),
                EnrollmentStatus.APPROVED,
                START_DATE,
                null
            );

            Pageable expected = PageRequest.of(
                1, 5,
                Sort.by(Sort.Order.asc("status"), Sort.Order.asc("id"))
            );

            given(queries.enrollmentsOf(anyValidUUID(), any()))
                .willReturn(
                    new PageImpl<>(
                        List.of(enrollmentView),
                        expected,
                        6
                    )
                );

            mvc.perform(get("/patients/{patientId}/enrollments", PATIENT_ID)
                    .param("page", "1")
                    .param("size", "5")
                    .param("sort", "status,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].medication.name").value(enrollmentView.medication().name()))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.totalPages").value(2));

            then(queries).should().enrollmentsOf(PATIENT_ID, expected);
        }

        @Test
        void listWithoutParametersUsesTheDefaultPaging() throws Exception {
            Pageable expected = PageRequest.of(
                0, 20,
                Sort.by(Sort.Order.desc("startDate"), Sort.Order.asc("id"))
            );

            given(queries.enrollmentsOf(anyValidUUID(), any()))
                .willReturn(
                    new PageImpl<>(List.of(), expected, 0)
                );

            mvc.perform(get("/patients/{patientId}/enrollments", PATIENT_ID))
                .andExpect(status().isOk());

            then(queries).should().enrollmentsOf(PATIENT_ID, expected);
        }

        @Test
        void sortingByAFieldOutsideTheListIsABadRequest() throws Exception {
            mvc.perform(get("/patients/{patientId}/enrollments", PATIENT_ID)
                    .param("sort", "patient.email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot sort by patient.email"));

            then(queries).should(never()).enrollmentsOf(anyValidUUID(), any());
        }

    }

    @Nested
    class Approve {

        @Test
        void approvingPassesTheDateAndAnswersNoContent() throws Exception {
            String requestBodyJson = serializeApproveRequest(
                mapper,
                LocalDate.of(2026, 10, 14)
            );

            mvc.perform(postJson("/enrollments/{id}/approve",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(enrollments).should().approve(ENROLLMENT_ID, TODAY);
        }

        @Test
        void dateInAnotherFormatIsABadRequest() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/approve",
                    ENROLLMENT_ID,
                    """
                    {"startDate":"14.10.2026"}
                    """))
                .andExpect(status().isBadRequest());

            then(enrollments).should(never()).approve(anyValidUUID(), any());
        }

        @Test
        void missingStartDateNamesTheField() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/approve", ENROLLMENT_ID, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));

            then(enrollments).should(never()).approve(anyValidUUID(), any());
        }

        @Test
        void dateRefusedByTheDomainIsABadRequest() throws Exception {
            willThrow(new DomainValidationException("startDate must not be in the future"))
                .given(enrollments).approve(ENROLLMENT_ID, TODAY);

            String requestBodyJson = serializeApproveRequest(
                mapper,
                TODAY
            );

            mvc.perform(postJson("/enrollments/{id}/approve",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("startDate must not be in the future"));

            then(enrollments).should().approve(ENROLLMENT_ID, TODAY);
        }

        @Test
        void concurrentChangeIsAConflict() throws Exception {
            willThrow(new OptimisticLockingFailureException("version mismatch"))
                .given(enrollments).approve(ENROLLMENT_ID, START_DATE);

            String requestBodyJson = serializeApproveRequest(
                mapper,
                START_DATE
            );

            mvc.perform(postJson("/enrollments/{id}/approve",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The record was changed by another request"));

            then(enrollments).should().approve(ENROLLMENT_ID, START_DATE);
        }

    }

    @Nested
    class Complete {

        @Test
        void completingPassesTheDateAndAnswersNoContent() throws Exception {
            String requestBodyJson = serializeCompleteRequest(
                mapper,
                TODAY
            );

            mvc.perform(postJson("/enrollments/{id}/complete",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(enrollments).should().complete(ENROLLMENT_ID, TODAY);
        }

        @Test
        void forbiddenTransitionIsAConflict() throws Exception {
            willThrow(new InvalidEnrollmentTransitionException("complete", EnrollmentStatus.PENDING))
                .given(enrollments).complete(ENROLLMENT_ID, TODAY);

            String requestBodyJson = serializeCompleteRequest(
                mapper,
                TODAY
            );

            mvc.perform(postJson("/enrollments/{id}/complete",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isConflict());

            then(enrollments).should().complete(ENROLLMENT_ID, TODAY);
        }

        @Test
        void missingCloseDateOnCompleteNamesTheField() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/complete", ENROLLMENT_ID, "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("closeDate"));

            then(enrollments).should(never()).complete(anyValidUUID(), any());
        }

    }

    @Nested
    class Reject {

        @Test
        void rejectingPassesTheClosure() throws Exception {
            String requestBodyJson = serializeCloseRequest(
                mapper,
                TODAY,
                CloseReason.OTHER,
                "The other reason"
            );

            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isNoContent());

            then(enrollments).should().reject(
                ENROLLMENT_ID,
                new Closure(CloseReason.OTHER, "The other reason"),
                TODAY
            );
        }

        @Test
        void unknownCloseReasonIsABadRequest() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    """
                    {"closeReason":"NO_SUCH_REASON","closeDate":"2026-10-14"}
                    """))
                .andExpect(status().isBadRequest());

            then(enrollments).should(never()).reject(anyValidUUID(), any(), any());
        }

        @Test
        void tooLongOtherReasonNamesTheField() throws Exception {
            String requestBodyJson = serializeCloseRequest(
                mapper,
                TODAY,
                CloseReason.OTHER,
                "a".repeat(OTHER_REASON_MAX_LENGTH + 1)
            );

            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("otherReason"));

            then(enrollments).should(never()).reject(anyValidUUID(), any(), any());
        }

        @Test
        void missingOtherReasonTextForOtherIsABadRequest() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    """
                    {"closeReason":"OTHER", "closeDate":"2026-10-14"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("otherReason must not be empty"));

            then(enrollments).should(never()).reject(anyValidUUID(), any(), any());
        }

        @Test
        void missingCloseDateOnRejectNamesTheField() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    """
                    {"closeReason":"NOT_ELIGIBLE"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("closeDate"));

            then(enrollments).should(never()).reject(anyValidUUID(), any(), any());
        }

        @Test
        void missingCloseReasonNamesTheField() throws Exception {
            mvc.perform(postJson("/enrollments/{id}/reject",
                    ENROLLMENT_ID,
                    """
                    {"closeDate":"2026-10-14"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("closeReason"));

            then(enrollments).should(never()).reject(anyValidUUID(), any(), any());
        }

    }

    @Nested
    class Cancel {

        @Test
        void cancellingTakesThePatientFromTheHeader() throws Exception {
            String requestBodyJson = serializeCloseRequest(
                mapper,
                TODAY,
                CloseReason.OTHER,
                "The Other Reason Text"
            );

            mvc.perform(postJson("/enrollments/{id}/cancel",
                    ENROLLMENT_ID,
                    requestBodyJson).header("X-Patient-Id", PATIENT_ID))
                .andExpect(status().isNoContent());

            then(enrollments).should()
                .cancelByPatient(
                    ENROLLMENT_ID,
                    PATIENT_ID,
                    new Closure(CloseReason.OTHER, "The Other Reason Text"),
                    TODAY
                );
        }

        @Test
        void cancellingWithoutTheHeaderIsABadRequest() throws Exception {
            String requestBodyJson = serializeCloseRequest(
                mapper,
                TODAY,
                CloseReason.OTHER,
                "The Other Reason Text"
            );

            mvc.perform(postJson("/enrollments/{id}/cancel",
                    ENROLLMENT_ID,
                    requestBodyJson))
                .andExpect(status().isBadRequest());

            then(enrollments).should(never())
                .cancelByPatient(anyValidUUID(), any(), any(), any());
        }

    }

    @Nested
    class GettingById {

        @Test
        void enrollmentIsReturnedAsJson() throws Exception {
            MedicationView medicationView = MedicationView.from(
                withId(aMedicationWithUniqueCode(), MEDICATION_ID)
            );

            given(queries.enrollment(ENROLLMENT_ID))
                .willReturn(new EnrollmentView(
                    ENROLLMENT_ID,
                    medicationView,
                    EnrollmentStatus.APPROVED,
                    START_DATE,
                    null
                ));

            mvc.perform(get("/enrollments/{id}", ENROLLMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID.toString()))
                .andExpect(jsonPath("$.medication.id").value(MEDICATION_ID.toString()))
                .andExpect(jsonPath("$.medication.name").value(medicationView.name()))
                .andExpect(jsonPath("$.medication.code").value(medicationView.code()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.startDate").value("2025-08-10"))
                .andExpect(jsonPath("$.closure").value(nullValue()));

            then(queries).should().enrollment(ENROLLMENT_ID);
        }

        @Test
        void closedEnrollmentIsReturnedAsJson() throws Exception {
            MedicationView medicationView = MedicationView.from(
                withId(aMedicationWithUniqueCode(), MEDICATION_ID)
            );

            given(queries.enrollment(ENROLLMENT_ID))
                .willReturn(new EnrollmentView(
                    ENROLLMENT_ID,
                    medicationView,
                    EnrollmentStatus.CANCELLED,
                    START_DATE,
                    new ClosureView(
                        CloseReason.OTHER,
                        OTHER_REASON,
                        CLOSE_DATE
                    )
                ));

            mvc.perform(get("/enrollments/{id}", ENROLLMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.closure.closeReason").value("OTHER"))
                .andExpect(jsonPath("$.closure.otherReason").value("Other Reason Text"))
                .andExpect(jsonPath("$.closure.closeDate").value("2026-09-01"));

            then(queries).should().enrollment(ENROLLMENT_ID);
        }

        @Test
        void unknownEnrollmentIdIsNotFound() throws Exception {
            willThrow(new EnrollmentNotFoundException(ENROLLMENT_ID))
                .given(queries)
                .enrollment(ENROLLMENT_ID);

            mvc.perform(get("/enrollments/{id}", ENROLLMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Enrollment not found: " + ENROLLMENT_ID));

            then(queries).should().enrollment(ENROLLMENT_ID);
        }

        @Test
        void unexpectedErrorHidesItsMessage() throws Exception {
            willThrow(new IllegalStateException("password=secret"))
                .given(queries)
                .enrollment(ENROLLMENT_ID);

            mvc.perform(get("/enrollments/{id}", ENROLLMENT_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error"))
                .andExpect(content().string(not(containsString("secret"))));

            then(queries).should().enrollment(ENROLLMENT_ID);
        }

    }

    private static String serializeEnrollRequest(JsonMapper mapper, UUID medicationId) {
        Map<String, Object> enrollRequest = Map.of(
            "medicationId", medicationId
        );

        return jsonSerialize(mapper, enrollRequest);
    }

    private static String serializeApproveRequest(JsonMapper mapper, LocalDate startDate) {
        Map<String, Object> approveRequest = Map.of(
            "startDate", startDate
        );

        return jsonSerialize(mapper, approveRequest);
    }

    private static String serializeCompleteRequest(JsonMapper mapper, LocalDate closeDate) {
        Map<String, Object> completeRequest = Map.of(
            "closeDate", closeDate
        );

        return jsonSerialize(mapper, completeRequest);
    }

    private static String serializeCloseRequest(
        JsonMapper mapper,
        LocalDate closeDate,
        CloseReason closeReason,
        String otherReason
    ) {
        Map<String, Object> closeRequest = Map.of(
            "closeDate", closeDate,
            "closeReason", closeReason,
            "otherReason", otherReason
        );

        return jsonSerialize(mapper, closeRequest);
    }

}
