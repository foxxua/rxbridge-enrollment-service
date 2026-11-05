package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import io.rxbridge.enrollment.medication.MedicationService;
import io.rxbridge.enrollment.patient.Patient;
import io.rxbridge.enrollment.patient.PatientService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EnrollmentServiceTransactionTest {

    @Autowired
    EnrollmentService service;

    @Autowired
    PatientService patientService;

    @Autowired
    MedicationService medicationService;

    @Autowired
    EnrollmentRepository enrollments;

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
    void approvingIsSaved() {
        UUID id = persisted(anEnrollment());

        service.approve(id, TODAY);

        assertThat(statusOf(id)).isEqualTo(EnrollmentStatus.APPROVED);
    }

    @Test
    void enrollReturnsIdOfSavedEnrollment() {
        UUID patientId = persistedEntity(aPatient()).getId();
        UUID medicationId = persistedEntity(aMedication()).getId();

        UUID id = service.enroll(patientId, medicationId);

        assertThat(statusOf(id)).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void deactivatePatientCancelsTheirOpenEnrollments() {
        Enrollment enrollment = anEnrollment();
        UUID id = persisted(enrollment);

        patientService.deactivate(enrollment.getPatient().getId());

        assertThat(statusOf(id)).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void deactivateMedicationCancelsItsOpenEnrollments() {
        Enrollment enrollment = anEnrollment();
        UUID id = persisted(enrollment);

        medicationService.deactivate(enrollment.getMedication().getId());

        assertThat(statusOf(id)).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void parallelEnrollmentsOfTheSameMedicationGiveOneDuplicate() {
        Patient patient = persistedEntity(aPatient());
        UUID medicationId = persistedEntity(aMedicationWithUniqueCode()).getId();

        CountDownLatch start = new CountDownLatch(1);
        Callable<String> attempt = () -> {
            start.await();
            service.enroll(patient.getId(), medicationId);
            return "enrolled";
        };

        List<String> outcomes;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = List.of(
                executor.submit(attempt),
                executor.submit(attempt)
            );
            start.countDown();
            outcomes = futures.stream().map(EnrollmentServiceTransactionTest::outcomeOf).toList();
        }

        assertThat(outcomes).containsExactlyInAnyOrder(
            "enrolled",
            DuplicateEnrollmentException.class.getSimpleName()
        );
    }

    private UUID persisted(Enrollment enrollment) {
        return tx.execute(status -> {
            entityManager.persist(enrollment.getPatient());
            entityManager.persist(enrollment.getMedication());
            entityManager.persist(enrollment);
            return enrollment.getId();
        });
    }

    private <T> T persistedEntity(T entity) {
        tx.executeWithoutResult(status -> entityManager.persist(entity));
        return entity;
    }

    private EnrollmentStatus statusOf(UUID id) {
        return tx.execute(status -> enrollments.findById(id).orElseThrow().getStatus());
    }

    private static String outcomeOf(Future<String> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause().getClass().getSimpleName();
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError("enrollment attempt did not finish", e);
        }
    }

}
