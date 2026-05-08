# Glossário — Mar Sem Lixo

Vocabulário compartilhado do projeto. Termos definidos aqui são usados de forma
consistente em código, banco de dados, API, UI e documentação.

> **Nota:** alguns termos refletem o esboço inicial do data model e ainda
> precisam de validação contra a metodologia da ONG. Itens marcados com
> `⚠ a confirmar` são hipóteses de trabalho.

## Termos do domínio

**Voluntário**
Pessoa que registra resíduos durante mutirões. Autentica-se via Google e usa
o app em campo no próprio celular. Pode ser recorrente ou participar
eventualmente.

**Coordenação / Coordenador**
Papel de quem organiza mutirões, gerencia áreas, faz a triagem do material
e gera relatórios institucionais. No MVP, coordenação atua sobre o núcleo
de Cabo Frio.

**Núcleo**
Subgrupo regional da ONG. O MVP foca no núcleo de Cabo Frio, com
possibilidade de outros núcleos aderirem em fases futuras.

**Mutirão**
Evento de coleta de resíduos com data, hora, local definido (área) e
voluntários participantes. É o contexto sob o qual registros de resíduo
são feitos. Possui ciclo de vida (planejado, em andamento, concluído,
cancelado).

**Área**
Região geográfica onde mutirões acontecem, delimitada por polígono
geográfico. Tipos: praia, lagoa, mangue, rio. Exemplos: "Praia do Forte
(Cabo Frio)", "Canal de Itajuru".

**Registro de Resíduo**
Unidade de dado capturada em campo durante um mutirão. Descreve um
agrupamento de resíduos do mesmo tipo, com dimensões, quantidade,
geolocalização e foto opcional.

**Tipo de Resíduo** ⚠ a confirmar
Categoria de classificação do resíduo encontrado. No MVP, com base no
esboço inicial: Entulho, Madeira, Plástico, Metal, Orgânico. Esta lista
provavelmente vai evoluir para uma taxonomia mais granular (ex: PET,
vidro, alumínio, isopor, redes de pesca, têxtil) conforme alinhamento com
a metodologia de triagem da ONG.

**Metragem Perpendicular** ⚠ a confirmar
Medida em metros da extensão do resíduo (ou do conjunto de resíduos do
mesmo tipo) na direção perpendicular à linha d'água ou à orla. Capturada
pelo voluntário em campo. Semântica precisa ser validada contra a
metodologia oficial da ONG — possível que eles usem peso (kg) ou contagem
de itens em vez de área estimada.

**Metragem Transversal** ⚠ a confirmar
Medida em metros da extensão do resíduo na direção transversal (paralela
à orla). Mesma observação da metragem perpendicular: validar contra
metodologia da ONG.

**Área Total** ⚠ a confirmar
Cálculo derivado: metragem perpendicular × metragem transversal ×
quantidade. Usado como aproximação da área impactada pelo conjunto de
resíduos. Sujeito a redefinição quando o relatório oficial for analisado.

**Triagem**
Atividade pós-mutirão de classificação, contagem e quantificação do
material recolhido para fins de relatório. No MVP, a triagem em si
permanece com a coordenação, mas o app reduz o trabalho ao já capturar
dados estruturados em campo.

**Relatório de Triagem**
Documento consolidado entregue pela coordenação a parceiros
institucionais (MPF, prefeituras, patrocinadores) com os dados
quantitativos e qualitativos do mutirão. O app gera versão em Excel
desse relatório.

## Termos técnicos

**Sincronização**
Processo pelo qual registros feitos offline no celular do voluntário são
enviados ao servidor central quando há conectividade. Estratégia
offline-first com IndexedDB local + sync em lote.

**Geolocalização**
Coordenadas (latitude e longitude) capturadas automaticamente pelo
dispositivo no momento do registro de resíduo, via Geolocation API do
navegador. Armazenadas no banco como PostGIS Point.

**Polígono Geográfico**
Representação espacial de uma Área no formato PostGIS Polygon (SRID 4326,
WGS84). Permite operações como "registros dentro desta área", cálculo de
densidade e geração de mapas.

**ID Token**
JWT assinado pelo Google retornado após login OAuth. Validado pelo
backend uma vez para identificar o voluntário; depois disso, o backend
emite seu próprio JWT para uso nas requisições subsequentes.
