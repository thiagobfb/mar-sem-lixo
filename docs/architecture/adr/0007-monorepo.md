# ADR 0007: Estrutura de repositório monorepo

## Status

Aceito (2026-05)

## Contexto

O projeto contém múltiplos componentes que vão evoluir juntos:

- Backend Spring Boot
- Frontend PWA React
- Infraestrutura como código (CDK Java)
- Documentação versionada (specs, ADRs, vision, glossary, model)
- Configurações de CI/CD

Características relevantes:

- **Solo developer**: não há equipes separadas que justifiquem
  fronteiras de repositório por permissão ou cadência de trabalho
- **Mudanças cross-cutting são comuns**: alterar contrato de API
  exige mudança simultânea em backend, frontend e specs
- **Portfólio**: recrutador deve conseguir entender o projeto
  inteiro a partir de um único link, sem caçar repositórios
  relacionados
- **Specs governam todos os componentes**: vision, modelo de domínio
  e ADRs aplicam-se ao sistema todo, não a um lado

A escolha entre repositório único (monorepo) e múltiplos repositórios
(polyrepo) afeta organização, fluxo de trabalho e percepção do
projeto.

## Decisão

Usar **repositório único (monorepo)** com a seguinte estrutura:

```
mar-sem-lixo/
├── README.md                # Cartão de visita do projeto
├── LICENSE                  # Apache 2.0
├── .gitignore               # Híbrido: Java + Node + IDE + OS
├── CLAUDE.md                # Contexto para Claude Code
├── docs/                    # Specs versionados (governam todo o projeto)
│   ├── vision.md
│   ├── domain/
│   ├── architecture/
│   │   └── adr/
│   └── features/
├── backend/                 # Spring Boot
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/
│   ├── CLAUDE.md
│   └── README.md
├── frontend/                # PWA React
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   ├── CLAUDE.md
│   └── README.md
├── infra/                   # AWS CDK em Java
│   ├── pom.xml
│   ├── src/
│   └── README.md
└── .github/
    └── workflows/           # Workflows com path filters
        ├── backend-ci.yml
        ├── frontend-ci.yml
        └── deploy.yml
```

Cada componente (`backend/`, `frontend/`, `infra/`) mantém seu próprio
build nativo (Maven, npm, Maven respectivamente) sem ferramenta meta
(Turborepo, Nx, Bazel).

GitHub Actions usa `paths:` filter em cada workflow para rodar build
apenas quando arquivos relevantes mudam, evitando builds desnecessários.

## Alternativas consideradas

- **Polyrepo (repositórios separados para backend, frontend e infra).**
  Rejeitada por: fragmentação do portfólio (recrutador precisa achar
  três repos e conectar mentalmente), overhead de PRs cruzados em
  mudanças que afetam múltiplos componentes (ex: novo campo em DTO),
  necessidade de duplicar configuração de CI e specs, e dificuldade
  de manter contratos sincronizados.

- **Monorepo com ferramenta meta (Turborepo, Nx, Bazel).** Rejeitada
  como obrigatória no MVP. Essas ferramentas brilham em monorepos
  JavaScript com muitos pacotes interdependentes (sharing de
  componentes, tipos compartilhados); aqui o backend e o frontend são
  stacks independentes em linguagens diferentes (Java + TypeScript),
  o que minimiza o ganho. Pode ser introduzido em ADR posterior se a
  complexidade crescer.

- **Submodules Git.** Rejeitada por agregar complexidade operacional
  (clone, atualização, sync) sem benefício claro neste cenário.

## Consequências

**Positivas:**
- Fonte única de verdade para o projeto inteiro
- Commits atômicos cross-cutting são possíveis (mudança coordenada
  em backend + frontend + specs no mesmo PR)
- Specs em `docs/` governam todo o projeto sem duplicação
- Recrutador acessa o projeto inteiro em um link
- Setup de CI mais simples (uma configuração, com filtros por path)
- Histórico unificado de evolução do produto

**Negativas:**
- Clone do repositório baixa código que talvez nem todo contribuidor
  precise (irrelevante para projeto solo, mas vale considerar se
  abrir contribuição)
- CI precisa de filtros de path para evitar builds desnecessários
  (configuração simples de fazer)
- Espaço em disco maior por contribuidor

**Neutras:**
- Sem ferramenta meta de monorepo, mas as build chains nativas (Maven
  e npm) lidam bem com seus respectivos diretórios
- Fronteiras claras entre `backend/`, `frontend/` e `infra/` evitam
  acoplamento acidental
