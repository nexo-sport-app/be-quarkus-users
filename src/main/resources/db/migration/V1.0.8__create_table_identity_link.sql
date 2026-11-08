CREATE TABLE identity_link (
    id             UUID NOT NULL DEFAULT gen_random_uuid(),
    issuer         TEXT NOT NULL,
    subject        TEXT NOT NULL,
    membership_id  UUID NOT NULL,
    tenant_id      UUID NOT NULL,
    linked_at      TIMESTAMPTZ DEFAULT now() NOT NULL,
    created_by     TEXT NOT NULL,
    created_at     TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by     TEXT NOT NULL,
    updated_at     TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT identity_link_pk PRIMARY KEY (id),
    CONSTRAINT identity_link_membership_fk FOREIGN KEY (membership_id) REFERENCES membership (id),
    CONSTRAINT identity_link_uq UNIQUE (issuer, subject, membership_id)
);
CREATE INDEX idx_identity_link_issuer_subject ON identity_link (issuer, subject);
