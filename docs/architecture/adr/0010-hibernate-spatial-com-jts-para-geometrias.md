# ADR 0010: Mapeamento geométrico via Hibernate Spatial com JTS

## Status

Aceito (2026-05)

## Contexto

O ADR 0004 escolheu PostgreSQL com PostGIS, mas não decidiu como
JPA/Hibernate mapeia tipos geoespaciais para Java. A feature 01
(Cadastro de Área) precisa persistir `Polygon` e a futura feature de
Registro de Resíduo precisa persistir `Point`. Sem padrão, cada
módulo escolheria a sua representação, gerando inconsistência entre
DTOs, entities e queries.

Decisões em aberto:

- Qual API Java representa geometria nas entidades JPA
- Como a entidade vai e volta entre Postgres e Java
- Como GeoJSON (formato de input/output da API REST) converte para
  esses tipos Java
- Qual dialect Hibernate usar e o que precisa ser configurado
- Como funções espaciais (`ST_Contains`, `ST_IsValid`) ficam acessíveis

## Decisão

**Hibernate Spatial + JTS como API Java padrão de geometria.**

- Dependência: `hibernate-spatial` (já versionada pelo BOM do Spring
  Boot 3.3, sem necessidade de declarar versão explícita)
- Dialect: `org.hibernate.dialect.PostgreSQLDialect`. Spring Boot
  auto-configura ao detectar `hibernate-spatial` no classpath; nenhum
  dialect customizado
- Tipos Java em entidades JPA: `org.locationtech.jts.geom.Polygon` e
  `org.locationtech.jts.geom.Point` (e demais tipos JTS conforme
  necessário)
- DDL declara a coluna explicitamente com tipo e SRID nas migrations
  Flyway: `GEOMETRY(Polygon, 4326)`, `GEOMETRY(Point, 4326)`. Hibernate
  Spatial não emite o SRID automaticamente, então deixar isso fora do
  schema gerado pelo Hibernate (ADR 0009 já bloqueia `ddl-auto`)
- Conversão GeoJSON ↔ JTS no boundary HTTP: serializer/deserializer
  Jackson dedicados, registrados em uma `ObjectMapper` config de
  módulo. Implementação manual com base no `org.locationtech.jts`
  para evitar dependência em `jackson-datatype-jts` (pouco mantido)
- Validação topológica de polígonos via `ST_IsValid` no Postgres
  (decidido na spec da feature 01) — JTS no Java é usado para
  construção e desserialização, não como source of truth de validade
- Funções espaciais em repositories: preferir `@Query(nativeQuery=true)`
  com PostGIS direto em vez de funções JPQL Hibernate Spatial. Mais
  legível, mais alinhado com quem conhece SQL, e evita boilerplate de
  registrar funções customizadas em dialect

## Alternativas consideradas

- **JTS direto com `AttributeConverter` ou `UserType` customizado.**
  Manual sem Hibernate Spatial. Rejeitada porque Hibernate Spatial já
  fornece os converters bem testados de `org.locationtech.jts.*` para
  `geometry`; reimplementar é trabalho sem ganho e introduz risco de
  bug em parsing WKB.

- **Geometria como `String` (WKT) + `ST_GeomFromText` em queries.**
  Sem types geoespaciais no Java. Rejeitada por perder type-safety e
  forçar conversão manual em todo controller, service e teste — exato
  oposto do que JPA agrega.

- **GeoTools.** Stack completa de geomática Java. Rejeitada como
  overkill para o escopo (não há transformação de projeção, leitura
  de Shapefile, raster, etc.); seria adicionar uma dependência grande
  para usar 1% dela.

- **`jackson-datatype-jts` para conversão GeoJSON.** Considerada como
  forma de evitar serializer custom. Rejeitada porque a manutenção do
  pacote é irregular (releases esparsas, suporte a novas versões de
  Jackson em atraso) e o conversor manual é ~50 linhas — vale o
  controle.

## Consequências

**Positivas:**
- Type-safety completa nas entidades: `Polygon poligono` em vez de
  `String` ou `Object`. Compilador pega erros que SQL puro não pega
- Hibernate Spatial é mantido pela equipe Hibernate, alinhado com o
  ciclo de release do Spring Boot
- JTS é o standard de fato para geometria em Java; familiarização
  paga em outros projetos do ecossistema (GeoTools, Apache SIS, etc.)
- Testcontainers com PostGIS exercita exatamente o mesmo mapping que
  prod, eliminando classe de bug específica do binding
- Sem dependência de pacote terceiro pouco mantido para JSON

**Negativas:**
- JTS adiciona ~500 KB ao JAR final (negligível, mas notável)
- Construir um `Polygon` JTS manualmente em testes exige alguma
  cerimônia (`GeometryFactory.createPolygon(coordinates)`); helper de
  teste mitiga
- Conversor GeoJSON ↔ JTS é código de boundary que precisa ser
  mantido — pequeno (50–80 linhas), mas é mais um ponto a testar
- Funções espaciais em JPQL através de Hibernate Spatial exigem
  registro custom em dialect; ao optar por `nativeQuery`, ganho de
  legibilidade vem com perda de portabilidade entre bancos (não é
  preocupação dado ADR 0004 fixar Postgres)

**Neutras:**
- Versão do JTS é controlada pelo Hibernate Spatial, que por sua vez
  é controlado pelo BOM do Spring Boot. Atualização vem em rebote do
  upgrade de Spring Boot
- DDL de coluna geométrica fica explícito nas migrations Flyway,
  mantendo o schema source of truth fora do Hibernate
