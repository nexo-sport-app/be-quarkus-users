CREATE TABLE privacy_consent (
    id          UUID DEFAULT gen_random_uuid() NOT NULL,
    tenant_id UUID NOT NULL,
    user_id     UUID NOT NULL,
    code        VARCHAR(15) NOT NULL,
    consent     BOOLEAN NOT NULL,
    created_by  TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by  TEXT NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT privacy_consent_pk PRIMARY KEY (id),
    CONSTRAINT privacy_consent_users_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT privacy_consent_uq UNIQUE (tenant_id, user_id, code)
);
CREATE INDEX idx_privacy_consent_tenant_id_user_id ON privacy_consent(tenant_id, user_id);
