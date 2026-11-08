package io.rxbridge.enrollment.medication;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MedicationQueries {

    private final MedicationRepository medications;

    MedicationView medication(UUID medicationId) {
        return medications.findById(medicationId)
            .map(MedicationView::from)
            .orElseThrow(() -> new MedicationNotFoundException(medicationId));
    }

    MedicationView medication(String code) {
        return medications.findByCode(Medication.normalizeCode(code))
            .map(MedicationView::from)
            .orElseThrow(() -> new MedicationNotFoundByCodeException(code));
    }

}
