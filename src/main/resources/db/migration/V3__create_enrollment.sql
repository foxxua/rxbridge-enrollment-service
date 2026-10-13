CREATE TABLE enrollment (
    id             uuid         PRIMARY KEY,
    patient_id     uuid         NOT NULL,
    medication_id  uuid         NOT NULL,
    start_date     date,
    close_date     date,
    status         varchar(20)  NOT NULL,
    close_reason   varchar(40),
    other_reason   text,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    version        integer      NOT NULL DEFAULT 0,

    CONSTRAINT fk_enrollment_patient
        FOREIGN KEY (patient_id) REFERENCES patient (id),

    CONSTRAINT fk_enrollment_medication
        FOREIGN KEY (medication_id) REFERENCES medication (id),

    CONSTRAINT chk_enrollment_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'COMPLETED',
                'CANCELLED',
                'REJECTED'
            )
        ),

    CONSTRAINT chk_enrollment_close_reason
        CHECK (
            close_reason IN (
                'PATIENT_DEACTIVATED',
                'MEDICATION_DEACTIVATED',
                'NOT_ELIGIBLE',
                'THERAPY_COMPLETED',
                'OTHER'
            )
        ),

    CONSTRAINT chk_enrollment_start_date_null_or_not
        CHECK (
            CASE status
                WHEN 'PENDING'   THEN start_date IS NULL
                WHEN 'APPROVED'  THEN start_date IS NOT NULL
                WHEN 'COMPLETED' THEN start_date IS NOT NULL
                WHEN 'REJECTED'  THEN start_date IS NULL
                ELSE             TRUE
            END
        ),

    CONSTRAINT chk_enrollment_close_date_null_or_not
        CHECK (
            CASE status
                WHEN 'PENDING'   THEN close_date IS NULL
                WHEN 'APPROVED'  THEN close_date IS NULL
                WHEN 'COMPLETED' THEN close_date IS NOT NULL
                WHEN 'CANCELLED' THEN close_date IS NOT NULL
                WHEN 'REJECTED'  THEN close_date IS NOT NULL
                ELSE             FALSE
            END
        ),

    CONSTRAINT chk_enrollment_close_after_start
        CHECK ( close_date >= start_date ),

    CONSTRAINT chk_enrollment_close_reason_status
        CHECK (
            CASE close_reason
                WHEN 'PATIENT_DEACTIVATED'    THEN status = 'CANCELLED'
                WHEN 'MEDICATION_DEACTIVATED' THEN status = 'CANCELLED'
                WHEN 'NOT_ELIGIBLE'           THEN status = 'REJECTED'
                WHEN 'THERAPY_COMPLETED'      THEN status = 'COMPLETED'
                WHEN 'OTHER'                  THEN status IN ('CANCELLED', 'REJECTED')
                ELSE                               status IN ('PENDING', 'APPROVED')
            END
        ),

    CONSTRAINT chk_enrollment_close_reason_null_or_not
        CHECK (
            CASE status
                WHEN 'PENDING'   THEN close_reason IS NULL
                WHEN 'APPROVED'  THEN close_reason IS NULL
                WHEN 'COMPLETED' THEN close_reason IS NOT NULL
                WHEN 'CANCELLED' THEN close_reason IS NOT NULL
                WHEN 'REJECTED'  THEN close_reason IS NOT NULL
                ELSE             FALSE
            END
        ),

    CONSTRAINT chk_enrollment_other_reason_null_or_not
        CHECK (
            CASE close_reason
                WHEN 'OTHER' THEN other_reason IS NOT NULL
                ELSE              other_reason IS NULL
            END
        )
);

CREATE        INDEX idx_enrollment_patient_id                ON enrollment (patient_id);
CREATE        INDEX idx_enrollment_medication_id             ON enrollment (medication_id);
CREATE UNIQUE INDEX uq_enrollment_patient_medication_active  ON enrollment (patient_id, medication_id) WHERE status IN ('PENDING', 'APPROVED');