CREATE TABLE contacts (
    id          UUID DEFAULT uuid_generate_v4() NOT NULL,
    user_id     UUID NOT NULL,
    type        VARCHAR(15) NOT NULL,
    priority    VARCHAR(15) NOT NULL,
    description VARCHAR(60) NOT NULL,
    value       VARCHAR(60) NOT NULL,
    created_by  TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by  TEXT NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT contacts_pk PRIMARY KEY (id),
    CONSTRAINT contacts_users_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_contacts_user_id ON contacts(user_id);