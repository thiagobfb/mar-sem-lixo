# ADR 0003: Arquitetura monolito em vez de microsserviços

## Status

Aceito (2026-05)

## Contexto

A discussão de arquitetura precisa balancear:

- **Escala real**: o domínio é simples (CRUD com geração de relatório),
  o volume esperado é baixo (estimativa: dezenas de mutirões/mês,
  milhares de registros/mês), os usuários são poucos (voluntários do
  núcleo de Cabo Frio)
- **Equipe**: solo developer no MVP
- **Operação**: a aplicação precisa ter custo operacional baixo e
  manutenção mínima após entregue à ONG
- **Portfólio**: arquitetura visivelmente sobreengenharizada é vista
  por entrevistadores experientes como sinal de imaturidade técnica,
  não de senioridade

Microsserviços trazem complexidade real (orquestração, observabilidade
distribuída, latência de rede, complexidade de transações distribuídas,
deploy coordenado, infraestrutura mais cara) em troca de benefícios que
só se materializam em escala (deploys independentes, escala horizontal
seletiva, isolamento de falhas).

Para um sistema deste porte, microsserviços representariam custo sem
contraparte.

## Decisão

A aplicação backend será uma **única aplicação Spring Boot (monolito)**,
com organização interna modular para facilitar manutenibilidade e
testabilidade.

- Pacotes por contexto de domínio (`auth`, `mutirao`, `residuo`, `area`,
  `relatorio`)
- Camadas explícitas dentro de cada módulo: `controller`, `service`,
  `repository`, `domain`
- Comunicação entre módulos via interfaces de service, não por endpoints
  HTTP internos
- Banco de dados único, com schema único
- ArchUnit para enforçar regras de dependência entre pacotes

Se em algum momento futuro um módulo precisar escalar de forma
independente ou ser substituído por serviço externo, a fronteira clara
entre módulos facilita a extração — pattern conhecido como "modular
monolith" ou "majestic monolith".

## Alternativas consideradas

- **Microsserviços (auth-service, mutirao-service, etc.).** Rejeitada
  por todos os motivos do contexto: complexidade desproporcional ao
  problema, custo operacional alto (múltiplos containers, ALB, service
  discovery), latência adicional, debugging distribuído, e —
  criticamente — sinaliza falta de juízo arquitetural em portfólio.

- **Serverless (Lambda) por função.** Rejeitada por incompatibilidade
  prática com Spring Boot tradicional (cold start de JVM), pelo padrão
  de carreira Java não ser orientado a serverless, e por adicionar
  complexidade de orquestração sem benefício para o volume esperado.

- **Modular monolith desde o dia 1 com ferramenta dedicada (ex:
  Spring Modulith).** Considerada interessante; rejeitada como
  obrigatória no MVP por adicionar dependência sem necessidade
  imediata. A organização modular com convenções e ArchUnit cumpre o
  papel para o escopo atual; Spring Modulith pode ser introduzido em
  ADR posterior se a complexidade crescer.

## Consequências

**Positivas:**
- Deploy mais simples: uma imagem, um container, um endpoint
- Debug local trivial: rodar uma aplicação reproduz todo o sistema
- Banco de dados único permite transações ACID sobre múltiplos contextos
  sem two-phase commit ou sagas
- Custo de infraestrutura baixo (uma task Fargate em vez de múltiplas)
- Coerência com a tendência atual da indústria de revalorizar monolitos
  bem feitos para projetos de pequeno e médio porte (DHH/37signals,
  Shopify "majestic monolith", etc.)
- Para entrevista técnica, justificativa clara de escolha demonstra
  maturidade arquitetural

**Negativas:**
- Não é possível escalar partes do sistema de forma independente (não é
  necessário no escopo)
- Falha em qualquer módulo derruba a aplicação inteira (mitigado por
  health checks e auto-restart do ECS Fargate)
- Limite de escala vertical eventualmente seria atingido (não é
  preocupação para o volume previsto)

**Neutras:**
- Refatoração futura para microsserviços é possível dado o cuidado com
  fronteiras modulares; não é o objetivo, mas a porta fica aberta
