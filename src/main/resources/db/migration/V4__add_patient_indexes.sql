CREATE INDEX idx_patient_name_lower_birthdate
    ON patient (lower(first_name), lower(last_name), birthdate);

CREATE INDEX idx_patient_email
    ON patient (email);