package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static io.rxbridge.enrollment.common.persistence.DatabaseConstraints.violates;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medications;
    private final ApplicationEventPublisher events;
    private final BusinessCalendar calendar;

    @Transactional
    public UUID register(
        String name,
        String description,
        String code,
        String sideEffects
    ) {
        String normalizedCode = Medication.normalizeCode(code);
        if (medications.existsByCode(normalizedCode)) {
            throw new DuplicateMedicationException(normalizedCode);
        }

        try {
            return medications.saveAndFlush(new Medication(
                name,
                description,
                code,
                sideEffects
            )).getId();
        } catch (DataIntegrityViolationException e) {
            if (violates(e, MedicationRepository.CODE_UNIQUE)) {
                throw new DuplicateMedicationException(normalizedCode, e);
            }
            throw e;
        }
    }

    @Transactional
    public void rename(UUID medicationId, String name) {
        find(medicationId).rename(name);
    }

    @Transactional
    public void activate(UUID medicationId) {
        find(medicationId).activate();
    }

    @Transactional
    public void deactivate(UUID medicationId) {
        Medication medication = find(medicationId);
        if (medication.isActive()) {
            medication.deactivate();
            events.publishEvent(new MedicationDeactivated(medicationId, calendar.today()));
        }
    }

    @Transactional
    public void changeDescription(UUID medicationId, String description) {
        find(medicationId).changeDescription(description);
    }

    @Transactional
    public void changeSideEffects(UUID medicationId, String sideEffects) {
        find(medicationId).changeSideEffects(sideEffects);
    }

    private Medication find(UUID medicationId) {
        return medications.findById(medicationId)
            .orElseThrow(() -> new MedicationNotFoundException(medicationId));
    }

}
