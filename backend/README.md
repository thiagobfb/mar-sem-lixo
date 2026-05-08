# Mar Sem Lixo — Backend

API REST do projeto Mar Sem Lixo, em Spring Boot 3.3 / Java 21.

## Requisitos

- **Java 21** (LTS). O Maven Wrapper baixa o Maven 3.9.x sob demanda; não é necessário ter Maven instalado.
- **Docker** (apenas para construir a imagem).
- Internet na primeira execução, para o wrapper baixar Maven e o Maven baixar dependências.

## Rodando localmente

Rodar a aplicação no perfil `dev`:

```bash
./mvnw spring-boot:run
```

Sobe em `http://localhost:8080`. Endpoints úteis:

- `http://localhost:8080/actuator/health` — health check
- `http://localhost:8080/swagger-ui.html` — Swagger UI
- `http://localhost:8080/v3/api-docs` — OpenAPI JSON

Para alternar perfil:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Testes

Rodar a suíte completa:

```bash
./mvnw verify
```

Apenas testes unitários (sem integração):

```bash
./mvnw test
```

Um único teste:

```bash
./mvnw test -Dtest=MarSemLixoApplicationTests
./mvnw test -Dtest=MarSemLixoApplicationTests#contextLoads
```

## Build da imagem Docker

A partir de `backend/`:

```bash
docker build -t mar-sem-lixo-api:dev .
```

Rodar o container:

```bash
docker run --rm -p 8080:8080 mar-sem-lixo-api:dev
```

O Dockerfile é multi-stage com Spring Boot Layered JAR (ver ADR 0008): build em `eclipse-temurin:21-jdk-alpine`, runtime em `eclipse-temurin:21-jre-alpine` com usuário non-root e `HEALTHCHECK` apontando para `/actuator/health`.

## Estrutura de pacotes

Monolito modular (ADR 0003). Pacote raiz `com.marsemlixo.api`, com um subpacote por contexto de domínio:

```
com.marsemlixo.api
├── auth/       (controller, service, repository, domain)
├── area/
├── mutirao/
├── residuo/
└── relatorio/
```

Comunicação entre módulos deve passar por interfaces de service, nunca por chamadas HTTP internas.
