# ADR 0008: Dockerfile multi-stage com layered JAR

## Status

Aceito (2026-05)

## Contexto

O backend Spring Boot será implantado como container em ECS Fargate
(ADR 0006). O Dockerfile influencia diretamente:

- **Tamanho da imagem**: afeta tempo de pull em cada start de task no
  Fargate, custo de transferência de dados e tempo de cold start
- **Eficiência de cache**: builds frequentes em CI se beneficiam de
  reutilização agressiva de camadas Docker
- **Segurança**: superfície de ataque depende do que está na imagem
  final (build tools, shells, package managers)
- **Portfólio**: Dockerfile bem estruturado é frequentemente revisado
  por entrevistadores técnicos como sinal de competência DevOps

Um Dockerfile ingênuo (single-stage com JDK completo + fat JAR) gera
imagens de 500-700MB e quebra cache toda vez que o código muda,
trazendo resultado ruim em todas as dimensões acima.

## Decisão

Usar **Dockerfile multi-stage** com **Spring Boot Layered JAR**, base
JRE Alpine e usuário non-root, conforme:

```dockerfile
# === Build stage ===
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# === Runtime stage ===
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder --chown=spring:spring /build/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/target/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

Acompanhado de `.dockerignore` na raiz de `backend/` para evitar copiar
artefatos desnecessários no build context.

## Alternativas consideradas

- **Single-stage com JDK completo.** Rejeitada por gerar imagens 2-3x
  maiores que o necessário, incluir tooling de build em produção
  (superfície de ataque) e desperdiçar cache.

- **Imagem base Distroless (`gcr.io/distroless/java21`).** Considerada
  como alternativa mais segura que Alpine (sem shell, sem package
  manager). Não adotada no MVP porque dificulta debugging emergencial
  (sem `docker exec` interativo) e o ganho de tamanho/segurança é
  marginal sobre Alpine para este cenário. Pode ser revisitado em ADR
  posterior quando a operação estiver estável.

- **Cloud Native Buildpacks (`./mvnw spring-boot:build-image`).**
  Considerada por automatizar a construção de imagem otimizada sem
  Dockerfile manual. Rejeitada porque elimina a oportunidade de
  demonstrar conhecimento de Docker em portfólio, e por dar menos
  controle sobre detalhes da imagem final.

- **GraalVM Native Image (`spring-boot:build-image -Pnative`).**
  Considerada pelo ganho dramático em startup e tamanho. Rejeitada no
  MVP por: build de native image leva 10-15 minutos vs ~30 segundos
  do build tradicional, exige configuração de reflection hints para
  algumas bibliotecas (Spring Data JPA, Spring Security), e o ganho
  de cold start é menos relevante em ECS Fargate (que mantém tasks
  vivas) que seria em Lambda. Pode ser explorado em fase 2 como
  exercício, em branch separada.

- **JLink custom JRE.** Considerada para reduzir ainda mais a imagem.
  Rejeitada por ganho marginal sobre `jre-alpine` e por adicionar
  complexidade ao Dockerfile.

## Consequências

**Positivas:**
- Imagem final de ~250-280MB (vs 500-700MB de Dockerfile ingênuo)
- Cache de Docker layers eficiente: mudanças apenas em código
  invalidam só a camada `application/`, mantendo `dependencies/`
  intacta
- Builds incrementais em CI dramaticamente mais rápidos após o
  primeiro
- Push para ECR mais rápido (menos bytes)
- Pull no Fargate mais rápido (start de task acelerado)
- Usuário non-root no container reduz superfície de ataque
- HEALTHCHECK integrado permite ECS detectar tasks saudáveis sem
  configuração adicional
- Padrão moderno demonstra competência DevOps em portfólio

**Negativas:**
- Dockerfile mais longo e mais complexo que single-stage trivial
- Necessário entender Spring Boot Layered JAR para manter
- Alpine usa musl libc, o que ocasionalmente causa incompatibilidades
  em libs nativas (mitigável trocando para `eclipse-temurin:21-jre`
  baseado em Debian se aparecer problema)

**Neutras:**
- `.dockerignore` precisa ser mantido sincronizado com o que
  efetivamente entra no build context
- Healthcheck via `wget` exige `wget` na imagem (já vem no
  `eclipse-temurin:21-jre-alpine`); se trocar para distroless,
  precisaria estratégia diferente (ex: HEALTHCHECK via Java direto)
