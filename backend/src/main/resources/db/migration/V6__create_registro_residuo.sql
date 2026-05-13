CREATE TABLE registro_residuo (
    id                     UUID PRIMARY KEY,
    mutirao_id             BIGINT        NOT NULL REFERENCES mutirao(id),
    voluntario_id          BIGINT        NOT NULL REFERENCES voluntario(id),
    tipo_residuo           VARCHAR(50)   NOT NULL,
    metragem_perpendicular NUMERIC(8,2)  NOT NULL CHECK (metragem_perpendicular > 0),
    metragem_transversal   NUMERIC(8,2)  NOT NULL CHECK (metragem_transversal > 0),
    quantidade             INTEGER       NOT NULL CHECK (quantidade > 0),
    area_total             NUMERIC(10,2) NOT NULL CHECK (area_total > 0),
    localizacao            GEOMETRY(Point, 4326) NOT NULL,
    foto_url               TEXT,
    data_registro          TIMESTAMPTZ   NOT NULL,
    synced_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_registro_residuo_mutirao ON registro_residuo(mutirao_id);
CREATE INDEX idx_registro_residuo_data_registro ON registro_residuo(data_registro);
CREATE INDEX idx_registro_residuo_localizacao ON registro_residuo USING GIST(localizacao);
