CREATE TABLE patient (
    id               uuid          PRIMARY KEY,
    first_name       varchar(100)  NOT NULL,
    last_name        varchar(100)  NOT NULL,
    email            varchar(254)  NOT NULL,
    address_street   varchar(100)  NOT NULL,
    address_city     varchar(100)  NOT NULL,
    address_country  varchar(2)    NOT NULL,
    address_region   varchar(100),
    address_zipcode  varchar(16)   NOT NULL,
    sex              varchar(20)   NOT NULL,
    birthdate        date          NOT NULL,
    active           boolean       NOT NULL DEFAULT TRUE,
    created_at       timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL,
    version          integer       NOT NULL DEFAULT 0,

    CONSTRAINT chk_patient_first_name_not_blank_trimmed
        CHECK ( first_name ~ '\S' AND first_name !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_last_name_not_blank_trimmed
        CHECK ( last_name ~ '\S' AND last_name !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_email
        CHECK ( email ~ '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$' ),

    CONSTRAINT chk_patient_address_street_not_blank_trimmed
        CHECK ( address_street ~ '\S' AND address_street !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_address_city_not_blank_trimmed
        CHECK ( address_city ~ '\S' AND address_city !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_address_country
        CHECK ( address_country ~ '^[A-Z]{2}$' ),

    CONSTRAINT chk_patient_address_region_not_blank_trimmed
        CHECK ( address_region ~ '\S' AND address_region !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_address_zipcode_not_blank_trimmed
        CHECK ( address_zipcode ~ '\S' AND address_zipcode !~ '^\s|\s$' ),

    CONSTRAINT chk_patient_sex
        CHECK ( sex IN ('MALE', 'FEMALE', 'UNKNOWN') )
);