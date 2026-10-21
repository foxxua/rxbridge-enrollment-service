package io.rxbridge.enrollment.medication;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends Repository<Medication, UUID> {

    Medication save(Medication medication);

    Optional<Medication> findById(UUID id);

    Optional<Medication> findByCode(String code);

}
