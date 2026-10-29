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

    private static final Closure THERAPY_COMPLETED_CLOSURE = new Closure(CloseReason.THERAPY_COMPLETED, null);

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

        if (this.status == EnrollmentStatus.APPROVED && startDate.equals(this.startDate)) {
            return;
        }

        requireCanMoveTo(EnrollmentStatus.APPROVED, "approve");

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

        if (isRepeatedClosing(EnrollmentStatus.COMPLETED, THERAPY_COMPLETED_CLOSURE, closeDate)) {
            return;
        }

        requireCanMoveTo(EnrollmentStatus.COMPLETED, "complete");
        close(EnrollmentStatus.COMPLETED, THERAPY_COMPLETED_CLOSURE, closeDate, today);
    }

    public void reject(Closure closure, LocalDate closeDate, LocalDate today) {
        Objects.requireNonNull(closure, "closure");
        Objects.requireNonNull(closeDate, "closeDate");

        if (isRepeatedClosing(EnrollmentStatus.REJECTED, closure, closeDate)) {
            return;
        }

        requireCanMoveTo(EnrollmentStatus.REJECTED, "reject");
        close(EnrollmentStatus.REJECTED, closure, closeDate, today);
    }

    public void cancel(Closure closure, LocalDate closeDate, LocalDate today) {
        Objects.requireNonNull(closure, "closure");
        Objects.requireNonNull(closeDate, "closeDate");

        if (isRepeatedClosing(EnrollmentStatus.CANCELLED, closure, closeDate)) {
            return;
        }

        requireCanMoveTo(EnrollmentStatus.CANCELLED, "cancel");
        requireReasonIsTrue(closure);
        close(EnrollmentStatus.CANCELLED, closure, closeDate, today);
    }

    private void requireCanMoveTo(EnrollmentStatus target, String action) {
        if (!this.status.canMoveTo(target)) {
            throw new IllegalStateException(
                CANNOT_MOVE_TEMPLATE.formatted(action, this.status)
            );
        }
    }

    private boolean isRepeatedClosing(EnrollmentStatus target, Closure closure, LocalDate closeDate) {
        return this.status == target
            && closure.equals(currentClosure())
            && closeDate.equals(this.closeDate);
    }

    private void close(EnrollmentStatus target, Closure closure, LocalDate closeDate, LocalDate today) {
        if (!closure.closeReason().isAllowedFor(target)) {
            throw new IllegalArgumentException(
                CLOSE_REASON_NOT_ALLOWED_TEMPLATE.formatted(closure.closeReason(), target)
            );
        }

        requireNotFuture(closeDate, today, "closeDate");
        if (this.startDate != null) {
            requireNotBefore(closeDate, this.startDate, "closeDate");
        }

        this.status = target;
        this.closeDate = closeDate;
        this.closeReason = closure.closeReason();
        this.otherReason = closure.otherReason();
    }

    private void requireReasonIsTrue(Closure closure) {
        if (closure.closeReason() == CloseReason.PATIENT_DEACTIVATED && this.patient.isActive()) {
            throw new IllegalArgumentException(
                CANNOT_CANCEL_ACTIVE_TEMPLATE.formatted(
                    closure.closeReason(),
                    "patient",
                    this.patient.getId()
                )
            );
        }

        if (closure.closeReason() == CloseReason.MEDICATION_DEACTIVATED && this.medication.isActive()) {
            throw new IllegalArgumentException(
                CANNOT_CANCEL_ACTIVE_TEMPLATE.formatted(
                    closure.closeReason(),
                    "medication",
                    this.medication.getId()
                )
            );
        }
    }

    private Closure currentClosure() {
        return new Closure(this.closeReason, this.otherReason);
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
