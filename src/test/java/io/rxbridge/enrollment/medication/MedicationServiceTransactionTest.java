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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.medication.MedicationFixtures.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MedicationServiceTransactionTest {

    @Autowired
    MedicationService service;

    @Autowired
    MedicationRepository medications;

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
    void registerReturnsIdOfSavedMedication() {
        Medication medication = aMedication();

        UUID id = service.register(
            medication.getName(),
            medication.getDescription(),
            medication.getCode(),
            medication.getSideEffects()
        );

        assertThat(id).isNotNull();

        Optional<Medication> found = medications.findById(id);
        assertThat(found)
            .hasValueSatisfying(m -> {
                assertThat(m.getId()).isEqualTo(id);
                assertThat(m.getName()).isEqualTo(medication.getName());
                assertThat(m.getDescription()).isEqualTo(medication.getDescription());
                assertThat(m.getCode()).isEqualTo(medication.getCode());
                assertThat(m.getSideEffects()).isEqualTo(medication.getSideEffects());
            });
    }

    @Test
    void refusesRegistrationIfTheCodeExists() {
        Medication medication = aMedication();
        persisted(medication);

        assertThatThrownBy(() -> service.register(
                medication.getName(),
                medication.getDescription(),
                medication.getCode(),
                medication.getSideEffects()))
            .isInstanceOfSatisfying(
                DuplicateMedicationException.class,
                e -> assertThat(e.code()).isEqualTo(medication.getCode())
            );
    }

    @Test
    void parallelRegistrationWithSameCodeGiveOneDuplicate() {
        Medication medication = aMedication();

        CountDownLatch start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            start.await();
            service.register(
                medication.getName(),
                medication.getDescription(),
                medication.getCode(),
                medication.getSideEffects()
            );
            return "registered";
        };

        List<String> outcomes;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = List.of(
                executor.submit(attempt),
                executor.submit(attempt)
            );
            start.countDown();
            outcomes = futures
                .stream()
                .map(MedicationServiceTransactionTest::outcomeOf)
                .toList();
        }

        assertThat(outcomes)
            .containsExactlyInAnyOrder(
                "registered",
                DuplicateMedicationException.class.getSimpleName()
            );
    }

    private UUID persisted(Medication medication) {
        return tx.execute(status -> {
            entityManager.persist(medication);
            return medication.getId();
        });
    }

    private static String outcomeOf(Future<String> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause().getClass().getSimpleName();
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError("registration attempt did not finish", e);
        }
    }

}
