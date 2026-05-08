# Vision: Mar Sem Lixo App

## Contexto

A ONG **Mar Sem Lixo**, sediada em Cabo Frio (RJ), atua desde 2021 com
mutirões de limpeza, educação ambiental e identificação/controle de lixo
marinho em praias, lagoas e mangues, em parceria com comunidades locais,
escolas, prefeituras, Ministério Público Federal (MPF), ICMBio e colônias
de pescadores.

Origem na Região dos Lagos, com atuação documentada em mais de 150 praias
do litoral brasileiro (BA, SC, PR, SP, ES, PE, além do RJ) durante a
Semana Nacional de Limpeza dos Mares. Em 2021, ações da ONG retiraram mais
de 28 toneladas de lixo do litoral.

Parte essencial da operação consiste em **contabilizar e triar o material
recolhido durante os mutirões, gerando dados quantitativos e qualitativos
sobre os resíduos encontrados**. Esses dados alimentam relatórios entregues
a parceiros institucionais, incluindo o MPF.

## Problema

Hoje, o registro de resíduos coletados é feito manualmente — tipicamente
em caderno por voluntários durante o mutirão e depois consolidado pela
equipe responsável pela triagem. Esse fluxo apresenta limitações:

- **Consolidação lenta**: dados em papel precisam ser digitados depois,
  somando horas após cada mutirão
- **Risco de perda**: cadernos molhados, ilegíveis ou esquecidos em campo
- **Falta de georreferenciamento**: o "onde" do resíduo se perde,
  empobrecendo análise de hotspots de poluição
- **Dificuldade de fotografar evidência**: registro fotográfico fica
  dissociado do registro tabular
- **Geração manual de relatórios**: consolidação para entrega a MPF e
  outros parceiros consome tempo da coordenação

## Proposta

PWA + API REST que permite a voluntários registrarem em campo, via
celular próprio e mesmo sem conexão, cada coleta de resíduo com tipo,
medidas, quantidade, geolocalização e foto opcional, sincronizando com
um servidor central que consolida e gera relatórios em Excel para a
coordenação.

A entrega do MVP é direcionada inicialmente ao núcleo de Cabo Frio,
podendo escalar para outros núcleos da ONG conforme adoção.

## Personas

**Voluntário em campo**
- Participa de mutirões eventuais, frequência irregular
- Pode ser voluntário recorrente ou estudante em ação pontual
- Usa celular próprio, conectividade variável (orla, mangue, restinga)
- Baixa fricção é requisito: precisa abrir e usar em segundos

**Coordenação local (Cabo Frio)**
- Organiza mutirões, define áreas, faz a triagem do material
- Consolida dados para relatórios institucionais
- Precisa de visão agregada por mutirão, área, tipo de resíduo, período
- Exporta relatórios em formato compatível com o que já entrega ao MPF

## Escopo do MVP

- Autenticação simples via Google (sem cadastro com senha)
- Cadastro de áreas de atuação (orla, lagoa, mangue) com polígonos geográficos
- Criação de mutirões (data, local, área, voluntários esperados)
- Registro de resíduo durante mutirão: tipo, medidas, quantidade,
  geolocalização automática, foto opcional
- Funcionamento offline-first com sincronização automática ao reconectar
- Visão consolidada para coordenação: filtros por mutirão, área, período,
  tipo de resíduo
- Exportação para Excel no formato utilizado pela ONG nos relatórios
  atuais (a ser confirmado com a coordenação)

## Fora de escopo no MVP

- Painel administrativo multi-núcleo (Cabo Frio em foco; outros núcleos
  da ONG podem aderir em fase posterior)
- Gamificação, ranking de voluntários, redes sociais
- Integração direta com sistemas do MPF ou outros órgãos
- App nativo iOS/Android (PWA atende e elimina fricção de loja)
- Relatórios automáticos por email ou agendados
- Análise estatística avançada e mapas de calor (dados ficam disponíveis;
  visualização avançada fica para fase 2)

## Métricas de sucesso

- Adoção pelo núcleo de Cabo Frio em pelo menos um mutirão completo no
  primeiro trimestre após entrega
- Tempo médio de consolidação pós-mutirão reduzido em pelo menos 70%
  comparado ao fluxo manual atual
- Pelo menos 80% dos resíduos registrados em campo durante mutirões com
  uso do app passam a ter geolocalização associada (vs. nenhum no fluxo
  atual)
- Relatório gerado pelo app aceito pela coordenação como insumo para
  entrega institucional sem necessidade de retrabalho

## Restrições e premissas

- Projeto é uma **contribuição voluntária** ao núcleo de Cabo Frio.
  Adoção depende de aceitação pela coordenação local após avaliação.
- Custo de operação deve ser zero ou próximo a zero. Stack escolhida
  prioriza free tiers que sustentam o volume estimado de uso.
- Implementação não pressupõe acesso a bases de dados históricas da ONG.
  Sistema parte do zero a partir do primeiro mutirão registrado.
- Formato do relatório de saída deve ser validado contra o que a ONG
  já entrega hoje (Excel/PDF de triagem).

## Status

MVP em desenvolvimento.

Repositório: [Mar Sem Lixo](https://github.com/thiagobfb/mar-sem-lixo)