# Mar Sem Lixo

> Aplicativo PWA para registro estruturado de coleta de resíduos em mutirões de limpeza ambiental, em apoio ao trabalho da [ONG Mar Sem Lixo](https://www.projetomarsemlixo.com.br).

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

## Sobre o projeto

A ONG **Mar Sem Lixo**, sediada em Cabo Frio (RJ), realiza desde 2021 mutirões de limpeza, educação ambiental e identificação/controle de lixo marinho em parceria com comunidades locais, escolas, prefeituras, Ministério Público Federal, ICMBio e colônias de pescadores. Em 2021, ações da ONG retiraram mais de 28 toneladas de lixo de mais de 150 praias do litoral brasileiro.

Parte essencial do trabalho consiste em **contabilizar e triar o material recolhido durante os mutirões**, gerando dados quantitativos e qualitativos que alimentam relatórios entregues a parceiros institucionais. Hoje esse registro é feito manualmente em caderno por voluntários durante o mutirão e consolidado posteriormente — fluxo lento, sujeito a perdas e sem georreferenciamento dos pontos de coleta.

Este projeto fornece um aplicativo PWA que substitui o caderno por um registro digital offline-first via celular, com geolocalização automática, foto opcional e sincronização ao reconectar — permitindo à coordenação gerar relatórios consolidados sem retrabalho.

## Funcionalidades do MVP

- Autenticação via Google (sem cadastro com senha)
- Cadastro de áreas de atuação com polígonos geográficos
- Criação e gestão de mutirões pela coordenação
- Registro de resíduos em campo: tipo, dimensões, quantidade, geolocalização e foto opcional
- Funcionamento offline-first com sincronização automática ao reconectar
- Visão consolidada para coordenação com filtros por mutirão, área, período e tipo
- Exportação de relatórios em Excel

## Stack

**Backend**
- Spring Boot 3.3+ com Java 21
- PostgreSQL com extensão PostGIS
- Spring Security (Google OAuth + JWT próprio)
- Apache POI (geração de Excel)
- Flyway, Springdoc OpenAPI
- Testcontainers, JUnit 5, ArchUnit

**Frontend**
- React + TypeScript + Vite (configurado como PWA)
- Tailwind CSS + shadcn/ui
- TanStack Query, React Hook Form + Zod
- Dexie.js (IndexedDB), Workbox (estratégia offline-first)

**Infraestrutura**
- AWS — ECS Fargate, RDS PostgreSQL, S3, CloudFront
- AWS CDK em Java (Infrastructure as Code)
- GitHub Actions (CI/CD)

## Estrutura do repositório

```
mar-sem-lixo/
├── backend/         Spring Boot + Java 21
├── frontend/        PWA React + TypeScript
├── infra/           AWS CDK em Java
├── docs/            Especificações (vision, domain, architecture)
└── .github/         Workflows de CI/CD
```

## Documentação

A arquitetura e o domínio do projeto estão documentados em `docs/`:

- [`docs/vision.md`](docs/vision.md) — problema, usuários, escopo do MVP, métricas de sucesso
- [`docs/domain/glossary.md`](docs/domain/glossary.md) — linguagem ubíqua do domínio
- [`docs/domain/model.md`](docs/domain/model.md) — modelo de domínio, entidades e regras de negócio
- [`docs/architecture/adr/`](docs/architecture/adr/) — Architecture Decision Records (ADRs)

## Como rodar localmente

Cada componente tem instruções específicas em seu próprio README:

- [`backend/README.md`](backend/README.md) — requisitos, configuração e comandos do backend
- [`frontend/README.md`](frontend/README.md) — requisitos, configuração e comandos do frontend
- [`infra/README.md`](infra/README.md) — pré-requisitos e deploy da infraestrutura

## Status do projeto

MVP em desenvolvimento. O projeto adota Spec Driven Development — toda decisão arquitetural significativa é precedida de ADR e toda feature por uma especificação em `docs/features/`.

## Contribuição

Este projeto é uma contribuição voluntária à ONG Mar Sem Lixo, desenvolvido como projeto open-source. Sugestões, melhorias e correções são bem-vindas via issues e pull requests.

Antes de propor mudanças significativas, recomenda-se a leitura dos ADRs em [`docs/architecture/adr/`](docs/architecture/adr/) para entender o racional das decisões existentes.

## Licença

Distribuído sob a licença Apache 2.0. Veja [LICENSE](LICENSE) para o texto completo.

## Reconhecimentos

ONG **Mar Sem Lixo** ([projetomarsemlixo.com.br](https://www.projetomarsemlixo.com.br)) pelo trabalho ambiental que inspira este projeto.