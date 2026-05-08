# ADR 0002: Frontend como PWA em vez de React Native

## Status

Aceito (2026-05)

## Contexto

A aplicação será usada por voluntários em campo durante mutirões de
limpeza promovidos pela ONG Mar Sem Lixo. Características relevantes do
contexto de uso:

- **Voluntários rotativos**: cada mutirão atrai pessoas novas que
  precisam começar a usar o app em segundos, sem fricção de cadastro
- **Conectividade variável** em orla, mangue e restinga; offline-first
  é requisito de viabilidade, não opcional
- **Variedade de dispositivos**: Android e iOS, modelos diversos, sem
  controle sobre o parque
- **Orçamento de ONG**: zero custo de distribuição é restrição forte
- O projeto serve como portfólio de TypeScript/frontend moderno

A decisão entre PWA e app nativo (React Native, Flutter, native
Swift/Kotlin) afeta diretamente a curva de adoção dos voluntários, custo
operacional e o escopo de manutenção.

## Decisão

Construir o frontend como **Progressive Web App (PWA)** com a seguinte
stack:

- React + TypeScript + Vite
- Tailwind CSS + shadcn/ui
- TanStack Query (data fetching e cache)
- React Hook Form + Zod (formulários e validação)
- Dexie.js (IndexedDB para persistência offline)
- Workbox (service worker e estratégia offline-first)
- @react-oauth/google (autenticação)

Distribuição via "adicionar à tela inicial" no navegador móvel, sem
passar por app stores.

## Alternativas consideradas

- **React Native (com Expo).** Ecossistema rico, código TypeScript
  reutilizável, OTA updates via EAS. Rejeitada por: fricção de
  instalação via Play Store / App Store (incompatível com voluntário
  de ocasião), revisão da Apple para cada update significativo, custo
  de US$ 99/ano para conta Apple Developer, complexidade de builds
  nativos.

- **Flutter.** Performance e UI consistente entre plataformas, hot
  reload superior. Rejeitada porque dilui o objetivo de portfólio
  (Dart é menos comum em vagas no mercado brasileiro vs TypeScript) e
  não resolve a fricção de loja de apps.

- **Apps nativos (Swift + Kotlin).** Melhor performance e acesso
  completo a APIs nativas. Rejeitada por exigir duas codebases para
  manter, custo de desenvolvimento muito maior, sem benefícios reais
  para o caso de uso (não há demanda por gráficos intensos ou APIs
  exóticas).

- **Ionic / Capacitor.** Compromisso entre web e nativo. Rejeitada por
  performance inferior a alternativas e por adicionar camada sem
  benefício claro vs PWA puro.

## Consequências

**Positivas:**
- Zero fricção de instalação: voluntário acessa link e usa em segundos
- Updates instantâneos (próxima abertura sempre tem versão mais recente)
- Codebase único, deploy único
- Custo de distribuição zero (sem fees de loja, sem conta de developer)
- Demonstra competência em PWA moderno e offline-first (diferencial
  em portfólio frente a SPAs comuns)
- Service Worker + IndexedDB + background sync resolvem offline-first
  de forma robusta

**Negativas:**
- Limitações em iOS: push notifications nativos full não disponíveis
  (push web tem limitações), instalação menos intuitiva que no Android,
  storage de IndexedDB sujeito a eviction mais agressiva
- Acesso a APIs nativas restrito ao que os browsers expõem (Geolocation,
  câmera via `<input capture>`, IndexedDB — todas suficientes para o
  escopo)
- Performance gráfica intensa seria pior que nativo (não é o caso aqui)

**Neutras:**
- Geolocation API e captura de foto via `<input type="file" capture>`
  funcionam consistentemente em ambas plataformas
- PWAs não aparecem em buscas de loja de apps (irrelevante para
  distribuição direta da ONG)
