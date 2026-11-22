package io.rxbridge.enrollment.enrollment;

import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import io.rxbridge.enrollment.TestcontainersConfiguration;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.patient.Patient;
import jakarta.persistence.EntityManager;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EnrollmentApiContractTest {

    private static final Path SNAPSHOT = Path.of("src/test/resources/openapi/enrollment-service.json");

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    @Qualifier("businessClock")
    Clock businessClock;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate enrollment, medication, patient");
    }

    @Test
    @Disabled("See PLAN.md technical debt")
    void documentMatchesTheReviewedSnapshot() throws Exception {
        String actual = document();
        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, new JSONObject(actual).toString(2));
        }

        JSONAssert.assertEquals(
            Files.readString(SNAPSHOT),
            actual,
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @Disabled("See PLAN.md technical debt")
    void realResponsesMatchTheDocument() throws Exception {
        ResultMatcher matchesDocument = OpenApiValidationMatchers.openApi().isValid(document());
        Patient patient = persisted(aPatient());
        Medication medication = persisted(aMedication());

        String body = """
            {"medicationId":"%s"}
            """.formatted(medication.getId());

        String location = mvc.perform(post("/patients/{patientId}/enrollments", patient.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(matchesDocument)
            .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(matchesDocument);

        mvc.perform(get("/patients/{id}/enrollments", patient.getId()))
            .andExpect(status().isOk())
            .andExpect(matchesDocument);

        mvc.perform(post("/patients/{id}/enrollments", patient.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(matchesDocument);

        mvc.perform(post(location + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startDate": "%s"}
                        """.formatted(LocalDate.now(businessClock))))
            .andExpect(status().isNoContent())
            .andExpect(matchesDocument);

        mvc.perform(post("/patients/{id}/enrollments", patient.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        mvc.perform(get("/enrollments/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(matchesDocument);
    }

    private String document() throws Exception {
        return mvc.perform(get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private <T> T persisted(T entity) {
        tx.executeWithoutResult(status -> entityManager.persist(entity));
        return entity;
    }

}
