CREATE TABLE users (
    id UUID DEFAULT uuid_generate_v4() NOT NULL,
    first_name      VARCHAR(40) NOT NULL,
    middle_name     VARCHAR(40),
    last_name       VARCHAR(40) NOT NULL,
    gender          VARCHAR(2) NOT NULL,
    tax_code        VARCHAR(20) NOT NULL,
    nationality     VARCHAR(10) NOT NULL,
    date_of_birth   DATE NOT NULL,
    place_of_birth  VARCHAR(10) NOT NULL,
    created_by      TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by      TEXT NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (id),
    CONSTRAINT users_uq UNIQUE (tax_code)
);
CREATE INDEX idx_users_tax_code ON users(tax_code);
CREATE INDEX idx_users_last_name ON users(last_name);