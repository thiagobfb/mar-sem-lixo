# CLAUDE.md — Backend

Complementa o `CLAUDE.md` da raiz com convenções específicas do módulo backend. Em conflito, este aqui prevalece para arquivos sob `backend/`.

## Sempre usar o Maven Wrapper

Nunca chame `mvn` direto — sempre `./mvnw` (ou `mvnw.cmd` no Windows). O wrapper trava a versão do Maven e funciona em qualquer máquina com Java instalado, sem depender do `mvn` global do dev ou do CI.

## Java 21, mesmo se a JVM local for mais nova

`<java.version>21</java.version>` no `pom.xml` já força bytecode 21 via `--release 21`, mesmo rodando em JDK mais novo. Não suba esse target sem ADR — Java 21 é a versão usada em produção (imagem `eclipse-temurin:21-*-alpine` no Dockerfile).

## Organização modular (ADR 0003)

- Cada subpacote de domínio (`auth`, `area`, `mutirao`, `residuo`, `relatorio`) tem suas próprias camadas `controller / service / repository / domain`.
- Um módulo nunca chama o `controller` ou o `repository` de outro — só o `service` (via interface).
- Quando um módulo precisar de outro, prefira injetar a interface de service, não a implementação.
- ArchUnit vai enforçar essas regras quando entrar; até lá, a convenção é o contrato.

## Não adicionar agora

Estes blocos serão introduzidos em PRs próprios e têm ADR/decisão de design pendente — não antecipe sem alinhamento:

- Spring Security / autenticação JWT (ADR 0005)
- Datasource, JPA, Flyway, PostgreSQL/PostGIS (ADR 0004)
- Entidades JPA (a estrutura `domain/` está vazia de propósito)
- Controllers/endpoints REST de negócio
- Apache POI / geração de Excel
- Configuração de CORS, observabilidade, etc.

Se um PR atual exigir tocar em algum desses, levante a questão antes de implementar.

## Configuração

- Perfis em `application.yml` no mesmo arquivo (separados por `---`). Default é `dev`.
- Virtual threads habilitadas via `spring.threads.virtual.enabled=true` (ADR 0001).
- Actuator expõe apenas `health` e `info`. Não exponha mais endpoints sem decisão de segurança.
- Springdoc serve Swagger em `/swagger-ui.html` e o JSON em `/v3/api-docs`.

## Testes

- Smoke test (`MarSemLixoApplicationTests`) garante que o contexto sobe; mantenha-o passando — ele é o canário do esqueleto.
- Para testes que tocam banco: Testcontainers com PostgreSQL real, nunca H2 (CLAUDE.md raiz).
- Nome de classe de teste: `XxxTests` (plural) para alinhar com o padrão do Spring Initializr.

## Dockerfile

O Dockerfile segue exatamente o ADR 0008 (multi-stage, layered JAR, Alpine, non-root, HEALTHCHECK). Mudança nele exige atualizar o ADR ou abrir um novo. Não modifique sem essa rastreabilidade.

## Idioma

Código (classes, métodos, variáveis, paths REST) em inglês. Mensagens ao usuário, comentários de regra de negócio e documentação em português. Logs em inglês.
