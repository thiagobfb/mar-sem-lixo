CREATE TABLE area (
    id        BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    nome      VARCHAR(255) NOT NULL,
    tipo      VARCHAR(50)  NOT NULL,
    municipio VARCHAR(255) NOT NULL,
    estado    VARCHAR(2)   NOT NULL,
    poligono  GEOMETRY(Polygon, 4326) NOT NULL,
    ativa     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_area_nome_municipio UNIQUE (nome, municipio)
);

CREATE INDEX idx_area_poligono ON area USING GIST (poligono);
