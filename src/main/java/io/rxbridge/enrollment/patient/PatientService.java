package io.rxbridge.enrollment.patient;

import io.rxbridge.enrollment.common.calendar.BusinessCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patients;
    private final ApplicationEventPublisher events;
    private final BusinessCalendar calendar;

    @Transactional
    public void activate(UUID patientId) {
        find(patientId).activate();
    }

    @Transactional
    public void deactivate(UUID patientId) {
        Patient patient = find(patientId);
        if (patient.isActive()) {
            patient.deactivate();
            events.publishEvent(new PatientDeactivated(patientId, calendar.today()));
        }
    }

    @Transactional
    public void changeEmail(UUID patientId, String email) {
        find(patientId).changeEmail(email);
    }

    @Transactional
    public void changeAddress(UUID patientId, Address address) {
        find(patientId).changeAddress(address);
    }

    private Patient find(UUID patientId) {
        return patients.findById(patientId)
            .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

}
