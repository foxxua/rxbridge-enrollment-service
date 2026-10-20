package io.rxbridge.enrollment.patient;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends Repository<Patient, UUID> {

    Patient save(Patient patient);

    Optional<Patient> findById(UUID id);

    @Query("""
           select p from Patient p
           where lower(p.personName.firstName) = lower(:#{#personName.firstName})
           and lower(p.personName.lastName) = lower(:#{#personName.lastName})
           and p.birthdate = :birthdate
           order by p.createdAt desc, p.id desc
           """)
    List<Patient> findDuplicateCandidates(
        PersonName personName,
        LocalDate birthdate,
        Limit limit
    );

    Page<Patient> findByEmail(String email, Pageable pageable);

}
