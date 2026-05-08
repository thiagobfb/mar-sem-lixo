# ADR 0004: PostgreSQL com PostGIS como banco de dados

## Status

Aceito (2026-05)

## Contexto

O sistema tem requisitos de persistência com características específicas:

- **Modelo relacional natural**: voluntários, mutirões, áreas e registros
  de resíduos têm relações 1:N e N:1 bem definidas, com integridade
  referencial relevante
- **Domínio fundamentalmente geoespacial**: todo registro de resíduo
  tem coordenadas (lat/long) e toda área é um polígono. Operações
  esperadas incluem "registros dentro deste polígono", "distância média
  entre pontos de coleta", "densidade de poluição em uma região",
  visualização em mapa
- **Relatórios agregados são o produto principal**: a saída final do
  sistema é um Excel consolidado por mutirão, área, tipo, período. SQL
  agregativo é essencial
- **Flexibilidade de schema seria útil em campos pontuais**: metadados
  específicos de algumas categorias de resíduo podem variar
- **Volume esperado é baixo**: dezenas de mutirões/mês, milhares de
  registros/mês. Performance de banco não é gargalo previsível

## Decisão

Usar **PostgreSQL 16+** com a extensão **PostGIS** habilitada como banco
de dados único do sistema, com:

- Hospedado em RDS PostgreSQL na AWS (instância t4g.micro inicialmente,
  free tier nos primeiros 12 meses)
- PostGIS para tipos `geometry`, `geography`, índices GIST e funções
  espaciais (`ST_Contains`, `ST_Distance`, `ST_Within`, etc.)
- JSONB com índices GIN para campos flexíveis quando necessário
- Migrations versionadas via Flyway
- Sem prefixo de schema em SQL (schema `public` padrão), evitando
  acoplamento a ambiente

## Alternativas consideradas

- **MySQL / MariaDB.** Banco relacional maduro e largamente usado.
  Rejeitada porque o suporte a tipos geoespaciais é significativamente
  menos rico que PostGIS, JSON tem indexação mais limitada que JSONB,
  e PostgreSQL se tornou o padrão de mercado para stacks novas em 2026.

- **MongoDB (document store).** Schema flexível e modelo de documento
  adequado para "registro de resíduo" como documento autocontido.
  Rejeitada porque os relatórios agregados são o produto, e SQL com
  JOINs e window functions é dramaticamente mais expressivo que
  aggregation pipelines para esse caso de uso. Modelo do domínio é
  fundamentalmente relacional (voluntário registra em mutirão em área).

- **DynamoDB.** Key-value gerenciado pela AWS. Rejeitada por
  incompatibilidade com queries agregadas e geoespaciais, lock-in com
  AWS, e ergonomia distante do modelo relacional do domínio.

- **SQLite.** Excelente para single-user embarcado. Rejeitada porque o
  sistema é multi-usuário com sincronização concorrente de voluntários
  em campo.

## Consequências

**Positivas:**
- PostGIS desbloqueia operações geoespaciais nativas em SQL,
  eliminando a necessidade de cálculos no application layer ou
  serviços externos
- JSONB com indexação GIN permite flexibilidade pontual de schema sem
  precisar de NoSQL separado
- Window functions, CTEs, LATERAL JOIN, full-text search nativo e
  outros recursos avançados de SQL simplificam relatórios agregados
- Tipagem estrita do PostgreSQL evita comportamentos surpreendentes
  comuns em outros bancos
- RDS PostgreSQL na AWS tem tooling maduro de backup, point-in-time
  recovery e monitoring
- Hospedagem gerenciada gratuita disponível em múltiplos provedores
  (RDS free tier, Neon, Supabase) caso seja necessário migrar
- Padrão atual da indústria para stacks novas, com base de talentos
  ampla

**Negativas:**
- Custo de RDS após 12 meses de free tier (~US$ 15/mês para t4g.micro)
- Pegada de memória maior que SQLite (irrelevante em servidor)
- Curva de aprendizado de PostGIS é real, mas documentação é boa

**Neutras:**
- Necessário planejar migrations cuidadosamente com Flyway desde o
  início; não é diferencial vs alternativas
