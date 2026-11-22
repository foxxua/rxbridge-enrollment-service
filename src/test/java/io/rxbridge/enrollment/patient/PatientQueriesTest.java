package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PatientQueriesTest {

//    private static final UUID PATIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    PatientQueries queries;

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
    void patientFoundById() {
        Patient patient = aPatient();
        UUID id = persisted(patient);

        PatientView patientView = queries.patient(id);

        assertThat(patientView).isEqualTo(PatientView.from(patient));
    }

    @Test
    void unknownPatientIdIsNotFound() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000404");

        assertThatThrownBy(() -> queries.patient(unknown))
            .isInstanceOfSatisfying(
                PatientNotFoundException.class,
                e -> assertThat(e.patientId()).isEqualTo(unknown)
            );
    }

    @Test
    void returnsDuplicatesWithLimit() {
        Patient patient = aPatient();
        persistedList(getPatientsList(10));

        List<PatientView> foundDuplicatesViews = queries.duplicatesOf(
            patient.getPersonName(),
            patient.getBirthdate(),
            Limit.of(3)
        );

        assertThatList(foundDuplicatesViews).hasSize(3);
        foundDuplicatesViews.forEach(
            patientView -> assertSoftly(softly -> {
                softly.assertThat(patientView.personName()).isEqualTo(PersonNameView.from(patient.getPersonName()));
                softly.assertThat(patientView.email()).isEqualTo(patient.getEmail());
                softly.assertThat(patientView.address()).isEqualTo(AddressView.from(patient.getAddress()));
                softly.assertThat(patientView.sex()).isEqualTo(patient.getSex());
                softly.assertThat(patientView.birthdate()).isEqualTo(patient.getBirthdate());
            })
        );
    }

    @Test
    void duplicatesNotFoundWithDifferentBirthdate() {
        Patient patient = aPatientBuilder().birthdate(LocalDate.of(1991, 10, 12)).build();
        List<Patient> patients = List.of(
            aPatientBuilder().birthdate(LocalDate.of(1980, 8, 4)).build(),
            aPatientBuilder().birthdate(LocalDate.of(2001, 7, 1)).build(),
            aPatientBuilder().birthdate(LocalDate.of(1999, 2, 5)).build()
        );
        persistedList(patients);

        List<PatientView> foundDuplicatesViews = queries.duplicatesOf(
            patient.getPersonName(),
            patient.getBirthdate(),
            Limit.of(3)
        );

        assertThatList(foundDuplicatesViews).isEmpty();
    }

    @Test
    void duplicatesNotFoundWithDifferentPersonName() {
        Patient patient = aPatientBuilder()
            .personName(anotherPatientPersonName())
            .build();
        List<Patient> patients = List.of(
            aPatient(),
            aPatient(),
            aPatient()
        );
        persistedList(patients);

        List<PatientView> foundDuplicatesViews = queries.duplicatesOf(
            patient.getPersonName(),
            patient.getBirthdate(),
            Limit.of(3)
        );

        assertThatList(foundDuplicatesViews).isEmpty();
    }

    @Test
    void returnsPatientPageByEmail() {
        Patient patient = aPatient();
        persistedList(getPatientsList(7));

        Page<PatientView> page = queries.patientsOf(
            patient.getEmail(),
            PageRequest.of(0, 10)
        );

        List<PatientView> result = page.getContent();
        assertThatList(result).hasSize(7);
        result.forEach(
            patientView -> assertSoftly(softly -> {
                softly.assertThat(patientView.personName()).isEqualTo(PersonNameView.from(patient.getPersonName()));
                softly.assertThat(patientView.email()).isEqualTo(patient.getEmail());
                softly.assertThat(patientView.address()).isEqualTo(AddressView.from(patient.getAddress()));
                softly.assertThat(patientView.sex()).isEqualTo(patient.getSex());
                softly.assertThat(patientView.birthdate()).isEqualTo(patient.getBirthdate());
            })
        );
    }

    @Test
    void noResultsOfPatientPageWithDifferentEmail() {
        Patient patient = aPatientBuilder().email(anotherPatientEmail()).build();
        persistedList(getPatientsList(7));

        Page<PatientView> page = queries.patientsOf(
            patient.getEmail(),
            PageRequest.of(0, 10)
        );

        List<PatientView> result = page.getContent();
        assertThatList(result).isEmpty();
    }

    private UUID persisted(Patient patient) {
        return tx.execute(status -> {
            entityManager.persist(patient);
            return patient.getId();
        });
    }

    private List<Patient> getPatientsList(int count) {
        return IntStream.range(0, count)
            .mapToObj(item -> aPatient()).toList();
    }

    private void persistedList(List<Patient> patients) {
        tx.executeWithoutResult(status -> patients.forEach(entityManager::persist));
    }

}
