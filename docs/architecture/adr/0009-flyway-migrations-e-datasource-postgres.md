# ADR 0009: Estratégia de migrations Flyway e configuração do datasource

## Status

Aceito (2026-05)

## Contexto

O ADR 0004 fixou PostgreSQL com PostGIS e Flyway como ferramentas, mas
não definiu *como* o backend Spring Boot conecta ao banco nem como as
migrations são organizadas. A feature 01 (Cadastro de Área) é a
primeira a tocar persistência e destrava o restante do domínio — sem
um padrão acordado, cada PR de feature traz risco de divergência.

Pontos em aberto que este ADR fecha:

- Convenção de nomenclatura e localização das migrations
- Estratégia de baseline (banco vazio no MVP)
- Onde a extensão PostGIS é habilitada
- Comportamento em caso de falha de migration durante startup
- Configuração do pool de conexões
- Como o datasource é provisionado em dev / test / prod

## Decisão

**Flyway com migrations versionadas, integradas ao ciclo de startup do
Spring Boot.**

- Localização: `backend/src/main/resources/db/migration/`
- Convenção: `V<n>__<descricao_em_snake_case>.sql` (somente *versioned*
  no MVP; *repeatable* `R__` e *undo* `U__` ficam fora até haver
  necessidade concreta — views, triggers, etc.)
- `spring.flyway.baseline-on-migrate=false` — o MVP parte de banco
  vazio
- `spring.flyway.fail-on-missing-locations=true` — falha de
  configuração é loud, não silent
- Primeira migration: `V1__enable_postgis.sql` com
  `CREATE EXTENSION IF NOT EXISTS postgis;`. PostGIS entra na trilha
  versionada, não em bootstrap externo, garantindo rastreabilidade
- Falha de migration no startup → aplicação **não sobe** (default do
  Spring Boot, mantido)
- Pool de conexões: HikariCP padrão do Spring Boot; sem tuning custom
  enquanto observabilidade não indicar gargalo
- Migrations imutáveis após merge para `main`; correções vêm como
  migration nova (`V<n+1>__fix_...`)

**Configuração do datasource por perfil:**

| Perfil | Origem do datasource                                          |
|--------|---------------------------------------------------------------|
| dev    | Docker Compose local com `postgis/postgis:16-3.4`             |
| test   | Testcontainers com `postgis/postgis:16-3.4`, mesmas migrations |
| prod   | RDS PostgreSQL (ADR 0006) com credenciais via Secrets Manager |

URL, usuário e senha são externalizados via `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` —
nenhuma credencial em `application.yml`.

## Alternativas consideradas

- **Liquibase em vez de Flyway.** Mais flexível (XML/YAML/JSON além de
  SQL), com rollback declarativo. Rejeitada porque SQL puro do Flyway
  é mais transparente para revisão em PR, alinha com a expertise
  Spring/Java do dono do produto, e o histórico do projeto não tem
  cenário de rollback automatizado planejado (correção via migration
  nova é suficiente).

- **Migrations em Java (Flyway `JavaMigration`).** Permitem lógica
  programática complexa. Rejeitada porque o domínio do MVP é CRUD com
  uma extensão geoespacial — SQL resolve sem boilerplate Java extra.
  Pode ser introduzida pontualmente sem ADR novo se aparecer cenário
  legítimo (data migration complexa).

- **Sem migrations: Hibernate `ddl-auto=update`.** Geração automática
  do schema a partir das entidades JPA. Rejeitada porque perde
  previsibilidade em prod (mudanças silenciosas), impede colunas
  customizadas (PostGIS, índices GiST, generated columns) e é
  consenso de mercado evitar em qualquer ambiente além de prototipagem.

- **PostGIS habilitada via bootstrap externo (CDK ou setup manual no
  RDS).** Cria a extensão fora da trilha de migrations. Rejeitada
  porque amarra a habilitação ao provisioning de infra, perdendo
  rastreabilidade no histórico de schema e tornando ambientes locais
  divergentes do RDS.

## Consequências

**Positivas:**
- Histórico de schema versionado e auditável em PRs
- Mesma sequência de migrations em dev / test / prod, eliminando
  classe inteira de bug "funciona na minha máquina"
- Fail-fast em startup torna inconsistências de schema visíveis no
  primeiro segundo, não na primeira request com erro 500
- SQL puro mantém o que está acontecendo legível para qualquer DBA
- Naming convention reduz bikeshedding em PRs

**Negativas:**
- Migrations imutáveis após merge: erros viram migration corretiva,
  o que polui um pouco o histórico mas é o trade-off aceito
- PostGIS exige permissão `CREATE EXTENSION` no role da aplicação na
  primeira migration; em RDS, isso requer role `rds_superuser` ou um
  bootstrap manual da extensão antes do primeiro deploy. Mitigação:
  documentar no README do backend e tratar no CDK (ADR 0006)
- Fail-fast em startup pode bloquear deploy inteiro se migration tiver
  bug; mitigação: rodar migrations contra Testcontainers no CI antes
  de promover para prod
- HikariCP default pode subdimensionar em volume alto; aceito porque
  o volume previsto é baixo (ADR 0003) e tuning fica para quando o
  problema aparecer

**Neutras:**
- Repeatable migrations (`R__`) podem ser introduzidas sem ADR novo
  quando houver caso justificado (views, triggers idempotentes)
- Tipo de teste de integração padrão passa a ser Testcontainers
  com PostGIS, herdando a mesma trilha de migrations
