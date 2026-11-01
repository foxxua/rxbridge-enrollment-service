package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.patient.Patient;
import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.enrollment.EnrollmentFixtures.*;
import static io.rxbridge.enrollment.medication.MedicationFixtures.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;
import static io.rxbridge.enrollment.common.TestDates.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class EnrollmentRepositoryTest {

    @Autowired
    EnrollmentRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findsOpenEnrollmentOfSamePatientAndMedication() {
        Enrollment enrollment = persistWithParents(anEnrollment());
        flushAndClear();

        boolean found = repository.hasOpenEnrollment(
            enrollment.getPatient().getId(),
            enrollment.getMedication().getId()
        );

        assertThat(found).isTrue();
    }

    @Test
    void ignoresOtherPatientAndOtherMedication() {
        Patient patient = entityManager.persist(aPatient());
        Medication medication = entityManager.persist(anAnalginMedication());
        persistWithParents(anEnrollmentBuilder().patient(patient).build());
        persistWithParents(anEnrollmentBuilder().medication(medication).build());
        flushAndClear();

        boolean found = repository.hasOpenEnrollment(patient.getId(), medication.getId());

        assertThat(found).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EnrollmentStatus.class)
    void countsOnlyOpenStatuses(EnrollmentStatus status) {
        Enrollment enrollment = persistWithParents(anEnrollmentIn(status));
        flushAndClear();

        boolean found = repository.hasOpenEnrollment(
            enrollment.getPatient().getId(),
            enrollment.getMedication().getId()
        );

        assertThat(found).isEqualTo(status.isOpen());
    }

    @ParameterizedTest(name = "first one {0}")
    @EnumSource(EnrollmentStatus.class)
    void agreesWithPartialUniqueIndex(EnrollmentStatus status) {
        Enrollment first = persistWithParents(anEnrollmentIn(status));
        entityManager.flush();
        boolean queryFindsOpen = repository.hasOpenEnrollment(
            first.getPatient().getId(),
            first.getMedication().getId()
        );

        Throwable thrown = catchThrowable(() ->
            repository.saveAndFlush(new Enrollment(first.getPatient(), first.getMedication())));

        assertThat(thrown != null)
            .as("index rejects a second enrollment exactly when the query sees an open one")
            .isEqualTo(queryFindsOpen);
    }

    @Test
    void reportsIndexNameWhenSecondOpenEnrollmentIsSaved() {
        Enrollment enrollment = persistWithParents(anEnrollment());
        entityManager.flush();

        ThrowableAssert.ThrowingCallable throwing = () -> repository.saveAndFlush(
            new Enrollment(
                enrollment.getPatient(),
                enrollment.getMedication()
            )
        );

        assertThatThrownBy(throwing)
            .isInstanceOf(DataIntegrityViolationException.class)
            .cause()
            .isInstanceOfSatisfying(
                ConstraintViolationException.class,
                violation -> assertThat(violation.getConstraintName())
                    .isEqualTo(EnrollmentRepository.OPEN_ENROLLMENT_INDEX)
            );
    }

    @Test
    void pagesEnrollmentsOfOnePatient() {
        Patient patient = entityManager.persist(aPatient());
        persistApproved(patient, TODAY.minusDays(1));
        Enrollment earliest = persistApproved(patient, TODAY.minusDays(3));
        Enrollment middle = persistApproved(patient, TODAY.minusDays(2));
        persistApproved(entityManager.persist(aPatient()), TODAY.minusDays(4));
        flushAndClear();

        Page<Enrollment> page = repository.findByPatientId(
            patient.getId(), PageRequest.of(0, 2, Sort.by("startDate")));

        assertThat(page.getContent())
            .extracting(Enrollment::getId)
            .containsExactly(earliest.getId(), middle.getId());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void loadsMedicationInTheSameQuery() {
        Patient patient = entityManager.persist(aPatient());
        persistApproved(patient, TODAY.minusDays(2));
        persistApproved(patient, TODAY.minusDays(1));
        flushAndClear();

        Page<Enrollment> page = repository.findByPatientId(patient.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent())
            .hasSize(2)
            .allSatisfy(e -> assertThat(Hibernate.isInitialized(e.getMedication())).isTrue());
    }

    private Enrollment persistWithParents(Enrollment enrollment) {
        entityManager.persist(enrollment.getPatient());
        entityManager.persist(enrollment.getMedication());

        return entityManager.persist(enrollment);
    }

    private Enrollment persistApproved(Patient patient, LocalDate startDate) {
        Enrollment enrollment = anEnrollmentBuilder()
            .patient(patient)
            .medication(aMedicationWithUniqueCode())
            .build();
        enrollment.approve(startDate);

        return persistWithParents(enrollment);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
