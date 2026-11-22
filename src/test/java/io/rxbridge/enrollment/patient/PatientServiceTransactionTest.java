package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PatientServiceTransactionTest {

    @Autowired
    PatientService service;

    @Autowired
    PatientRepository patients;

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
    void registersNewPatient() {
        Patient patient = aPatient();

        UUID id = service.register(
            patient.getPersonName(),
            patient.getEmail(),
            patient.getAddress(),
            patient.getSex(),
            patient.getBirthdate()
        );

        assertThat(id).isNotNull();

        Optional<Patient> found = patients.findById(id);
        assertThat(found)
            .hasValueSatisfying(p -> {
                assertThat(p.getId()).isEqualTo(id);
                assertThat(p.getPersonName()).isEqualTo(patient.getPersonName());
                assertThat(p.getEmail()).isEqualTo(patient.getEmail());
                assertThat(p.getAddress()).isEqualTo(patient.getAddress());
                assertThat(p.getSex()).isEqualTo(patient.getSex());
                assertThat(p.getBirthdate()).isEqualTo(patient.getBirthdate());
            });
    }

}
