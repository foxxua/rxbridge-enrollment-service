package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.patient.Patient;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;
import static io.rxbridge.enrollment.common.TestDates.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EnrollmentPersistenceTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Nested
    class Mapping {

        @ParameterizedTest(name = "{0}")
        @MethodSource("enrollmentVariants")
        void readsBackEveryField(Enrollment enrollment) {
            Enrollment found = persistAndReload(enrollment);

            assertThat(found)
                .isNotSameAs(enrollment)
                .usingRecursiveComparison()
                .ignoringFields("patient", "medication", "createdAt", "updatedAt")
                .isEqualTo(enrollment);
            assertThat(found.getPatient()).isEqualTo(enrollment.getPatient());
            assertThat(found.getMedication()).isEqualTo(enrollment.getMedication());
        }

        @Test
        void readsBackOtherReasonOfMaxLength() {
            Enrollment enrollment = anEnrollment();
            enrollment.cancel(
                new Closure(CloseReason.OTHER, "a".repeat(OTHER_REASON_MAX_LENGTH)),
                TODAY,
                TODAY
            );
            Enrollment found = persistAndReload(enrollment);

            assertThat(found.getOtherReason()).hasSize(OTHER_REASON_MAX_LENGTH);
        }

        @Test
        void writesDatesToTheirColumns() {
            Enrollment enrollment = anEnrollment();
            enrollment.approve(TODAY.minusDays(3));
            enrollment.complete(TODAY.minusDays(1), TODAY);
            UUID id = persistAndReload(enrollment).getId();

            LocalDate startDate = jdbc.queryForObject(
                "SELECT start_date FROM enrollment WHERE id = ?",
                LocalDate.class,
                id
            );
            LocalDate closeDate = jdbc.queryForObject(
                "SELECT close_date FROM enrollment WHERE id = ?",
                LocalDate.class,
                id
            );

            assertThat(startDate).isEqualTo(TODAY.minusDays(3));
            assertThat(closeDate).isEqualTo(TODAY.minusDays(1));
        }

        static Stream<Named<Enrollment>> enrollmentVariants() {
            Stream<Named<Enrollment>> everyStatus =
                Arrays.stream(EnrollmentStatus.values())
                    .map(status -> Named.of(status.name(), anEnrollmentIn(status)));

            Stream<Named<Enrollment>> reasonsNotInStatusFixtures = Stream.of(
                Named.of("CANCELLED, PATIENT_DEACTIVATED", aCancelledEnrollmentBecausePatientDeactivated()),
                Named.of("CANCELLED, MEDICATION_DEACTIVATED", aCancelledEnrollmentBecauseMedicationDeactivated())
            );

            return Stream.concat(everyStatus, reasonsNotInStatusFixtures);
        }

    }

    @Nested
    class DatabaseConstraints {

        UUID patientId;
        UUID medicationId;

        @BeforeEach
        void persistPatientAndMedication() {
            patientId    = entityManager.persistAndFlush(aPatient()).getId();
            medicationId = entityManager.persistAndFlush(aMedication()).getId();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(EnrollmentStatus.class)
        void acceptsValidRowInEveryStatus(EnrollmentStatus status) {
            assertThat(insertEnrollment(validRow(status))).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0}: {1} = {2} -> {3}")
        @CsvSource(delimiter = '|', textBlock = """
            # status  | column       | value                  | constraint                                  #
            #-----------------------------------------------------------------------------------------------#
            PENDING   | start_date   | 2025-08-10             | chk_enrollment_start_date_null_or_not
            APPROVED  | start_date   |                        | chk_enrollment_start_date_null_or_not
            COMPLETED | start_date   |                        | chk_enrollment_start_date_null_or_not
            REJECTED  | start_date   | 2025-08-10             | chk_enrollment_start_date_null_or_not
            PENDING   | close_date   | 2026-09-01             | chk_enrollment_close_date_null_or_not
            APPROVED  | close_date   | 2026-09-01             | chk_enrollment_close_date_null_or_not
            COMPLETED | close_date   |                        | chk_enrollment_close_date_null_or_not
            CANCELLED | close_date   |                        | chk_enrollment_close_date_null_or_not
            REJECTED  | close_date   |                        | chk_enrollment_close_date_null_or_not
            COMPLETED | close_date   | 2025-08-09             | chk_enrollment_close_after_start
            REJECTED  | close_reason | REFUSED                | chk_enrollment_close_reason
            COMPLETED | close_reason | MEDICATION_DEACTIVATED | chk_enrollment_close_reason_status
            REJECTED  | close_reason | PATIENT_DEACTIVATED    | chk_enrollment_close_reason_status
            REJECTED  | close_reason | THERAPY_COMPLETED      | chk_enrollment_close_reason_status
            COMPLETED | close_reason |                        | chk_enrollment_close_reason_null_or_not
            REJECTED  | close_reason |                        | chk_enrollment_close_reason_null_or_not
            REJECTED  | other_reason | 'Other Reason Text'    | chk_enrollment_other_reason_null_or_not
            CANCELLED | other_reason |                        | chk_enrollment_other_reason_null_or_not
            CANCELLED | other_reason | '   '                  | chk_enrollment_other_reason_not_blank_trimmed
            CANCELLED | other_reason | 'Other Reason Text '   | chk_enrollment_other_reason_not_blank_trimmed
            CANCELLED | other_reason | ' Other Reason Text'   | chk_enrollment_other_reason_not_blank_trimmed
            """)
        void rejectsRowBreakingOneRule(
            EnrollmentStatus status,
            String column,
            String value,
            String constraint
        ) {
            Map<String, Object> row = validRow(status);
            row.put(column, toColumnValue(column, value));

            assertThatThrownBy(() -> insertEnrollment(row))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("\"" + constraint + "\"");
        }

        @Test
        void rejectsUnknownStatus() {
            Map<String, Object> row = validRow(EnrollmentStatus.PENDING);
            row.put("status", "REFUSED");

            assertThatThrownBy(() -> insertEnrollment(row))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("violates check constraint");
        }

        @Test
        void rejectsOtherReasonTooLong() {
            Map<String, Object> row = validRow(EnrollmentStatus.CANCELLED);
            row.put("other_reason", "a".repeat(OTHER_REASON_MAX_LENGTH + 1));

            assertThatThrownBy(() -> insertEnrollment(row))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(%d)".formatted(OTHER_REASON_MAX_LENGTH));
        }

        private Map<String, Object> validRow(EnrollmentStatus status) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("patient_id", patientId);
            row.put("medication_id", medicationId);
            row.put("status", status.name());
            row.putAll(
                switch (status) {
                    case PENDING   -> Map.of();
                    case APPROVED  -> Map.of("start_date", START_DATE);
                    case COMPLETED -> Map.of("start_date", START_DATE,
                                             "close_date", CLOSE_DATE,
                                             "close_reason", "THERAPY_COMPLETED");
                    case REJECTED  -> Map.of("close_date", CLOSE_DATE,
                                             "close_reason", "NOT_ELIGIBLE");
                    case CANCELLED -> Map.of("close_date", CLOSE_DATE,
                                             "close_reason", "OTHER",
                                             "other_reason", OTHER_REASON);
                }
            );

            return row;
        }

        private int insertEnrollment(Map<String, Object> row) {
            String columns = String.join(", ", row.keySet());
            String params = row.keySet().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

            return jdbc.update(
                """
                INSERT INTO enrollment (id, version, created_at, updated_at, %s)
                VALUES (gen_random_uuid(), 0, now(), now(), %s)
                """.formatted(columns, params),
                row.values().toArray()
            );
        }

        private static Object toColumnValue(String column, String value) {
            boolean isDate = column.equals("start_date") || column.equals("close_date");

            return (isDate && value != null)
                ? LocalDate.parse(value)
                : value;
        }

    }

    @Nested
    class PartialUniqueIndex {

        Patient patient;
        Medication medication;

        @BeforeEach
        void persistPatientAndMedication() {
            patient    = entityManager.persistAndFlush(aPatient());
            medication = entityManager.persistAndFlush(aMedication());
        }

        @ParameterizedTest(name = "first one {0}")
        @MethodSource("toOpenStatus")
        void rejectsSecondOpenEnrollmentOfSamePatientAndMedication(Consumer<Enrollment> moveFirst) {
            Enrollment first = new Enrollment(patient, medication);
            moveFirst.accept(first);
            entityManager.persistAndFlush(first);

            assertThatThrownBy(() -> entityManager.persistAndFlush(new Enrollment(patient, medication)))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("\"uq_enrollment_patient_medication_active\"");
        }

        @ParameterizedTest(name = "first one {0}")
        @MethodSource("toCloseStatus")
        void acceptsNewEnrollmentAfterCloseOne(Consumer<Enrollment> closeFirst) {
            Enrollment first = new Enrollment(patient, medication);
            closeFirst.accept(first);
            entityManager.persistAndFlush(first);

            assertThatCode(() -> entityManager.persistAndFlush(new Enrollment(patient, medication)))
                .doesNotThrowAnyException();
        }

        static Stream<Named<Consumer<Enrollment>>> toOpenStatus() {
            return Stream.of(
                Named.of("PENDING" , r -> { }),
                Named.of("APPROVED", r -> r.approve(START_DATE))
            );
        }

        static Stream<Named<Consumer<Enrollment>>> toCloseStatus() {
            return Stream.of(
                Named.of("COMPLETED", r -> {
                    r.approve(START_DATE);
                    r.complete(CLOSE_DATE, TODAY);
                }),
                Named.of("REJECTED" , r -> r.reject(REJECT_CLOSURE, CLOSE_DATE, TODAY)),
                Named.of("CANCELLED", r -> r.cancel(CLOSURE_OTHER, CLOSE_DATE, TODAY))
            );
        }

    }

    private Enrollment persistAndReload(Enrollment enrollment) {
        entityManager.persist(enrollment.getPatient());
        entityManager.persist(enrollment.getMedication());
        entityManager.persist(enrollment);
        entityManager.flush();
        entityManager.clear();

        return entityManager.find(Enrollment.class, enrollment.getId());
    }

}
