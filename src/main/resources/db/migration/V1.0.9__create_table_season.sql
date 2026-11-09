CREATE TABLE season (
    id          UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    label       VARCHAR(9) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    created_by  TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_by  TEXT NOT NULL,
    updated_at  TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT season_pk PRIMARY KEY (id),
    CONSTRAINT season_uq UNIQUE (tenant_id, start_date),
    CONSTRAINT season_c01 CHECK (end_date > start_date)
);
CREATE INDEX idx_season_tenant_id_start_date ON season (tenant_id, start_date);
