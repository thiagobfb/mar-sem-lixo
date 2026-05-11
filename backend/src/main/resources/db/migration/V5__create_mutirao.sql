CREATE TABLE mutirao (
    id             BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    titulo         VARCHAR(255) NOT NULL,
    data           DATE         NOT NULL,
    hora_inicio    TIME         NOT NULL,
    hora_fim       TIME         NOT NULL,
    area_id        BIGINT       NOT NULL REFERENCES area(id),
    organizador_id BIGINT       NOT NULL REFERENCES voluntario(id),
    status         VARCHAR(50)  NOT NULL DEFAULT 'PLANEJADO',
    observacoes    TEXT
);

CREATE INDEX idx_mutirao_status  ON mutirao(status);
CREATE INDEX idx_mutirao_area_id ON mutirao(area_id);
CREATE INDEX idx_mutirao_data    ON mutirao(data);
