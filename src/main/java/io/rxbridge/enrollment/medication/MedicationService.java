package io.rxbridge.enrollment.medication;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medications;
    private final ApplicationEventPublisher events;
    private final BusinessCalendar calendar;

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

    private Medication find(UUID medicationId) {
        return medications.findById(medicationId)
            .orElseThrow(() -> new MedicationNotFoundException(medicationId));
    }

}
