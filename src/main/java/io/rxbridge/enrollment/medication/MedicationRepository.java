package io.rxbridge.enrollment.medication;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends Repository<Medication, UUID> {

    String CODE_UNIQUE = "uq_medication_code";

    Medication save(Medication medication);

    Medication saveAndFlush(Medication medication);

    Optional<Medication> findById(UUID id);

    Optional<Medication> findByCode(String code);

    boolean existsByCode(String code);

}
