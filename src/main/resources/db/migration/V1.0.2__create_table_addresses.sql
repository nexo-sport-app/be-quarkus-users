CREATE TABLE addresses (
    id          UUID DEFAULT uuid_generate_v4() NOT NULL,
    user_id     UUID NOT NULL,
    street      TEXT NOT NULL,
    city        VARCHAR(40) NOT NULL,
    zip_code    VARCHAR(10) NOT NULL,
    province    VARCHAR(10) NOT NULL,
    state       VARCHAR(10) NOT NULL,
    created_by  TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by  TEXT NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT addresses_pk PRIMARY KEY (id),
    CONSTRAINT addresses_users_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_addresses_user_id ON addresses(user_id);