package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MedicationQueriesTest {

    @Autowired
    MedicationQueries queries;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate enrollment, medication, patient");
    }

    @Test
    void medicationFoundById() {
        Medication medication = aMedication();
        UUID id = persisted(medication);

        MedicationView medicationView = queries.medication(id);

        assertThat(medicationView).isEqualTo(MedicationView.from(medication));
    }

    @Test
    void unknownMedicationIdIsNotFound() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000404");

        assertThatThrownBy(() -> queries.medication(unknown))
            .isInstanceOfSatisfying(
                MedicationNotFoundException.class,
                e -> assertThat(e.medicationId()).isEqualTo(unknown)
            );
    }

    @Test
    void medicationFountByCode() {
        Medication medication = aMedication();
        persisted(medication);

        MedicationView medicationView = queries.medication(medication.getCode());

        assertThat(medicationView).isEqualTo(MedicationView.from(medication));
    }

    @Test
    void unknownMedicationCodeIsNotFound() {
        String unknown = "M999999";

        assertThatThrownBy(() -> queries.medication(unknown))
            .isInstanceOfSatisfying(
                MedicationNotFoundByCodeException.class,
                e -> assertThat(e.code()).isEqualTo(unknown)
            );
    }

    private UUID persisted(Medication medication) {
        return tx.execute(status -> {
            entityManager.persist(medication);
            return medication.getId();
        });
    }

}
