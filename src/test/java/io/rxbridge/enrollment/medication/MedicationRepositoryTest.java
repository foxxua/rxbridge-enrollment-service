package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MedicationRepositoryTest {

    @Autowired
    MedicationRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findsValidCode() {
        persist("Aspirin", "M000001");
        Medication medication = persist("Analgin", "M000002");
        persist("Ibuprofen", "M000003");
        flushAndClear();

        Optional<Medication> found = repository.findByCode("M000002");

        assertThat(found).hasValue(medication);
    }

    @Test
    void returnsEmptyForUnknownCode() {
        persist("Aspirin", "M000001");
        persist("Ibuprofen", "M000003");
        flushAndClear();

        Optional<Medication> found = repository.findByCode("M000002");

        assertThat(found).isNotPresent();
    }

    private Medication persist(String name, String code) {
        return entityManager.persist(
            aMedicationBuilder()
                .name(name)
                .code(code)
                .build()
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
