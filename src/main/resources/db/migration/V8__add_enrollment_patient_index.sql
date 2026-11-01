CREATE INDEX idx_enrollment_patient_created
    ON enrollment (patient_id, created_at DESC, id DESC);