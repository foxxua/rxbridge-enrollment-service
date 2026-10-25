package io.rxbridge.enrollment.enrollment;

import io.rxbridge.enrollment.common.persistence.BaseEntity;
import io.rxbridge.enrollment.medication.Medication;
import io.rxbridge.enrollment.patient.Patient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

import static io.rxbridge.enrollment.enrollment.EnrollmentConstants.*;
import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;

@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", updatable = false, nullable = false)
    private Patient patient;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", updatable = false, nullable = false)
    private Medication medication;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_MAX_LENGTH)
    private EnrollmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = CLOSE_REASON_MAX_LENGTH)
    private CloseReason closeReason;

    @Size(max = OTHER_REASON_MAX_LENGTH)
    @Column(name = "other_reason", length = OTHER_REASON_MAX_LENGTH)
    private String otherReason;

    public Enrollment(
        Patient patient,
        Medication medication
    ) {
        this.patient = validatePatient(patient);
        this.medication = validateMedication(medication);
        this.status = EnrollmentStatus.PENDING;
    }

    public void approve(LocalDate startDate) {
        Objects.requireNonNull(startDate, "startDate");

        if (status == EnrollmentStatus.APPROVED && startDate.equals(this.startDate)) {
            return;
        }

        if (status != EnrollmentStatus.PENDING) {
            throw new IllegalStateException(
                CANNOT_APPROVE_TEMPLATE.formatted(status)
            );
        }

        if (!this.patient.isActive()) {
            throw new IllegalStateException(
                PATIENT_NOT_ACTIVE_TEMPLATE.formatted(patient.getId())
            );
        }

        if (!this.medication.isActive()) {
            throw new IllegalStateException(
                MEDICATION_NOT_ACTIVE_TEMPLATE.formatted(medication.getId())
            );
        }

        this.status = EnrollmentStatus.APPROVED;
        this.startDate = startDate;
    }

    public void complete(LocalDate closeDate, LocalDate today) {
        Objects.requireNonNull(closeDate, "closeDate");

        if (status == EnrollmentStatus.COMPLETED && closeDate.equals(this.closeDate)) {
            return;
        }

        if (status != EnrollmentStatus.APPROVED) {
            throw new IllegalStateException(
                CANNOT_COMPLETE_TEMPLATE.formatted(status)
            );
        }

        requireNotFuture(closeDate, today, "closeDate");
        requireNotBefore(closeDate, this.startDate, "closeDate");

        this.status = EnrollmentStatus.COMPLETED;
        this.closeDate = closeDate;
        this.closeReason = CloseReason.THERAPY_COMPLETED;
    }

    private static Patient validatePatient(Patient patient) {
        Objects.requireNonNull(patient, "patient");

        if (!patient.isActive()) {
            throw new IllegalArgumentException(
                PATIENT_NOT_ACTIVE_TEMPLATE.formatted(patient.getId())
            );
        }

        return patient;
    }

    private static Medication validateMedication(Medication medication) {
        Objects.requireNonNull(medication, "medication");

        if (!medication.isActive()) {
            throw new IllegalArgumentException(
                MEDICATION_NOT_ACTIVE_TEMPLATE.formatted(medication.getId())
            );
        }

        return medication;
    }

}
