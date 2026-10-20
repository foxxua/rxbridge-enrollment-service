package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientFixtures.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class PatientRepositoryTest {

    static final LocalDate BIRTHDATE = LocalDate.of(1992, 3, 3);
    static final Instant BASE_TIME = Instant.parse("2026-01-01T10:00:00Z");
    static final Sort NEWEST_FIRST = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );
    static final String SHARED_EMAIL = "patients@example.com";

    @Autowired
    PatientRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void findsDuplicatesIgnoringCase() {
        Patient exact = persist("William", "Shakespeare", BIRTHDATE);
        Patient shouted = persist("WILLIAM", "SHAKESPEARE", BIRTHDATE);
        persist("William", "Shakespeare", BIRTHDATE.plusDays(1));
        persist("William", "de Saint-Exupéry", BIRTHDATE);
        persist("Antoine", "Shakespeare", BIRTHDATE);
        flushAndClear();

        setCreatedAt(exact.getId(), BASE_TIME.minus(2, ChronoUnit.HOURS));
        setCreatedAt(shouted.getId(), BASE_TIME.minus(1, ChronoUnit.HOURS));

        List<Patient> found = repository.findDuplicateCandidates(
            new PersonName("WiLLiAm", "sHaKeSpEaRe"),
            BIRTHDATE,
            Limit.of(10)
        );

        assertThat(found)
            .extracting(Patient::getId)
            .containsExactly(shouted.getId(), exact.getId());
    }

    @Test
    void doesNotSwapFirstAndLastName() {
        persist("Shakespeare", "William", BIRTHDATE);
        flushAndClear();

        List<Patient> found = repository.findDuplicateCandidates(
            new PersonName("William", "Shakespeare"),
            BIRTHDATE,
            Limit.of(10)
        );

        assertThat(found).isEmpty();
    }

    @Test
    void respectsLimit() {
        persist("William", "Shakespeare", BIRTHDATE);
        persist("William", "Shakespeare", BIRTHDATE);
        persist("William", "Shakespeare", BIRTHDATE);
        flushAndClear();

        List<Patient> found = repository.findDuplicateCandidates(
            new PersonName("William", "Shakespeare"),
            BIRTHDATE,
            Limit.of(2)
        );

        assertThat(found).hasSize(2);
    }

    @Test
    void pagesPatientsSharingEmailNewestFirst() {
        Patient newest = persist("Antoine", "de Saint-Exupéry", SHARED_EMAIL);
        Patient oldest = persist("William", "Shakespeare", SHARED_EMAIL);
        Patient middle = persist("Agatha", "Christie", SHARED_EMAIL);
        persist("William", "de Saint-Exupéry", "other@example.com");
        flushAndClear();

        setCreatedAt(oldest.getId(), BASE_TIME.minus(3, ChronoUnit.HOURS));
        setCreatedAt(middle.getId(), BASE_TIME.minus(2, ChronoUnit.HOURS));
        setCreatedAt(newest.getId(), BASE_TIME.minus(1, ChronoUnit.HOURS));

        Page<Patient> first = repository.findByEmail(
            SHARED_EMAIL,
            PageRequest.of(0, 2, NEWEST_FIRST)
        );

        Page<Patient> second = repository.findByEmail(
            SHARED_EMAIL,
            PageRequest.of(1, 2, NEWEST_FIRST)
        );

        assertThat(first.getContent())
            .extracting(Patient::getId)
            .containsExactly(newest.getId(), middle.getId());

        assertThat(second.getContent())
            .extracting(Patient::getId)
            .containsExactly(oldest.getId());

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    private void setCreatedAt(UUID id, Instant createdAt) {
        int rowsUpdated = jdbc.update(
            "UPDATE patient SET created_at = ? WHERE id = ?",
            createdAt.atOffset(ZoneOffset.UTC), id
        );
        assertThat(rowsUpdated).isEqualTo(1);
    }

    private Patient persist(String firstName, String lastName, LocalDate birthdate) {
        return entityManager.persist(aPatientBuilder()
            .personName(new PersonName(firstName, lastName))
            .birthdate(birthdate)
            .build());
    }

    private Patient persist(String firstName, String lastName, String email) {
        return entityManager.persist(aPatientBuilder()
            .personName(new PersonName(firstName, lastName))
            .email(email)
            .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
