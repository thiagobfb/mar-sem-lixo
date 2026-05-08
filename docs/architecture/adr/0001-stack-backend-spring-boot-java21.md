# ADR 0001: Stack backend: Spring Boot 3 com Java 21

## Status

Aceito (2026-05)

## Contexto

O projeto Mar Sem Lixo precisa de uma API REST que exponha funcionalidades
de gestão de mutirões, registros de resíduos, geração de relatórios em
Excel e sincronização de dados offline. O idealizador é um desenvolvedor
sênior Java com experiência substancial em Spring Boot e ecossistema
Java em produção.

O projeto serve simultaneamente a três objetivos:

1. Resolver o problema operacional da ONG (substituir caderno por app)
2. Demonstrar competência técnica atualizada em Java moderno (portfólio)
3. Demonstrar capacidade de trabalhar com tecnologias além do Java —
   esse objetivo é cumprido pelo frontend em TypeScript

A escolha do framework de backend afeta produtividade de desenvolvimento,
disponibilidade de bibliotecas (geração de Excel, integração com
PostgreSQL/PostGIS, OAuth/JWT), facilidade de hospedagem e legibilidade
do projeto para recrutadores técnicos que avaliam o portfólio.

## Decisão

Usar **Spring Boot 3.3+ com Java 21 (LTS)** como framework de backend,
com as seguintes escolhas adicionais:

- Spring Data JPA para persistência
- Spring Security com OAuth2 Resource Server para autenticação/autorização
- Springdoc OpenAPI para documentação automática
- Apache POI para geração de Excel
- Flyway para migrations versionadas
- Testcontainers + JUnit 5 + ArchUnit para testes
- Recursos modernos do Java 21: records para DTOs, sealed types para
  hierarquias fechadas, pattern matching em switch, virtual threads
  habilitadas via `spring.threads.virtual.enabled=true`

## Alternativas consideradas

- **FastAPI (Python).** Excelente produtividade para CRUDs, OpenAPI
  automático, validação via Pydantic. Rejeitada porque exigiria aprender
  Python a nível de produção, sem ganho de produtividade que justificasse
  abrir mão do domínio existente em Spring.

- **Node.js com NestJS.** Mesma linguagem do frontend, ecossistema vivo,
  sintaxe inspirada em Spring. Rejeitada porque dilui o objetivo de
  portfólio Java; TypeScript já é demonstrado no frontend, e adicionar
  mais TS no backend não amplia a narrativa de versatilidade.

- **Go (Gin/Fiber).** Performance superior, binários pequenos, modelo de
  concorrência elegante. Rejeitada por curva de aprendizado, ecossistema
  menor para geração de Excel e geoespacial, e por ser overkill para o
  volume esperado.

- **Quarkus / Micronaut.** Frameworks Java modernos com startup rápido
  e pegada de memória menor. Rejeitados porque a vantagem real está em
  cenários serverless (não usado aqui), e usar framework não dominado
  em vez de Spring bem feito sinaliza menos senioridade, não mais.

## Consequências

**Positivas:**
- Aproveita expertise existente do desenvolvedor, maximizando produtividade
- Java 21 com features modernas (records, sealed, pattern matching,
  virtual threads) demonstra atualização técnica em portfólio
- Ecossistema maduro: Spring Data, Spring Security, Spring Boot Actuator,
  Spring Boot Test
- Demanda contínua no mercado brasileiro (Java + Spring é stack
  dominante em vagas senior)
- Integração direta com PostgreSQL/PostGIS via JPA + Hibernate Spatial
- Geração de Excel via Apache POI é solução madura e robusta

**Negativas:**
- Pegada de memória maior que alternativas leves (~256MB mínimo na JVM)
- Startup de 3-5s no JVM tradicional (mitigável com virtual threads e,
  futuramente, GraalVM native image)
- Imagem Docker maior que alternativas (mitigado pelo ADR 0008)

**Neutras:**
- Standard de mercado para Java enterprise; nenhum diferencial isolado,
  mas nenhuma fricção
