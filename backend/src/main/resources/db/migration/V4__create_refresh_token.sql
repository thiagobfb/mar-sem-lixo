CREATE TABLE refresh_token (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    voluntario_id UUID        NOT NULL REFERENCES voluntario(id),
    token_hash    VARCHAR(64) NOT NULL,
    expira_em     TIMESTAMPTZ NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_token_voluntario ON refresh_token(voluntario_id);
