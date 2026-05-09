# Feature 01: Cadastro de Área

## Objetivo

Permitir à coordenação cadastrar, editar, listar e inativar áreas geográficas (praias, lagoas, mangues, rios) onde mutirões acontecerão, com polígono delimitador para uso futuro em geolocalização e relatórios.

## Contexto

Áreas são pré-requisito do domínio: o `Mutirao` (`docs/domain/model.md`) referencia `areaId` e exige que a área esteja `ativa` no momento da criação. Sem áreas cadastradas, a operação não começa.

Esta é a primeira feature implementada — antes mesmo de autenticação — para validar end-to-end o pipeline de persistência (Postgres + PostGIS + JPA + Flyway) em escopo pequeno e sem dependência externa (Google OAuth). A autenticação real entra em feature subsequente; em dev/test esta feature opera com flag de auth desabilitada.

ADRs relevantes: `0004-postgresql-com-postgis.md` (SRID 4326), `0003-monolito-em-vez-de-microservicos.md` (módulo `area` com camadas próprias).

## User Stories

- Como **coordenador**, quero cadastrar uma nova área com nome, tipo, município, estado e polígono geográfico, para que mutirões possam ser organizados nela.
- Como **coordenador**, quero editar dados de uma área existente (nome, tipo, município, estado, polígono), para refinar a delimitação após inspeção em campo ou corrigir erros de cadastro.
- Como **coordenador**, quero inativar uma área que não será mais usada, sem apagá-la, para preservar o histórico de mutirões já realizados nela.
- Como **coordenador**, quero listar todas as áreas com filtro por município, tipo e status, para escolher onde cadastrar o próximo mutirão.
- Como **voluntário em campo**, quero ver as áreas ativas no app, para entender em qual área um mutirão está acontecendo.

## Critérios de Aceitação

- [ ] **Criação válida** — dado um payload com `nome` único no município, `tipo` válido, polígono GeoJSON topologicamente válido em SRID 4326, `municipio` não-vazio e `estado` (UF de 2 chars), quando `POST /api/areas`, então retorna `201 Created` com o recurso e `ativa=true`.
- [ ] **Detalhe** — dado uma área existente, quando `GET /api/areas/{id}`, então retorna `200` com o recurso completo, polígono em GeoJSON.
- [ ] **Listagem com filtros** — dado N áreas cadastradas, quando `GET /api/areas?ativa=true&tipo=PRAIA&municipio=Cabo Frio`, então retorna a lista filtrada (ordem alfabética por nome).
- [ ] **Edição parcial** — dado uma área existente, quando `PATCH /api/areas/{id}` com um subconjunto de campos, então retorna `200` com o recurso atualizado e os campos não enviados preservados.
- [ ] **Inativação** — dado uma área existente, quando `DELETE /api/areas/{id}`, então o recurso passa a `ativa=false` (soft-delete) e retorna `204 No Content`. Não há hard-delete.
- [ ] **Inativação com histórico** — dado uma área com mutirões `CONCLUIDO` associados, quando `DELETE /api/areas/{id}`, então a inativação é permitida (`204 No Content`); a leitura dos mutirões históricos referenciando a área inativa continua funcionando.
- [ ] **Erro: nome duplicado por município** — dado uma área já cadastrada com (`nome` X, `municipio` Y), quando criar outra com o mesmo par, então retorna `409 Conflict` com mensagem clara.
- [ ] **Erro: polígono inválido** — dado polígono com self-intersection, anel não-fechado ou SRID ≠ 4326, quando `POST` ou `PATCH`, então retorna `400 Bad Request` com mensagem indicando o problema topológico.
- [ ] **Erro: campos obrigatórios ausentes ou inválidos** — dado payload com `nome` vazio, `tipo` desconhecido, `estado` com tamanho ≠ 2, ou `poligono` ausente, então retorna `400` com lista dos campos inválidos.
- [ ] **Erro: ID inexistente** — dado um ID UUID não cadastrado, quando `GET`, `PATCH` ou `DELETE` em `/api/areas/{id}`, então retorna `404 Not Found`.

## Considerações Técnicas

**Estrutura no módulo `com.marsemlixo.api.area`** (camadas conforme ADR 0003):

| Camada | Componentes |
|--------|-------------|
| `controller` | `AreaController` (REST) |
| `service`    | `AreaService` (interface) + `AreaServiceImpl` |
| `repository` | `AreaRepository extends JpaRepository<Area, UUID>` |
| `domain`     | `Area` (entidade JPA), `AreaTipo` (enum) |

**Endpoints REST:**

| Método | Path                | Ação                                                            |
|--------|---------------------|-----------------------------------------------------------------|
| POST   | `/api/areas`        | Cria área                                                       |
| GET    | `/api/areas`        | Lista, com filtros: `ativa`, `tipo`, `municipio`, `estado`      |
| GET    | `/api/areas/{id}`   | Detalhe                                                         |
| PATCH  | `/api/areas/{id}`   | Edição parcial                                                  |
| DELETE | `/api/areas/{id}`   | Soft-delete (`ativa=false`)                                     |

**DTOs (records, conforme convenção Java moderna do CLAUDE.md raiz):**

- `AreaCreateRequest(String nome, AreaTipo tipo, String municipio, String estado, GeoJsonPolygon poligono)` com Bean Validation: `@NotBlank` (nome, municipio), `@NotNull` (tipo, poligono), `@Size(min=2,max=2)` (estado).
- `AreaUpdateRequest` com mesmos campos, todos opcionais (regra: pelo menos um campo presente).
- `AreaResponse(UUID id, String nome, AreaTipo tipo, String municipio, String estado, GeoJsonPolygon poligono, boolean ativa)`.
- `GeoJsonPolygon` é um wrapper sobre o objeto `Polygon` GeoJSON padrão (RFC 7946); serialização via Jackson.

**Entidade JPA:**

- Tabela `area`, coluna `poligono GEOMETRY(Polygon, 4326) NOT NULL`.
- Constraint única em `(nome, municipio)`.
- `tipo` mapeado como `@Enumerated(EnumType.STRING)`.
- Mapping de geometria via Hibernate Spatial (a confirmar em ADR de mapeamento geométrico).

**Migrations Flyway:**

- `V1__enable_postgis.sql` — `CREATE EXTENSION IF NOT EXISTS postgis;`
- `V2__create_area.sql` — tabela `area`, constraint única, índice GiST em `poligono`.

**Validação topológica do polígono** (decidido):

- Validação delegada ao Postgres via `ST_IsValid(poligono)` no service antes do persist (single source of truth do PostGIS, alinhado com o que o banco aceita).
- SRID validado ao desserializar o GeoJSON e antes do persist.
- Round-trip extra ao banco aceito como custo do single source of truth.
- Falhas mapeadas para `400 Bad Request` com `problem+json` (RFC 7807).

**Constraint única** (decidido): `(nome, municipio)`. Permite mesmo nome em municípios distintos. Caso raro de municípios homônimos em UFs diferentes não é tratado no MVP — vira backlog se acontecer.

**Listagem** (decidido): `GET /api/areas` retorna `poligono=null` por default; cliente pede `?incluirPoligono=true` quando precisa renderizar mapa. `GET /api/areas/{id}` sempre retorna polígono completo.

**Inativação com histórico** (decidido): permitir mesmo com mutirões `CONCLUIDO` associados. Mutirões concluídos são imutáveis e a referência por `areaId` continua funcionando após `ativa=false`. Criação de novos mutirões já valida `area.ativa=true`, então não há risco de uso indevido futuro.

**Autorização** (decidido — provisória até feature de auth chegar):

- Em dev/test: endpoints abertos atrás de flag `app.auth.enabled=false` (default em `application.yml` desses perfis).
- Em prod (`spring.profiles.active=prod`): aplicação **não sobe** com `app.auth.enabled=false` — fail-fast no startup, bloqueia exposição acidental. Implementação: `@PostConstruct` numa `AuthGuardConfig` que checa o perfil e a flag e dispara `IllegalStateException` se incompatíveis.
- Quando a feature de Autenticação Google + JWT entrar (spec dedicada), regra fica: `COORDENADOR` → CRUD completo; `VOLUNTARIO` → apenas `GET` filtrando `ativa=true`. A flag e o fail-fast são removidos quando a feature de auth substituir.

**Documentação OpenAPI:**

- Todos os endpoints anotados (`@Operation`, `@ApiResponse`) para aparecer em `/swagger-ui.html`.
- Exemplo de payload GeoJSON no schema do `AreaCreateRequest`.

**Testes:**

- Unitários (`AreaServiceImpl`) com repository mockado.
- Integração com Testcontainers (`postgis/postgis:16-3.4`) para repository e endpoints REST — cobre todos os critérios de aceitação acima.
- ArchUnit: `controller` não acessa `repository` direto; `service` é o único cliente do `repository`.

## Fora de Escopo

- **Autenticação e autorização real** — feature dedicada (a specar). Esta feature depende dela para operar em prod, mas não a implementa.
- **UI de desenho de polígono em mapa** (Leaflet / Mapbox / MapLibre) — feature de frontend dedicada, exige ADR de escolha da lib de mapa.
- **Busca espacial avançada** ("qual área contém este ponto via `ST_Contains`") — entra junto com a feature de Registro de Resíduo.
- **Importação em massa** a partir de KML, Shapefile ou similar.
- **Hard-delete administrativo** de áreas sem mutirões associados.
- **Auditoria/histórico** de alterações de polígono (Hibernate Envers ou similar).
- **Validação de cobertura geográfica** (ex: "polígono deve estar dentro do município X") — fora de escopo do MVP.

## Dependências

- **ADR 0009 (a escrever):** estratégia de migrations Flyway e configuração do datasource Postgres em Spring Boot.
- **ADR 0010 (a escrever):** mapeamento geométrico no Hibernate (Hibernate Spatial vs JTS direto + tipo customizado).
- **Feature de Autenticação Google + JWT** (a specar): bloqueia *deploy em produção*, mas não bloqueia desenvolvimento/testes desta feature.
- **Imagem Testcontainers:** `postgis/postgis:16-3.4` (alinhada com Postgres 16 em prod no RDS — ADR 0006).
- **PostGIS habilitado** no banco local de dev e em prod (extensão criada via Flyway na primeira migration).

## Riscos e Mitigações

- **Risco:** Hibernate Spatial pode exigir dialeto customizado ou tipo de coluna explícito na versão atual de Spring Boot 3.3 / Hibernate 6.
  **Mitigação:** validar early com smoke test de repository (criar área, ler de volta, comparar polígono). Resolver em ADR 0010 antes de avançar para a entidade `Mutirao`.

- **Risco:** payload GeoJSON volumoso na listagem degrada performance ou consome banda do dispositivo do voluntário.
  **Mitigação:** endpoint `GET /api/areas` retorna `poligono=null` por default; `?incluirPoligono=true` traz o detalhe quando necessário (mapa). Detalhe individual (`GET /{id}`) sempre retorna o polígono completo.

- **Risco:** validação topológica falhar tarde (no insert, não na entrada), gerando rollback caro.
  **Mitigação:** chamar `ST_IsValid` em transação curta no service *antes* do `save()`. Se inválido, lançar exception mapeada para 400 sem tocar na transação principal.

- **Risco:** flag `app.auth.enabled=false` ser deixada acidentalmente em prod.
  **Mitigação:** aplicação faz fail-fast no startup quando `spring.profiles.active=prod` e `app.auth.enabled=false`.

## Decisões Pendentes

Nenhuma pendência interna a esta feature — as cinco decisões abertas na primeira versão da spec foram resolvidas e migradas para "Considerações Técnicas".

Pendência externa (não bloqueia esta feature, mas referenciada aqui para rastreabilidade):

- **Lib de mapa no frontend** (Leaflet, Mapbox GL, MapLibre) — necessária na feature de UI de cadastro de polígono. Resolver em ADR dedicado antes daquela feature começar.
