package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.medication.MedicationView;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EnrollmentQueriesTest {

    @Autowired
    EnrollmentQueries queries;

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
    void enrollmentFoundByIdContainsMedication() {
        Enrollment enrollment = anEnrollment();
        UUID id = persisted(enrollment);

        EnrollmentView view = queries.enrollment(id);

        assertThat(view.medication())
            .isEqualTo(MedicationView.from(enrollment.getMedication()));
    }

    @Test
    void patientPageContainsMedicationNames() {
        Enrollment enrollment = anEnrollment();
        persisted(enrollment);

        Page<EnrollmentView> page = queries.enrollmentsOf(
            enrollment.getPatient().getId(),
            PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
            .extracting(EnrollmentView::medication)
            .containsExactly(MedicationView.from(enrollment.getMedication()));
    }

    @Test
    void unknownEnrollmentIsNotFound() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000404");

        assertThatThrownBy(() -> queries.enrollment(unknown))
            .isInstanceOfSatisfying(
                EnrollmentNotFoundException.class,
                e -> assertThat(e.enrollmentId()).isEqualTo(unknown)
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

}
