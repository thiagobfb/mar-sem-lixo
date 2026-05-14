# Feature 05: Relatórios de Mutirão

## Objetivo

Permitir que a coordenação visualize um resumo consolidado de um mutirão e
exporte um relatório em Excel com os registros de resíduos coletados, usando os
dados já persistidos nas features de autenticação, mutirão e registro.

## Contexto

O MVP promete reduzir drasticamente o tempo de consolidação pós-mutirão e gerar
uma saída utilizável pela coordenação sem retrabalho manual. Depois de
implementar áreas, autenticação, mutirões e registros de resíduos, falta a
camada que transforma esses dados em visão operacional e artefato de entrega.

No escopo desta feature, o relatório será centrado em **um mutirão por vez**.
Filtros cruzados por período, área e tipo de resíduo ficam como evolução
posterior, depois que o formato-base do relatório for validado com a ONG.

ADRs relevantes: `0003-monolito-em-vez-de-microservicos.md`,
`0004-postgresql-com-postgis.md`, `0008-multi-stage-dockerfile-com-layered-jar.md`.

## User Stories

- Como **coordenador**, quero visualizar um resumo consolidado de um mutirão,
  para conferir rapidamente volume coletado, distribuição por tipo e participação.
- Como **coordenador**, quero exportar um arquivo Excel do mutirão concluído,
  para usar o material nos relatórios institucionais da ONG.
- Como **sistema**, quero impedir exportação de relatório final de mutirão ainda
  não concluído, para evitar circulação de dados parciais como definitivos.

## Critérios de Aceitação

- [ ] **Resumo consolidado** — dado um mutirão existente, quando
  `GET /api/relatorios/mutiroes/{id}` por `COORDENADOR`, então retorna `200 OK`
  com dados do mutirão, totais consolidados e agrupamento por `tipoResiduo`.
- [ ] **Mutirão sem registros** — dado um mutirão existente sem registros de
  resíduo, quando `GET /api/relatorios/mutiroes/{id}`, então retorna `200 OK`
  com totais zerados e lista de agrupamentos vazia.
- [ ] **Exportação Excel válida** — dado um mutirão `CONCLUIDO` com registros,
  quando `GET /api/relatorios/mutiroes/{id}/excel`, então retorna `200 OK`,
  `Content-Type` de planilha Excel e um arquivo `.xlsx` com abas `Resumo` e
  `Registros`.
- [ ] **Exportação de mutirão concluído sem registros** — dado mutirão
  `CONCLUIDO` sem registros, quando exportar Excel, então o arquivo é gerado com
  resumo zerado e aba de registros apenas com cabeçalho.
- [ ] **Bloqueio de exportação prematura** — dado um mutirão `PLANEJADO`,
  `EM_ANDAMENTO` ou `CANCELADO`, quando `GET /api/relatorios/mutiroes/{id}/excel`,
  então retorna `409 Conflict`.
- [ ] **Erro: mutirão inexistente** — dado ID não cadastrado, quando consultar o
  resumo ou exportar Excel, então retorna `404 Not Found`.
- [ ] **Erro: papel insuficiente** — dado token `VOLUNTARIO`, quando consultar
  resumo ou exportar Excel, então retorna `403 Forbidden`.
- [ ] **Erro: sem autenticação** — quando consultar resumo ou exportar sem token
  válido, então retorna `401 Unauthorized`.

## Considerações Técnicas

### Estrutura no módulo `com.marsemlixo.api.relatorio`

| Camada       | Componentes                                              |
|--------------|-----------------------------------------------------------|
| `controller` | `RelatorioController`                                    |
| `service`    | `RelatorioService` + `RelatorioServiceImpl`              |
| `repository` | não obrigatório nesta primeira versão                    |
| `domain`     | sem entidade própria; usa `Mutirao` e `RegistroResiduo`  |

Nesta feature, o módulo `relatorio` pode consultar `MutiraoRepository` e
`RegistroResiduoRepository` em modo read-only, alinhado à regra do projeto para
consultas entre módulos.

### Endpoints REST

| Método | Path                               | Role exigida    | Ação                                |
|--------|------------------------------------|-----------------|-------------------------------------|
| GET    | `/api/relatorios/mutiroes/{id}`    | `COORDENADOR`   | Retorna resumo consolidado          |
| GET    | `/api/relatorios/mutiroes/{id}/excel` | `COORDENADOR` | Gera arquivo Excel do mutirão       |

### DTOs

**`RelatorioMutiraoResponse`**

```json
{
  "mutirao": {
    "id": 42,
    "titulo": "Mutirão Praia do Forte",
    "data": "2025-06-14",
    "status": "CONCLUIDO",
    "area": {
      "id": 10,
      "nome": "Praia do Forte",
      "tipo": "PRAIA",
      "municipio": "Cabo Frio",
      "estado": "RJ"
    }
  },
  "resumo": {
    "totalRegistros": 15,
    "totalItens": 42,
    "areaTotal": 123.40,
    "voluntariosDistintos": 6,
    "primeiroRegistroEm": "2025-06-14T10:00:00Z",
    "ultimoRegistroEm": "2025-06-14T11:15:00Z"
  },
  "totaisPorTipo": [
    {
      "tipoResiduo": "PLASTICO",
      "totalRegistros": 8,
      "totalItens": 25,
      "areaTotal": 70.20
    }
  ]
}
```

### Excel

O arquivo `.xlsx` terá duas abas:

- `Resumo`: metadados do mutirão e totais consolidados.
- `Registros`: uma linha por registro de resíduo, com colunas suficientes para
  auditoria operacional da coleta.

Biblioteca: Apache POI (`XSSFWorkbook`), já prevista na stack do projeto.

### Regras de domínio

- Apenas `COORDENADOR` pode acessar relatórios.
- Resumo consolidado pode ser consultado para qualquer mutirão existente.
- Exportação Excel só é permitida quando `mutirao.status == CONCLUIDO`.
- O relatório usa os dados persistidos; não recalcula nem corrige registros.
- Ordenação da aba `Registros`: `dataRegistro ASC`.

## Testes

- Unitários do service cobrindo resumo com e sem registros.
- Unitários do service cobrindo bloqueio de exportação fora de `CONCLUIDO`.
- Integração do controller cobrindo `200`, `401`, `403`, `404` e `409`.
- Teste de exportação validando tipo de conteúdo e estrutura básica do arquivo.

## Fora de Escopo

- Filtros cruzados por período, área e tipo fora do contexto de um mutirão.
- Geração automática de PDF.
- Envio agendado por e-mail ou WhatsApp.
- Dashboard analítico com séries históricas e mapas de calor.
- Customização avançada do layout do Excel conforme template visual da ONG.

## Dependências

- Feature 02: autenticação e autorização por papel.
- Feature 03: mutirão com status consistente.
- Feature 04: registros de resíduo persistidos e ordenáveis por data.
- Validação posterior com a coordenação sobre o formato final do Excel.

## Riscos e Mitigações

- **Risco:** o layout inicial do Excel não bater com o formato operacional da ONG.
  **Mitigação:** separar claramente a geração da planilha em serviço dedicado e
  manter a primeira versão simples, fácil de ajustar.

- **Risco:** relatório crescer de forma desnecessária em complexidade antes da
  validação do uso real.
  **Mitigação:** limitar a primeira versão a um mutirão por exportação e a um
  resumo consolidado enxuto.

## Decisões Pendentes

- Se o Excel institucional deve ter apenas aba tabular ou também capa/resumo
  visual com formatação mais forte.
- Se haverá endpoint futuro de relatórios por período e por área no backend ou
  se isso será composição no frontend a partir de múltiplos mutirões.
