# Architecture Decision Records (ADRs)

Esta pasta contém os Architecture Decision Records do projeto Mar Sem Lixo,
no formato proposto por Michael Nygard. Cada ADR documenta uma decisão
arquitetural significativa, seu contexto, alternativas consideradas e
consequências.

## Índice

| Número | Título | Status |
|--------|--------|--------|
| [0001](0001-stack-backend-spring-boot-java21.md) | Stack backend: Spring Boot 3 com Java 21 | Aceito |
| [0002](0002-frontend-pwa-em-vez-de-react-native.md) | Frontend como PWA em vez de React Native | Aceito |
| [0003](0003-monolito-em-vez-de-microservicos.md) | Arquitetura monolito em vez de microsserviços | Aceito |
| [0004](0004-postgresql-com-postgis.md) | PostgreSQL com PostGIS como banco de dados | Aceito |
| [0005](0005-google-oauth-com-jwt-proprio.md) | Autenticação via Google OAuth com JWT próprio | Aceito |
| [0006](0006-aws-com-cdk-em-java.md) | Cloud AWS com infraestrutura como código em CDK Java | Aceito |
| [0007](0007-monorepo.md) | Estrutura de repositório monorepo | Aceito |
| [0008](0008-multi-stage-dockerfile-com-layered-jar.md) | Dockerfile multi-stage com layered JAR | Aceito |

## Convenções

- ADRs são **imutáveis após aceitos**. Mudanças geram um novo ADR que
  substitui (`Supersedes`) o anterior.
- Status possíveis: `Proposto`, `Aceito`, `Substituído por NNNN`, `Revogado`.
- Numeração sequencial, nunca reutilizada, mesmo após revogação.
- Cada arquivo segue o template Nygard: Status, Contexto, Decisão,
  Alternativas, Consequências.

## Quando criar um novo ADR

Mudanças arquiteturais significativas devem ser precedidas de ADR. Exemplos:

- Adoção, troca ou remoção de tecnologia (linguagem, framework, banco)
- Mudança em padrão de comunicação (REST → gRPC, sync → async)
- Mudança em estratégia de deploy ou hospedagem
- Mudança em estratégia de autenticação ou autorização
- Mudança em modelo de domínio com impacto em múltiplos componentes

Mudanças menores (refatorações localizadas, bug fixes, ajustes de
configuração) não exigem ADR.

## Template

Para criar um novo ADR, copie o conteúdo abaixo:

```markdown
# ADR NNNN: Título conciso da decisão

## Status

Proposto | Aceito (YYYY-MM) | Substituído por NNNN | Revogado

## Contexto

Descrição da situação que motiva a decisão. Quais forças estão em jogo?
Quais restrições? Por que é necessário decidir agora?

## Decisão

A decisão tomada, em voz ativa e direta. "Vamos usar X."

## Alternativas consideradas

- **Alternativa A.** Por que foi avaliada e por que foi rejeitada.
- **Alternativa B.** Idem.

## Consequências

**Positivas:**
- Lista do que fica mais fácil ou melhor.

**Negativas:**
- Lista do que fica mais difícil ou pior.

**Neutras:**
- Lista do que muda mas sem juízo de valor claro.
```
