CONSTRAINT chk_medication_description_length
        CHECK ( char_length(description) <= 2000 );

CONSTRAINT chk_medication_side_effects_length
        CHECK ( char_length(side_effects) <= 10000 );