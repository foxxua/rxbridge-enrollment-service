CREATE TABLE patient (
    id               uuid          PRIMARY KEY,
    first_name       varchar(100)  NOT NULL,
    last_name        varchar(100)  NOT NULL,
    email            varchar(254)  NOT NULL,
    address_street   varchar(100)  NOT NULL,
    address_city     varchar(100)  NOT NULL,
    address_country  varchar(2)       NOT NULL,
    address_zipcode  varchar(16)   NOT NULL,
    sex              varchar(20)    NOT NULL,
    birthdate        date          NOT NULL,
    active           boolean       NOT NULL DEFAULT TRUE,
    created_at       timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL,
    version          integer       NOT NULL DEFAULT 0,

    CONSTRAINT chk_patient_address_country  CHECK  ( address_country ~ '^[A-Z]{2}$' ),
    CONSTRAINT chk_patient_sex              CHECK  ( sex IN ('MALE', 'FEMALE', 'UNKNOWN') )
);