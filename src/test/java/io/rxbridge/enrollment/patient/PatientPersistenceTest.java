package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.patient.PatientConstants.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class PatientPersistenceTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Nested
    class Mapping {

        @ParameterizedTest(name = "{0}")
        @MethodSource("patientVariants")
        void readsBackEveryField(Patient patient) {
            Patient foundPatient = entityManager.persistFlushFind(patient);

            assertThat(foundPatient)
                .isNotSameAs(patient)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(patient);
        }

        @Test
        void readsBackValuesOfMaxLength() {
            Patient foundPatient = entityManager.persistFlushFind(aPatientWithMaxLengthFields());

            assertSoftly(softly -> {
                softly.assertThat(foundPatient.getPersonName().firstName()).hasSize(NAME_MAX_LENGTH);
                softly.assertThat(foundPatient.getPersonName().lastName()).hasSize(NAME_MAX_LENGTH);
                softly.assertThat(foundPatient.getEmail()).hasSize(EMAIL_MAX_LENGTH);
                softly.assertThat(foundPatient.getAddress().street()).hasSize(STREET_MAX_LENGTH);
                softly.assertThat(foundPatient.getAddress().city()).hasSize(CITY_MAX_LENGTH);
                softly.assertThat(foundPatient.getAddress().country()).hasSize(COUNTRY_MAX_LENGTH);
                softly.assertThat(foundPatient.getAddress().region()).hasSize(REGION_MAX_LENGTH);
                softly.assertThat(foundPatient.getAddress().zipcode()).hasSize(ZIPCODE_MAX_LENGTH);
            });
        }

        static Stream<Named<Patient>> patientVariants() {
            return Stream.of(
                Named.of("male, active, region"     , aPatient()),
                Named.of("female, active, no region", aFemalePatientWithoutRegion()),
                Named.of("unknown, inactive, region", anInactiveUnknownSexPatient())
            );
        }

    }

    @Nested
    class GeneratedFields {

        @Test
        void assignsIdVersionAndTimestampsOnInsert() {
            Patient savedPatient = entityManager.persistAndFlush(aPatient());

            assertSoftly(softly -> {
                softly.assertThat(savedPatient.getId()).isNotNull();
                softly.assertThat(savedPatient.getVersion()).isZero();
                softly.assertThat(savedPatient.getCreatedAt()).isNotNull();
                softly.assertThat(savedPatient.getUpdatedAt()).isNotNull();
            });
        }

        @Test
        void incrementsVersionAndKeepsCreatedAtOnUpdate() {
            Patient savedPatient = entityManager.persistAndFlush(aPatient());
            Instant createdAt = savedPatient.getCreatedAt();
            Instant updatedAt = savedPatient.getUpdatedAt();

            savedPatient.changeEmail("agatha_christie@example.com");
            entityManager.flush();
            entityManager.clear();

            Patient foundPatient = entityManager.find(Patient.class, savedPatient.getId());

            assertSoftly(softly -> {
                softly.assertThat(foundPatient.getEmail()).isEqualTo("agatha_christie@example.com");
                softly.assertThat(foundPatient.getVersion()).isEqualTo(1);
                softly.assertThat(foundPatient.getCreatedAt())
                    .isCloseTo(createdAt, within(1, ChronoUnit.MILLIS));
                softly.assertThat(foundPatient.getUpdatedAt())
                    .isAfter(updatedAt);
            });
        }

        @Test
        void entityEqualsItsProxy() {
            Patient savedPatient = entityManager.persistAndFlush(aPatient());
            entityManager.clear();

            Patient proxy = entityManager.getEntityManager().getReference(Patient.class, savedPatient.getId());

            assertThat(proxy.getClass()).isNotEqualTo(Patient.class);
            assertThat(proxy).isEqualTo(savedPatient);
            assertThat(proxy.hashCode()).isEqualTo(savedPatient.hashCode());
        }

    }

    @Nested
    class DatabaseConstraints {

        @Test
        void acceptsValidRow() {
            assertThat(insertPatient(Map.of())).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0} = ''{1}'' -> {2}")
        @CsvSource(delimiter = '|', textBlock = """
                first_name        | '   '                 | chk_patient_first_name_not_blank_trimmed
                first_name        | ' William '           | chk_patient_first_name_not_blank_trimmed
                last_name         | '   '                 | chk_patient_last_name_not_blank_trimmed
                last_name         | ' Shakespeare '       | chk_patient_last_name_not_blank_trimmed
                email             | '   '                 | chk_patient_email
                email             | ' a@example.com'      | chk_patient_email
                email             | 'a@example.com '      | chk_patient_email
                email             | 'John@example.com'    | chk_patient_email
                email             | '@example.com'        | chk_patient_email
                email             | 'інвалід@example.com' | chk_patient_email
                email             | 'invalidemail'        | chk_patient_email
                email             | 'a@@example.com'      | chk_patient_email
                email             | 'a@example'           | chk_patient_email
                email             | 'a@example.c'         | chk_patient_email
                email             | 'a@example.12'        | chk_patient_email
                address_street    | '   '                 | chk_patient_address_street_not_blank_trimmed
                address_street    | ' 31 Milk Street'     | chk_patient_address_street_not_blank_trimmed
                address_city      | '   '                 | chk_patient_address_city_not_blank_trimmed
                address_city      | 'Boston '             | chk_patient_address_city_not_blank_trimmed
                address_country   | ' A'                 | chk_patient_address_country
                address_country   | 'A'                   | chk_patient_address_country
                address_country   | 'au'                  | chk_patient_address_country
                address_country   | 'A1'                  | chk_patient_address_country
                address_country   | 'ФР'                  | chk_patient_address_country
                address_region    | '   '                 | chk_patient_address_region_not_blank_trimmed
                address_region    | ' MA '                | chk_patient_address_region_not_blank_trimmed
                address_zipcode   | '   '                 | chk_patient_address_zipcode_not_blank_trimmed
                address_zipcode   | '02196 '              | chk_patient_address_zipcode_not_blank_trimmed
                sex               | 'male'                | chk_patient_sex
                sex               | 'OTHER'               | chk_patient_sex
                """)
        void rejectsInvalidValue(String column, String value, String constraint) {
            assertThatThrownBy(() -> insertPatient(Map.of(column, value)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "j.o_h%n+d-1@example.com", "john@mail.example.com",
            "john@my-site2.com", "john@example.co"
        })
        void acceptsValidEmail(String email) {
            assertThatCode(
                    () -> entityManager.persistAndFlush(
                            aPatientBuilder().email(email).build()
                        )
                    )
                .doesNotThrowAnyException();
        }

        private int insertPatient(Map<String, Object> overrides) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("first_name"      , "William");
            row.put("last_name"       , "Shakespeare");
            row.put("email"           , "william_shakespeare@example.com");
            row.put("address_street"  , "31 Milk Street");
            row.put("address_city"    , "Boston");
            row.put("address_country" , "US");
            row.put("address_region"  , "MA");
            row.put("address_zipcode" , "02196");
            row.put("sex"             , "MALE");
            row.put("birthdate"       , BIRTHDATE);
            row.put("active"          , true);
            row.putAll(overrides);

            String columns = String.join(", ", row.keySet());
            String params = row.keySet().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

            return jdbc.update(
                "INSERT INTO patient (id, version, created_at, updated_at, " + columns + ") "
                    + "VALUES (gen_random_uuid(), 0, now(), now(), " + params + ")",
                row.values().toArray()
            );
        }

    }

}
