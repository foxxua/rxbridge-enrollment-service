package io.rxbridge.enrollment.patient;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientQueries {

    private final PatientRepository patients;

    public PatientView patient(UUID patientId) {
        return patients.findById(patientId)
            .map(PatientView::from)
            .orElseThrow(() -> new PatientNotFoundException(patientId));
    }

    public List<PatientView> duplicatesOf(
        PersonName personName,
        LocalDate birthdate,
        Limit limit
    ) {
        return patients.findDuplicateCandidates(
            personName,
            birthdate,
            limit
        ).stream().map(PatientView::from).toList();
    }

    public Page<PatientView> patientsOf(String email, Pageable pageable) {
        return patients.findByEmail(Patient.normalizeEmail(email), pageable)
            .map(PatientView::from);
    }

}
