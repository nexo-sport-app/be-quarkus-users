CREATE TABLE membership (
    id          UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    user_id     UUID,
    status      VARCHAR(20) NOT NULL,
    created_by  TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by  TEXT NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT membership_pk PRIMARY KEY (id),
    CONSTRAINT membership_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_membership_tenant_id ON membership (tenant_id);
