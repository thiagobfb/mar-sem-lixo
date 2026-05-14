# Backlog do MVP

Backlog objetivo para concluir o MVP do Mar Sem Lixo com foco em entrega real
para o núcleo de Cabo Frio.

## Ordem de execução recomendada

1. **Feature 05 — Relatórios**
   Backend para visão consolidada por mutirão e exportação Excel para a
   coordenação. Fecha a promessa de consolidação operacional do MVP.

2. **Frontend PWA base**
   Scaffold React + TypeScript + Vite com autenticação Google, shell mobile,
   roteamento, setup de PWA e integração com a API já existente.

3. **Fluxo de campo offline-first**
   Tela de mutirões, formulário de registro de resíduo, persistência local em
   IndexedDB e sincronização tolerante a reconexão.

4. **Fluxo de coordenação no frontend**
   CRUD de áreas, gestão de mutirões, visualização de resumo consolidado e
   download de relatório Excel.

5. **Infraestrutura mínima**
   Provisionamento inicial do ambiente previsto na arquitetura: backend,
   PostgreSQL com PostGIS, storage de fotos e deploy do frontend.

6. **Validação operacional com a ONG**
   Confirmar o formato final do Excel, validar o fluxo em um mutirão piloto e
   ajustar UX e taxonomia de resíduos com base no uso real.

## Backlog por frente

### `docs`

- Especificar `docs/features/05-relatorio.md`.
- Escrever spec da PWA antes da implementação do frontend.
- Registrar decisão sobre edição de registro após sincronização.
- Registrar decisão sobre estratégia final de foto (`fotoUrl` / pré-signed URL).
- Validar e documentar o layout Excel aceito pela coordenação.

### `backend`

- Implementar módulo `relatorio`.
- Expor resumo consolidado por mutirão para `COORDENADOR`.
- Expor exportação Excel por mutirão concluído.
- Consolidar decisões pendentes de domínio que impactam API e frontend.
- Preparar endpoint de upload indireto de foto quando a estratégia for fechada.

### `frontend`

- Criar aplicação React + TypeScript + Vite.
- Implementar login Google e gestão de sessão JWT.
- Implementar navegação por perfil (`VOLUNTARIO` / `COORDENADOR`).
- Implementar armazenamento offline com IndexedDB.
- Implementar sync automático de registros pendentes.
- Implementar download de relatório Excel.

### `infra`

- Criar diretório `infra/` com CDK em Java.
- Provisionar banco PostgreSQL com PostGIS.
- Provisionar backend containerizado.
- Provisionar bucket para fotos e frontend estático.
- Definir pipeline mínima de build e deploy.

### `produto`

- Confirmar a taxonomia real de tipos de resíduo usada pela ONG.
- Confirmar se relatório institucional é sempre por mutirão ou também por período.
- Confirmar obrigatoriedade e política de retenção de fotos.
- Executar teste piloto com pelo menos um mutirão completo.
