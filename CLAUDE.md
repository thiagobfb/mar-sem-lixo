# CLAUDE.md

Guia para o Claude Code ao trabalhar neste repositório.

## Intenção do projeto

Mar Sem Lixo é uma PWA + API REST para voluntários de ONG ambiental registrarem
coleta de resíduos em praias e lagoas (Região dos Lagos, RJ), com geração de
relatórios consolidados em Excel para a coordenação.

Atende dois perfis: voluntários (registram resíduos em campo via celular,
inclusive offline) e coordenação (visualiza dados, gera relatórios, gerencia
mutirões e áreas geográficas).

## Stack

### Backend (`/backend`)
- Spring Boot 3.3+ com Java 21
- Spring Data JPA + PostgreSQL com extensão PostGIS
- Spring Security com JWT próprio (Google OAuth no front, backend valida ID Token e emite JWT da aplicação)
- Flyway para migrations
- Apache POI para geração de Excel
- Springdoc OpenAPI
- Testcontainers + JUnit 5 + ArchUnit
- Dockerfile multi-stage com layered JAR e usuário non-root

### Frontend (`/frontend`)
- React + TypeScript + Vite (configurado como PWA)
- Tailwind CSS + shadcn/ui
- TanStack Query, React Hook Form + Zod
- Dexie.js (IndexedDB) para persistência offline
- Workbox para service worker e estratégia offline-first
- @react-oauth/google para autenticação

### Infraestrutura (`/infra`)
- AWS CDK em Java
- ECS Fargate (backend), RDS PostgreSQL com PostGIS (banco)
- S3 + CloudFront (frontend), S3 (fotos de resíduos)
- GitHub Actions para CI/CD

## Estrutura do repositório

Monorepo com fronteiras claras:

```
backend/    Spring Boot
frontend/   PWA React
infra/      AWS CDK em Java
docs/       Specs (vision, domain, architecture, features)
.github/    Workflows de CI/CD
```

## Documentação de referência

Conforme os specs forem criados, este CLAUDE.md vai apontar para eles.
Estrutura prevista (será populada ao longo do desenvolvimento):

- `docs/vision.md` — problema, usuários, escopo do MVP, métricas
- `docs/domain/model.md` — entidades, relacionamentos, invariantes
- `docs/domain/glossary.md` — linguagem ubíqua
- `docs/architecture/overview.md` — arquitetura geral
- `docs/architecture/adr/` — decisões em formato Michael Nygard
- `docs/features/` — uma spec por feature

## Convenções

**Commits:** Conventional Commits em português (`feat:`, `fix:`, `docs:`,
`chore:`, `refactor:`, `test:`, `build:`, `ci:`).

**Branches:** `main` protegida. Feature branches com PR para merge.

**Java:** preferir features modernas — records para DTOs, sealed types para
hierarquias fechadas, pattern matching em switch, virtual threads habilitadas
via `spring.threads.virtual.enabled=true`.

**Testes:** Testcontainers para testes que tocam banco (PostgreSQL real,
nunca H2). ArchUnit para regras de arquitetura. JUnit 5.

**SQL:** PostgreSQL puro, sem prefixo de schema em queries ou DDL. Migrations
versionadas via Flyway. Nada de hardcoded schema name.

**Idioma:** strings de UI, mensagens ao usuário, comentários de regra de
negócio e documentação em português. Código (classes, métodos, variáveis,
endpoints REST) em inglês. Mensagens de log em inglês.

**ADR:** mudança arquitetural significativa exige ADR novo antes do código.

## Restrições

**Nunca:**
- Credenciais hardcoded em qualquer arquivo
- Bypass de Spring Security para "facilitar testes"
- `catch (Exception e)` sem justificativa explícita
- Lógica de negócio em controllers (tudo em service)
- Schema name em SQL (`schema.tabela`)
- Pular ADR em mudanças que afetam arquitetura

**Sempre:**
- Validação com Bean Validation (`@Valid`, `@NotNull`, etc.) em DTOs de entrada
- DTO de entrada e DTO de saída separados (nunca expor entidade JPA direto)
- Atualizar specs em `docs/` quando mudar contrato de API ou modelo de domínio
- Cobertura de teste em camada de service e controller

## Idioma de comunicação

Respostas ao usuário em português brasileiro. Conteúdo de UI, mensagens de
erro ao usuário e documentação em português. Código e nomes técnicos em inglês.