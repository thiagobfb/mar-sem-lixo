CREATE TABLE voluntario (
    id            BIGINT       PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    google_id     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'VOLUNTARIO',
    data_cadastro TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_voluntario_google_id UNIQUE (google_id),
    CONSTRAINT uq_voluntario_email     UNIQUE (email)
);
