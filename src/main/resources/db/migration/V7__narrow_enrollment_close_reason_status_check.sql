ALTER TABLE enrollment DROP CONSTRAINT chk_enrollment_close_reason_status;
ALTER TABLE enrollment ADD CONSTRAINT chk_enrollment_close_reason_status
    CHECK (
        CASE close_reason
            WHEN 'PATIENT_DEACTIVATED'    THEN status = 'CANCELLED'
            WHEN 'MEDICATION_DEACTIVATED' THEN status = 'CANCELLED'
            WHEN 'NOT_ELIGIBLE'           THEN status = 'REJECTED'
            WHEN 'THERAPY_COMPLETED'      THEN status = 'COMPLETED'
            WHEN 'OTHER'                  THEN status IN ('CANCELLED', 'REJECTED')
            ELSE                          TRUE
        END
    );