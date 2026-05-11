CREATE TABLE refresh_token (
    id            BIGINT      PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    voluntario_id BIGINT      NOT NULL REFERENCES voluntario(id),
    token_hash    VARCHAR(64) NOT NULL,
    expira_em     TIMESTAMPTZ NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_token_voluntario ON refresh_token(voluntario_id);
