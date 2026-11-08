CREATE TABLE membership_role (
    id             UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL,
    membership_id  UUID NOT NULL,
    role           VARCHAR(20) NOT NULL,
    created_by     TEXT NOT NULL,
    created_at     TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by     TEXT NOT NULL,
    updated_at     TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT membership_role_pk PRIMARY KEY (id),
    CONSTRAINT membership_role_membership_fk FOREIGN KEY (membership_id) REFERENCES membership (id),
    CONSTRAINT membership_role_uq UNIQUE (tenant_id, membership_id, role)
);
CREATE INDEX idx_membership_role_tenant_id_membership_id ON membership_role (tenant_id, membership_id);
