# Feature 04: Registro de Resíduo

## Objetivo

Permitir que voluntários e coordenadores registrem resíduos coletados em campo
durante um mutirão `EM_ANDAMENTO`, com tipo, medidas, quantidade,
geolocalização, foto opcional e suporte a idempotência para sincronização
offline.

## Contexto

Esta é a feature que materializa o propósito central do MVP: substituir o
caderno usado em campo por registros digitais georreferenciados. O
`RegistroResiduo` depende diretamente das features anteriores:

- `Area`, para contextualização geográfica futura e relatórios;
- `Autenticação`, para identificar quem registrou;
- `Mutirão`, porque o registro só existe dentro de um mutirão ativo.

O modelo de domínio já define duas decisões importantes:

- o `id` do registro é gerado no cliente para garantir idempotência offline;
- `areaTotal` é sempre derivada, nunca aceita como entrada.

ADRs relevantes: `0003-monolito-em-vez-de-microservicos.md`,
`0004-postgresql-com-postgis.md`, `0009-flyway-migrations-e-datasource-postgres.md`,
`0010-hibernate-spatial-com-jts-para-geometrias.md`.

## User Stories

- Como **voluntário em campo**, quero registrar um resíduo com tipo, medidas,
  quantidade e localização, para que a coleta fique documentada sem papel.
- Como **voluntário em campo**, quero anexar uma foto opcional, para registrar
  evidência visual do item coletado.
- Como **voluntário em campo**, quero conseguir sincronizar o mesmo registro
  sem duplicação quando a conexão voltar, para que o app seja tolerante a falhas.
- Como **coordenação**, quero consultar os registros de um mutirão, para validar
  a operação e usar os dados em relatórios.
- Como **sistema**, quero impedir novos registros em mutirões não iniciados,
  concluídos ou cancelados, para preservar a consistência do fluxo operacional.

## Critérios de Aceitação

- [ ] **Criação válida** — dado um payload com `id` UUID, `mutiraoId`
  existente e `EM_ANDAMENTO`, `tipoResiduo` válido, `metragemPerpendicular > 0`,
  `metragemTransversal > 0`, `quantidade > 0`, `localizacao` Point SRID 4326 e
  `dataRegistro` válida, quando `POST /api/registros-residuo`, então retorna
  `201 Created` com o recurso persistido e `areaTotal` calculada pelo backend.
- [ ] **Foto opcional** — dado payload válido com `fotoUrl`, quando criar o
  registro, então o backend persiste a URL; quando ausente, persiste `null`.
- [ ] **Idempotência offline** — dado um `id` já persistido, quando o cliente
  reenviar o mesmo `POST /api/registros-residuo`, então o backend não duplica o
  registro e retorna `200 OK` com o recurso já existente.
- [ ] **Listagem por mutirão** — dado um mutirão com N registros, quando
  `GET /api/mutiroes/{id}/registros-residuo`, então retorna `200` com os
  registros em ordem de `dataRegistro ASC`.
- [ ] **Detalhe** — dado um registro existente, quando
  `GET /api/registros-residuo/{id}`, então retorna `200` com todos os campos.
- [ ] **Criação por voluntário** — dado token com role `VOLUNTARIO`, quando
  criar registro em mutirão `EM_ANDAMENTO`, então a requisição é aceita.
- [ ] **Criação por coordenador** — dado token com role `COORDENADOR`, quando
  criar registro em mutirão `EM_ANDAMENTO`, então a requisição é aceita.
- [ ] **Erro: mutirão fora de andamento** — dado `mutiraoId` de mutirão
  `PLANEJADO`, `CONCLUIDO` ou `CANCELADO`, quando criar registro, então retorna
  `409 Conflict`.
- [ ] **Erro: mutirão inexistente** — dado `mutiraoId` não cadastrado, quando
  criar registro, então retorna `404 Not Found`.
- [ ] **Erro: localização ausente ou inválida** — dado payload sem
  `localizacao`, com latitude/longitude inválidas ou SRID diferente de 4326,
  então retorna `400 Bad Request`.
- [ ] **Erro: medidas inválidas** — dado payload com métricas menores ou iguais
  a zero, ou `quantidade <= 0`, então retorna `400 Bad Request`.
- [ ] **Erro: usuário não autenticado** — quando criar ou consultar sem token
  válido, então retorna `401 Unauthorized`.

## Considerações Técnicas

### Estrutura no módulo `com.marsemlixo.api.residuo`

| Camada       | Componentes                                                              |
|--------------|---------------------------------------------------------------------------|
| `controller` | `RegistroResiduoController`                                              |
| `service`    | `RegistroResiduoService` + `RegistroResiduoServiceImpl`                  |
| `repository` | `RegistroResiduoRepository extends JpaRepository<RegistroResiduo, UUID>` |
| `domain`     | `RegistroResiduo`, `TipoResiduo`                                         |

### Endpoints REST

| Método | Path                                 | Role exigida                 | Ação                            |
|--------|--------------------------------------|------------------------------|---------------------------------|
| POST   | `/api/registros-residuo`             | `VOLUNTARIO`, `COORDENADOR`  | Cria registro com idempotência  |
| GET    | `/api/registros-residuo/{id}`        | `VOLUNTARIO`, `COORDENADOR`  | Detalhe do registro             |
| GET    | `/api/mutiroes/{id}/registros-residuo` | `VOLUNTARIO`, `COORDENADOR` | Lista registros de um mutirão   |

Autorização via `@PreAuthorize`, mantendo o padrão da Feature 02.

### DTOs

**`RegistroResiduoCreateRequest`**

```json
{
  "id": "0f6f5b0c-9d1d-4e44-9809-caf0fbe11a21",
  "mutiraoId": 42,
  "tipoResiduo": "PLASTICO",
  "metragemPerpendicular": 2.5,
  "metragemTransversal": 1.2,
  "quantidade": 3,
  "localizacao": {
    "type": "Point",
    "coordinates": [-42.0432, -22.8791]
  },
  "fotoUrl": "https://bucket.s3.amazonaws.com/fotos/registro-42.jpg",
  "dataRegistro": "2025-06-14T10:32:11Z"
}
```

Validações:

- `id`, `mutiraoId`, `tipoResiduo`, `localizacao`, `dataRegistro`: obrigatórios
- `metragemPerpendicular`, `metragemTransversal`: `> 0`
- `quantidade`: inteiro `> 0`
- `fotoUrl`: opcional

**`RegistroResiduoResponse`**

```json
{
  "id": "0f6f5b0c-9d1d-4e44-9809-caf0fbe11a21",
  "mutiraoId": 42,
  "voluntario": {
    "id": 7,
    "nome": "Ana Souza"
  },
  "tipoResiduo": "PLASTICO",
  "metragemPerpendicular": 2.5,
  "metragemTransversal": 1.2,
  "quantidade": 3,
  "areaTotal": 9.0,
  "localizacao": {
    "type": "Point",
    "coordinates": [-42.0432, -22.8791]
  },
  "fotoUrl": "https://bucket.s3.amazonaws.com/fotos/registro-42.jpg",
  "dataRegistro": "2025-06-14T10:32:11Z",
  "syncedAt": "2025-06-14T10:33:02Z"
}
```

### Entidade JPA

- Tabela `registro_residuo`
- PK `id UUID` (fornecido pelo cliente; não gerado pelo backend)
- FK para `mutirao` e `voluntario`
- `tipo_residuo` como `EnumType.STRING`
- `localizacao GEOMETRY(Point, 4326) NOT NULL`
- `synced_at TIMESTAMPTZ NOT NULL`
- `area_total` calculada no backend e persistida como valor derivado

**Migration `V6__create_registro_residuo.sql`:**

```sql
CREATE TABLE registro_residuo (
    id UUID PRIMARY KEY,
    mutirao_id BIGINT NOT NULL REFERENCES mutirao(id),
    voluntario_id BIGINT NOT NULL REFERENCES voluntario(id),
    tipo_residuo VARCHAR(50) NOT NULL,
    metragem_perpendicular NUMERIC(8,2) NOT NULL,
    metragem_transversal NUMERIC(8,2) NOT NULL,
    quantidade INTEGER NOT NULL,
    area_total NUMERIC(10,2) NOT NULL,
    localizacao GEOMETRY(Point, 4326) NOT NULL,
    foto_url TEXT,
    data_registro TIMESTAMPTZ NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_registro_residuo_mutirao ON registro_residuo(mutirao_id);
CREATE INDEX idx_registro_residuo_localizacao ON registro_residuo USING GIST(localizacao);
```

### Regras de domínio

- O `voluntarioId` vem sempre do usuário autenticado; não entra no payload.
- O backend rejeita criação em mutirão fora de `EM_ANDAMENTO`.
- `areaTotal = metragemPerpendicular × metragemTransversal × quantidade`.
- Em caso de reenvio do mesmo `id`, o backend retorna o registro existente sem
  criar duplicata nem alterar `syncedAt`.

## Testes

- Unitários no service cobrindo cálculo de `areaTotal`, validação de mutirão e
  comportamento idempotente.
- Integração com Testcontainers cobrindo persistência PostGIS, autenticação e
  endpoints REST.
- Testes de contrato para `401`, `404`, `409` e payloads inválidos.

## Fora de Escopo

- Edição e deleção de registros já sincronizados.
- Upload binário de foto; nesta feature o backend recebe apenas `fotoUrl`.
- Busca espacial avançada e mapa de calor.
- Sync em lote (`POST` em massa) e resolução de conflitos entre versões locais.
- Exportação consolidada; isso entra na feature de relatórios.

## Dependências

- Feature 02: autenticação e roles já implementadas.
- Feature 03: mutirão com status e regras de transição já implementados.
- Estratégia de upload de foto para S3 via URL pré-assinada, a detalhar em
  feature ou ADR específica.

## Riscos e Mitigações

- **Risco:** diferenças entre relógio do device e do servidor gerarem
  inconsistência na ordenação por `dataRegistro`.
  **Mitigação:** persistir `dataRegistro` enviado pelo cliente e `syncedAt`
  gerado pelo servidor, deixando explícita a semântica de cada timestamp.

- **Risco:** duplicação durante reenvio offline.
  **Mitigação:** usar `UUID` gerado no cliente como PK e tratar novo `POST` com
  o mesmo `id` como operação idempotente.

- **Risco:** payloads geoespaciais inválidos quebrarem persistência.
  **Mitigação:** validar Point e SRID antes do `save()` e cobrir isso com
  integração em banco com PostGIS.

## Decisões Pendentes

- Persistir `area_total` calculada no service ou como coluna gerada no Postgres.
- Permitir edição de registros enquanto o mutirão estiver `EM_ANDAMENTO`.
- Criar endpoint de sync em lote para reduzir round-trips do frontend offline.
