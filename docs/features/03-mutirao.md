# Feature 03: Cadastro de Mutirão

## Objetivo

Permitir à coordenação criar e gerenciar mutirões de coleta — com data, horário,
área e ciclo de vida próprio — e permitir a todos os usuários autenticados listar
e visualizar mutirões, habilitando o contexto obrigatório para o registro de
resíduos em campo.

## Contexto

O `Mutirao` é a entidade central do domínio (`docs/domain/model.md`): registros
de resíduo só existem dentro de um mutirão `EM_ANDAMENTO`, e relatórios são
sempre agregados por mutirão. Sem esta feature, o fluxo operacional da ONG não
pode começar.

A máquina de estados do mutirão tem quatro status — `PLANEJADO`, `EM_ANDAMENTO`,
`CONCLUIDO`, `CANCELADO` — e transições controladas exclusivamente pelo
`COORDENADOR`. Voluntários têm acesso de leitura apenas.

ADRs relevantes: `0003-monolito-em-vez-de-microservicos.md` (módulo `mutirao`
com camadas próprias), `0004-postgresql-com-postgis.md`,
`0009-flyway-migrations-e-datasource-postgres.md`.

## User Stories

- Como **coordenador**, quero criar um mutirão informando título, data, horário
  de início/fim, área e observações opcionais, para planejar a operação de coleta.
- Como **coordenador**, quero editar dados de um mutirão ainda `PLANEJADO`, para
  corrigir erros ou ajustar detalhes antes de iniciar.
- Como **coordenador**, quero iniciar um mutirão (`PLANEJADO` → `EM_ANDAMENTO`),
  para que voluntários possam registrar resíduos em campo.
- Como **coordenador**, quero encerrar um mutirão (`EM_ANDAMENTO` → `CONCLUIDO`),
  para sinalizar fim da coleta e tornar os registros imutáveis.
- Como **coordenador**, quero cancelar um mutirão (`PLANEJADO` ou `EM_ANDAMENTO`
  → `CANCELADO`), para registrar que a operação não aconteceu ou foi interrompida.
- Como **voluntário em campo**, quero ver a lista de mutirões com status, área e
  data, para saber em qual mutirão estou participando.
- Como **voluntário em campo**, quero ver o detalhe de um mutirão — incluindo
  status atual —, para confirmar que ele está `EM_ANDAMENTO` antes de registrar
  resíduos.

## Critérios de Aceitação

- [ ] **Criação válida** — dado payload com `titulo` não-vazio, `data` igual ou
  posterior a hoje, `horaInicio` < `horaFim`, `areaId` de área existente e ativa,
  quando `POST /api/mutiroes` por COORDENADOR, então retorna `201 Created` com
  o recurso e `status=PLANEJADO`.
- [ ] **Detalhe** — dado um mutirão existente, quando `GET /api/mutiroes/{id}` por
  qualquer usuário autenticado, então retorna `200` com todos os campos, incluindo
  dados básicos da área (id, nome, tipo, município).
- [ ] **Listagem com filtros** — dado N mutirões cadastrados, quando
  `GET /api/mutiroes?status=PLANEJADO&areaId=<uuid>&dataInicio=2025-06-01&dataFim=2025-06-30`,
  então retorna lista paginada em ordem cronológica (mais próximos primeiro).
- [ ] **Edição válida** — dado mutirão com `status=PLANEJADO`, quando
  `PUT /api/mutiroes/{id}` por COORDENADOR com campos válidos, então retorna
  `200` com o recurso atualizado.
- [ ] **Transição PLANEJADO → EM_ANDAMENTO** — dado mutirão `PLANEJADO`, quando
  `PATCH /api/mutiroes/{id}/status` com `{ "status": "EM_ANDAMENTO" }` por
  COORDENADOR, então retorna `200` com `status=EM_ANDAMENTO`.
- [ ] **Transição EM_ANDAMENTO → CONCLUIDO** — dado mutirão `EM_ANDAMENTO`, quando
  `PATCH /api/mutiroes/{id}/status` com `{ "status": "CONCLUIDO" }` por
  COORDENADOR, então retorna `200` com `status=CONCLUIDO`.
- [ ] **Transição PLANEJADO → CANCELADO** — dado mutirão `PLANEJADO`, quando
  `PATCH /api/mutiroes/{id}/status` com `{ "status": "CANCELADO" }` por
  COORDENADOR, então retorna `200` com `status=CANCELADO`.
- [ ] **Transição EM_ANDAMENTO → CANCELADO** — dado mutirão `EM_ANDAMENTO`, quando
  `PATCH /api/mutiroes/{id}/status` com `{ "status": "CANCELADO" }` por
  COORDENADOR, então retorna `200` com `status=CANCELADO`.
- [ ] **Transição inválida — estado terminal** — dado mutirão `CONCLUIDO` ou
  `CANCELADO`, quando `PATCH /api/mutiroes/{id}/status` com qualquer status,
  então retorna `409 Conflict` com mensagem indicando que o mutirão está em
  estado terminal.
- [ ] **Transição inválida — pulo de estado** — dado mutirão `PLANEJADO`, quando
  `PATCH /api/mutiroes/{id}/status` com `{ "status": "CONCLUIDO" }`, então
  retorna `409 Conflict` com mensagem descrevendo a transição inválida.
- [ ] **Edição bloqueada fora de PLANEJADO** — dado mutirão `EM_ANDAMENTO`,
  `CONCLUIDO` ou `CANCELADO`, quando `PUT /api/mutiroes/{id}` por COORDENADOR,
  então retorna `409 Conflict` com mensagem indicando que apenas mutirões
  `PLANEJADO` podem ser editados.
- [ ] **Erro: área inativa ou inexistente** — dado `areaId` de área com
  `ativa=false` ou UUID não cadastrado, quando `POST` ou `PUT`, então retorna
  `422 Unprocessable Entity` com mensagem sobre a área.
- [ ] **Erro: `horaFim` ≤ `horaInicio`** — dado payload com `horaFim` igual ou
  anterior a `horaInicio`, quando `POST` ou `PUT`, então retorna `400 Bad Request`
  com mensagem sobre o intervalo de horário.
- [ ] **Erro: `data` no passado** — dado `data` anterior a hoje na criação, quando
  `POST`, então retorna `400 Bad Request`.
- [ ] **Erro: campos obrigatórios ausentes** — dado payload sem `titulo`, `data`,
  `horaInicio`, `horaFim` ou `areaId`, quando `POST`, então retorna `400` com
  lista dos campos inválidos.
- [ ] **Erro: VOLUNTARIO tenta criar** — dado token com role `VOLUNTARIO`, quando
  `POST /api/mutiroes`, então retorna `403 Forbidden`.
- [ ] **Erro: VOLUNTARIO tenta transicionar** — dado token com role `VOLUNTARIO`,
  quando `PATCH /api/mutiroes/{id}/status`, então retorna `403 Forbidden`.
- [ ] **Erro: VOLUNTARIO tenta editar** — dado token com role `VOLUNTARIO`, quando
  `PUT /api/mutiroes/{id}`, então retorna `403 Forbidden`.
- [ ] **Erro: ID inexistente** — dado UUID não cadastrado, quando `GET`, `PUT` ou
  `PATCH /api/mutiroes/{id}`, então retorna `404 Not Found`.

## Considerações Técnicas

### Estrutura no módulo `com.marsemlixo.api.mutirao`

| Camada       | Componentes                                                         |
|--------------|---------------------------------------------------------------------|
| `controller` | `MutiraoController` (REST)                                          |
| `service`    | `MutiraoService` (interface) + `MutiraoServiceImpl`                 |
| `repository` | `MutiraoRepository extends JpaRepository<Mutirao, UUID>`            |
| `domain`     | `Mutirao` (entidade JPA), `MutiraoStatus` (enum)                    |

---

### Endpoints REST

| Método | Path                          | Role exigida              | Ação                                     |
|--------|-------------------------------|---------------------------|------------------------------------------|
| POST   | `/api/mutiroes`               | `COORDENADOR`             | Cria mutirão (`status=PLANEJADO`)        |
| GET    | `/api/mutiroes`               | `VOLUNTARIO`, `COORDENADOR` | Lista com filtros e paginação          |
| GET    | `/api/mutiroes/{id}`          | `VOLUNTARIO`, `COORDENADOR` | Detalhe completo                       |
| PUT    | `/api/mutiroes/{id}`          | `COORDENADOR`             | Edita dados (somente `PLANEJADO`)        |
| PATCH  | `/api/mutiroes/{id}/status`   | `COORDENADOR`             | Transição de estado                      |

Autorização via `@PreAuthorize` nos métodos do controller, consistente com a
convenção adotada na Feature 02.

---

### DTOs (records Java)

**`MutiraoCreateRequest`** — usado em `POST`:

```json
{
  "titulo":      "Mutirão Praia do Forte — Junho/2025",
  "data":        "2025-06-14",
  "horaInicio":  "08:00",
  "horaFim":     "12:00",
  "areaId":      "550e8400-e29b-41d4-a716-446655440000",
  "observacoes": "Levar luvas e sacos de 100L."
}
```

Bean Validation: `@NotBlank` (titulo), `@NotNull` (data, horaInicio, horaFim,
areaId), `@FutureOrPresent` (data). Validação de `horaFim > horaInicio` e de
área ativa no service (não é Bean Validation — exige consulta ao banco).

**`MutiraoUpdateRequest`** — usado em `PUT` (campos obrigatórios, mesma estrutura
do create):

Mesmo payload do create. `PUT` exige todos os campos — semântica de substituição
completa dos dados básicos. Status não faz parte do payload de atualização.

**`MutiraoStatusRequest`** — usado em `PATCH /status`:

```json
{ "status": "EM_ANDAMENTO" }
```

`status` obrigatório e deve ser valor válido do enum `MutiraoStatus`.

**`MutiraoResponse`** — retornado em todas as operações de escrita e leitura:

```json
{
  "id":            "7f3e1a2b-...",
  "titulo":        "Mutirão Praia do Forte — Junho/2025",
  "data":          "2025-06-14",
  "horaInicio":    "08:00",
  "horaFim":       "12:00",
  "status":        "PLANEJADO",
  "observacoes":   "Levar luvas e sacos de 100L.",
  "area": {
    "id":        "550e8400-...",
    "nome":      "Praia do Forte",
    "tipo":      "PRAIA",
    "municipio": "Cabo Frio"
  },
  "organizador": {
    "id":   "a1b2c3d4-...",
    "nome": "Maria Coordenadora"
  }
}
```

`organizadorId` preenchido automaticamente no backend a partir do JWT do
usuário autenticado na criação — não aceito como campo de entrada.

**`MutiraoListResponse`** — retornado em `GET /api/mutiroes`:

Paginação Spring (`Page<MutiraoSummaryResponse>`). O summary omite `observacoes`
para reduzir payload na listagem:

```json
{
  "content": [
    {
      "id":         "7f3e1a2b-...",
      "titulo":     "Mutirão Praia do Forte — Junho/2025",
      "data":       "2025-06-14",
      "horaInicio": "08:00",
      "horaFim":    "12:00",
      "status":     "PLANEJADO",
      "area": {
        "id":        "550e8400-...",
        "nome":      "Praia do Forte",
        "tipo":      "PRAIA",
        "municipio": "Cabo Frio"
      }
    }
  ],
  "totalElements": 12,
  "totalPages":    2,
  "size":          10,
  "number":        0
}
```

Parâmetros de query aceitos: `status` (enum), `areaId` (UUID), `dataInicio`
(ISO date), `dataFim` (ISO date), `page` (default 0), `size` (default 10, máx 50).
Ordenação: `data ASC, horaInicio ASC` (mais próximos primeiro).

---

### Entidade JPA

```java
@Entity
@Table(name = "mutirao")
public class Mutirao {
    @Id @GeneratedValue UUID id;
    @Column(nullable = false) String titulo;
    @Column(nullable = false) LocalDate data;
    @Column(nullable = false) LocalTime horaInicio;
    @Column(nullable = false) LocalTime horaFim;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false) Area area;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false) Voluntario organizador;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) MutiraoStatus status;
    @Column(columnDefinition = "text") String observacoes;
}
```

`MutiraoStatus` enum: `PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`.

A lógica de transição de estado fica no `MutiraoServiceImpl`, não na entidade —
o service valida a transição antes de alterar o campo e persistir.

---

### Máquina de estados — implementação

Transições permitidas (validadas no service):

| De             | Para           | Permitida? |
|----------------|----------------|------------|
| PLANEJADO      | EM_ANDAMENTO   | Sim        |
| PLANEJADO      | CANCELADO      | Sim        |
| EM_ANDAMENTO   | CONCLUIDO      | Sim        |
| EM_ANDAMENTO   | CANCELADO      | Sim        |
| CONCLUIDO      | qualquer       | Não        |
| CANCELADO      | qualquer       | Não        |

Transição inválida lança exceção de domínio mapeada para `409 Conflict` com
`problem+json` (RFC 7807), consistente com o padrão de erro das features
anteriores.

---

### Migration Flyway

**`V5__create_mutirao.sql`:**

```sql
CREATE TABLE mutirao (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo         VARCHAR(255) NOT NULL,
    data           DATE         NOT NULL,
    hora_inicio    TIME         NOT NULL,
    hora_fim       TIME         NOT NULL,
    area_id        UUID         NOT NULL REFERENCES area(id),
    organizador_id UUID         NOT NULL REFERENCES voluntario(id),
    status         VARCHAR(50)  NOT NULL DEFAULT 'PLANEJADO',
    observacoes    TEXT
);

CREATE INDEX idx_mutirao_status  ON mutirao(status);
CREATE INDEX idx_mutirao_area_id ON mutirao(area_id);
CREATE INDEX idx_mutirao_data    ON mutirao(data);
```

Índice em `data` suporta os filtros de intervalo de datas da listagem.
Índice em `status` suporta o filtro mais frequente (voluntários buscam
mutirões `EM_ANDAMENTO`).

---

### Validações de negócio no service

1. `horaFim > horaInicio` — validada no service antes do persist; lança exceção
   mapeada para `400`.
2. `area.ativa == true` — verificada via `AreaRepository.findById(areaId)`;
   área inexistente ou inativa lança exceção mapeada para `422`.
3. Edição só quando `status == PLANEJADO` — verificada antes do `save()`, lança
   `409` caso contrário.
4. Transição de status — verificada contra a tabela de transições permitidas
   acima; estado terminal ou pulo de estado lança `409`.
5. `organizadorId` preenchido a partir do `SecurityContextHolder` (JWT claim
   `sub`) — nunca aceito via payload de entrada.

---

### Frontend — telas

**Lista de mutirões** (`/mutiroes`):
- Cards com: título, área (nome + tipo), data formatada, horário, badge de status
  com cor distinta por estado (PLANEJADO: azul, EM_ANDAMENTO: verde, CONCLUIDO:
  cinza, CANCELADO: vermelho).
- Filtros: status, período (data início/fim). Paginação.
- Botão "Novo Mutirão" visível apenas para COORDENADOR.

**Formulário de criação/edição** (`/mutiroes/novo`, `/mutiroes/{id}/editar`):
- Campos: título (text), data (date picker), hora início/fim (time picker), área
  (select com áreas ativas), observações (textarea).
- Validação client-side via React Hook Form + Zod antes do submit.
- Acesso restrito a COORDENADOR (redirecionamento se VOLUNTARIO tentar acessar).

**Detalhe do mutirão** (`/mutiroes/{id}`):
- Todos os campos, incluindo organizador e observações.
- Painel de ações de status: botões de transição visíveis e habilitados apenas
  para COORDENADOR, condicionais ao status atual:
  - `PLANEJADO`: botões "Iniciar" e "Cancelar".
  - `EM_ANDAMENTO`: botões "Encerrar" e "Cancelar".
  - `CONCLUIDO` / `CANCELADO`: sem botões de ação; badge informativo.
- Botão "Editar" visível apenas para COORDENADOR e apenas quando `PLANEJADO`.

**Gerenciamento de estado no frontend:** TanStack Query para fetch e invalidação
de cache após mutações (criação, edição, transição de status).

---

### Testes

**Unitários (`MutiraoServiceTest`):** mock de `MutiraoRepository` e `AreaRepository`.
Cobrir: criação válida, `horaFim ≤ horaInicio`, área inativa, área inexistente,
todas as transições válidas, todas as transições inválidas (estados terminais,
pulos de estado), edição fora de `PLANEJADO`.

**Integração (`MutiraoControllerIT`):** Testcontainers (`postgis/postgis:16-3.4`).
Cobrir: ciclo de vida completo PLANEJADO → EM_ANDAMENTO → CONCLUIDO, autorização
(VOLUNTARIO bloqueado em POST/PUT/PATCH), filtros de listagem, paginação, 404
para ID inexistente.

**ArchUnit:** `MutiraoController` não acessa `MutiraoRepository` diretamente;
`MutiraoServiceImpl` é o único cliente do repositório.

## Fora de Escopo

- **Registro de resíduos** — feature dedicada (Feature 04). Esta feature apenas
  habilita o contexto (`EM_ANDAMENTO`) para que registros possam existir.
- **Listagem de voluntários participantes por mutirão** — modelo atual não tem
  tabela de participação explícita; voluntário "participa" ao fazer seu primeiro
  registro. Feature futura se a ONG precisar de RSVP ou lista de presença.
- **Co-organizadores** — modelo atual suporta apenas um `organizadorId` por
  mutirão. Virar relação N:N se a ONG operar com múltiplos coordenadores por
  evento (ver Decisões Pendentes no `docs/domain/model.md`).
- **Notificações** (push, email) de abertura ou encerramento de mutirão.
- **Comentários ou log de auditoria** de transições de estado (quem mudou, quando).
- **Hard-delete** de mutirão em qualquer status.
- **Relatório de mutirão** (geração de Excel) — feature dedicada.
- **Sincronização offline** de mutirões no Dexie.js — a listagem e o detalhe do
  mutirão precisam de conectividade nesta feature; cache offline entra junto com
  a feature de sincronização (Feature 06 conforme `vision.md`).

## Dependências

- **Feature 01 (Cadastro de Área)** — entidade `Area` e tabela `area` devem
  existir; a validação de área ativa depende de `AreaRepository`.
- **Feature 02 (Autenticação)** — Spring Security com JWT próprio deve estar
  ativo; `organizadorId` é extraído do JWT; autorização por role
  (`@PreAuthorize`) depende do `JwtAuthenticationConverter` configurado.
- **Migration `V5__create_mutirao.sql`** — depende de `V3__create_voluntario.sql`
  (FK para `voluntario`) e `V2__create_area.sql` (FK para `area`).

## Riscos e Mitigações

- **Risco:** listagem com múltiplos filtros combinados pode gerar queries lentas
  conforme volume cresce.
  **Mitigação:** índices em `status`, `area_id` e `data` cobrem os filtros mais
  frequentes. Usar `Specification` JPA para compor predicados dinamicamente,
  evitando queries fixas com condicionais em HQL. Avaliar `EXPLAIN ANALYZE` se
  volume justificar.

- **Risco:** race condition em transição de status concorrente (dois coordenadores
  tentando encerrar o mesmo mutirão simultaneamente).
  **Mitigação:** `UPDATE mutirao SET status = ? WHERE id = ? AND status = ?` como
  operação atômica no service; se `0 rows affected`, relançar como `409`. Evita
  locks pessimistas sem abrir mão da consistência.

- **Risco:** frontend renderizando ações de status desatualizadas (ex: coordenador
  A encerra, coordenador B ainda vê botão "Encerrar").
  **Mitigação:** TanStack Query com `staleTime` curto (30s) na tela de detalhe
  e invalidação imediata após mutação bem-sucedida. `409` do backend com mensagem
  clara instrui o usuário a recarregar.

## Decisões Pendentes

- **`data` no passado em edição:** a regra `@FutureOrPresent` foi definida apenas
  para criação. Na edição de mutirão `PLANEJADO`, permitir ou bloquear alteração
  de `data` para uma data passada? (Ex: coordenador esqueceu de iniciar no dia
  correto.) Hipótese atual: bloquear — alinhado com a invariante do model. Confirmar
  com a coordenação da ONG antes de implementar.

- **Paginação padrão:** `size=10` e `size` máximo de 50 são estimativas. Validar
  com a coordenação quantos mutirões são típicos em um período de consulta.

- **Ordenação da listagem:** definida como `data ASC, horaInicio ASC` (mais próximos
  primeiro). Coordenação pode preferir os mais recentes no topo para histórico.
  Confirmar antes de implementar — se ordenação precisar ser configurável via
  parâmetro, a query fica mais complexa.
