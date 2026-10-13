CREATE TABLE medication (
    id            uuid          PRIMARY KEY,
    name          varchar(100)  NOT NULL,
    description   text,
    code          varchar(7)    NOT NULL,
    side_effects  text,
    active        boolean       NOT NULL DEFAULT TRUE,
    created_at    timestamptz   NOT NULL,
    updated_at    timestamptz   NOT NULL,
    version       integer       NOT NULL DEFAULT 0,

    CONSTRAINT chk_medication_code  CHECK  ( code ~ '^M\d{6}$' ),
    CONSTRAINT uq_medication_code UNIQUE (code)
);