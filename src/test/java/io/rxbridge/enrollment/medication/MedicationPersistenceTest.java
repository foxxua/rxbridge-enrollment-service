package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import jakarta.persistence.PersistenceException;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.medication.MedicationConstants.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MedicationPersistenceTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Nested
    class Mapping {

        @ParameterizedTest
        @MethodSource("medicationVariants")
        void readsBackEveryField(Medication medication) {
            Medication found = entityManager.persistFlushFind(medication);

            assertThat(found)
                .isNotSameAs(medication)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(medication);
        }

        @Test
        void readsBackValuesOfMaxLength() {
            Medication found = entityManager.persistFlushFind(aMedicationWithMaxLengthFields());

            assertSoftly(softly -> {
                softly.assertThat(found.getName()).hasSize(NAME_MAX_LENGTH);
                softly.assertThat(found.getDescription()).hasSize(DESCRIPTION_MAX_LENGTH);
                softly.assertThat(found.getCode()).hasSize(CODE_LENGTH);
                softly.assertThat(found.getSideEffects()).hasSize(SIDE_EFFECTS_MAX_LENGTH);
            });
        }

        static Stream<Named<Medication>> medicationVariants() {
            return Stream.of(
                Named.of("Aspirin, active", aMedication()),
                Named.of("Aspirin, inactive", anInactiveMedication()),
                Named.of("Analgin, active", anAnalginMedication()),
                Named.of("Ibuprofen, inactive", anInactiveIbuprofenMedication()),
                Named.of("Aspirin, description null, side effects null", aMedicationWithNullableFields())
            );
        }

    }

    @Nested
    class DatabaseConstraints {

        @Test
        void acceptsValidRow() {
            assertThat(insertMedication(Map.of())).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0} = ''{1}'' -> {2}")
        @CsvSource(delimiter = '|', textBlock = """
            name         | '   '                   | chk_medication_name_not_blank_trimmed
            name         | ' Aspirin'              | chk_medication_name_not_blank_trimmed
            name         | 'Aspirin '              | chk_medication_name_not_blank_trimmed
            description  | '   '                   | chk_medication_description_not_blank_trimmed
            description  | 'Test Description '     | chk_medication_description_not_blank_trimmed
            description  | ' Test Description'     | chk_medication_description_not_blank_trimmed
            code         | '   '                   | chk_medication_code
            code         | 'M12345'                | chk_medication_code
            code         | '1111111'               | chk_medication_code
            code         | 'MMMMMMM'               | chk_medication_code
            code         | '1MMMMMM'               | chk_medication_code
            code         | 'A123456'               | chk_medication_code
            code         | 'm123456'               | chk_medication_code
            side_effects | '   '                   | chk_medication_side_effects_not_blank_trimmed
            side_effects | 'Test Side Effects '    | chk_medication_side_effects_not_blank_trimmed
            side_effects | ' Test Side Effects'    | chk_medication_side_effects_not_blank_trimmed
            """)
        void rejectsInvalidValue(String column, String value, String constraint) {
            assertThatThrownBy(() -> insertMedication(Map.of(column, value)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
        }

        @Test
        void rejectsSideEffectsWithTrailingNewline() {
            String sideEffects = """
                Regular intake can lead to heartburn, nausea, or general stomach discomfort.
                It may elevate blood pressure and increase fluid retention in the body.
                Frequent high doses can negatively affect kidney function over time.
                """;

            assertThatThrownBy(() -> insertMedication(Map.of("side_effects", sideEffects)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_medication_side_effects_not_blank_trimmed");
        }

        @ParameterizedTest
        @ValueSource(strings = {"M123456", "M000000", "M999999"})
        void acceptsValidCode(String input) {
            assertThatCode(() -> entityManager.persistAndFlush(
                        aMedicationBuilder().code(input).build()
                    ))
                .doesNotThrowAnyException();
        }

        @Test
        void rejectsDuplicateCode() {
            entityManager.persistAndFlush(
                aMedicationBuilder()
                    .name("Aspirin")
                    .code("M123456")
                    .build()
            );

            assertThatThrownBy(() -> {
                        entityManager.persistAndFlush(
                            aMedicationBuilder()
                                .name("Analgin")
                                .code("M123456")
                                .build()
                        );
                    })
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("uq_medication_code");
        }

        @Test
        void rejectsDescriptionTooLong() {
            assertThatThrownBy(() -> insertMedication(
                    Map.of("description",
                    "a".repeat(DESCRIPTION_MAX_LENGTH + 1))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(%d)".formatted(DESCRIPTION_MAX_LENGTH));
        }

        @Test
        void rejectsSideEffectsTooLong() {
            assertThatThrownBy(() -> insertMedication(
                Map.of("side_effects",
                    "a".repeat(SIDE_EFFECTS_MAX_LENGTH + 1))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(%d)".formatted(SIDE_EFFECTS_MAX_LENGTH));
        }

        private int insertMedication(Map<String, Object> overrides) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",        "Aspirin");
            row.put("description", "A nonsteroidal anti-inflammatory drug " +
                "used to reduce pain, fever, and inflammation, as well as to prevent blood clots.");
            row.put("code",        "M111111");
            row.put("side_effects", """
                It can cause irritation and bleeding in the stomach lining.
                Long-term use may increase the risk of gastrointestinal ulcers.
                It can trigger allergic reactions or asthma attacks in sensitive individuals.""");
            row.put("active",      true);
            row.putAll(overrides);

            String columns = String.join(", ", row.keySet());
            String params = row.keySet().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

            return jdbc.update(
                "INSERT INTO medication (id, version, created_at, updated_at, " + columns + ") "
                + "VALUES (gen_random_uuid(), 0, now(), now(), " + params + ")",
                row.values().toArray()
            );
        }

    }

}
