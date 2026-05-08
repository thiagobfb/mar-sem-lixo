# ADR 0005: Autenticação via Google OAuth com JWT próprio

## Status

Aceito (2026-05)

## Contexto

A aplicação precisa autenticar voluntários e coordenadores com restrições
específicas do contexto:

- **Voluntários rotativos**: muitos usuários novos a cada mutirão. Cadastro
  com senha cria fricção alta e abandono
- **Maioria já tem Gmail**: presença quase universal de conta Google no
  público-alvo
- **Não queremos gerenciar senhas**: superfície de ataque, complexidade
  de "esqueci minha senha", responsabilidade de armazenamento seguro
- **Precisamos de autorização granular**: voluntário e coordenador têm
  permissões diferentes
- **Precisamos controlar tempo de sessão**: relatórios sensíveis não
  devem ficar acessíveis indefinidamente
- **PWA + API REST stateless**: padrão JWT é o caminho natural

A escolha de estratégia de autenticação afeta UX, segurança, complexidade
de implementação e manutenibilidade.

## Decisão

Implementar autenticação no padrão **OAuth 2.0 / OpenID Connect com
Google** no frontend, e **emissão de JWT próprio pelo backend** para
sessões subsequentes:

**Fluxo:**
1. Frontend exibe botão "Entrar com Google" (`@react-oauth/google`)
2. Google retorna ID Token (JWT assinado pelo Google) ao frontend
3. Frontend envia o ID Token uma vez ao endpoint `POST /api/auth/google`
4. Backend valida assinatura do Google via JWKS endpoint
5. Backend cria voluntário (no primeiro login) ou recupera existente
6. Backend emite seu próprio par de tokens:
   - **Access token**: JWT curto (~15 minutos), enviado via header
     `Authorization: Bearer ...`
   - **Refresh token**: JWT longo (~30 dias), armazenado em cookie
     `httpOnly`, `Secure`, `SameSite=Strict`
7. Renovação do access token via endpoint `POST /api/auth/refresh`
   usando o refresh token

**Claims customizados** no JWT próprio:
- `sub`: id do voluntário (UUID)
- `email`: email
- `role`: VOLUNTARIO ou COORDENADOR
- `iat`, `exp`: padrão

**Bibliotecas:**
- Backend: `spring-boot-starter-oauth2-resource-server`,
  `nimbus-jose-jwt`
- Frontend: `@react-oauth/google`

## Alternativas consideradas

- **Usar o ID Token do Google diretamente em todas as requisições.**
  Rejeitada porque: backend não controla o tempo de expiração, não
  pode adicionar claims customizados (role, etc.), revogação fica
  difícil, e a aplicação fica acoplada ao formato do Google
  indefinidamente.

- **Username/senha tradicional.** Rejeitada por toda a fricção descrita
  no contexto: cadastro, recuperação de senha, armazenamento seguro
  (bcrypt + salt), validações de força, e abandono de voluntários no
  primeiro contato com o app.

- **AWS Cognito.** Serviço gerenciado de identidade. Rejeitada porque
  adiciona complexidade significativa de configuração, lock-in com AWS,
  e não traz benefício educacional ou de portfólio claro vs implementar
  o fluxo padrão diretamente.

- **Auth0 / Clerk / Supabase Auth.** Plataformas SaaS de autenticação.
  Rejeitadas por terem custo recorrente em uso não trivial, lock-in,
  e por o objetivo de portfólio incluir demonstrar competência em
  OAuth + JWT implementado diretamente.

- **Magic links por email.** Considerada como alternativa simples.
  Rejeitada porque exige envio confiável de email (custo, complexidade)
  e o login com Google é tão simples quanto e mais rápido.

## Consequências

**Positivas:**
- UX mínima: voluntário entra com 2 cliques sem digitar nada
- Zero gerenciamento de senha pelo backend
- Controle total sobre expiração, claims e revogação via JWT próprio
- Refresh token em cookie httpOnly mitiga vetor de XSS (token não
  acessível via JavaScript)
- Demonstra competência em OAuth 2.0 + JWT no portfólio (tópico que
  muitos candidatos tropeçam em entrevista)
- Padrão amplamente usado em produção, fácil de evoluir

**Negativas:**
- Voluntário precisa ter conta Google (assumido true para a maioria)
- Backend precisa configurar dois validadores de JWT: um para o token
  do Google (no endpoint de login), outro para os tokens próprios (em
  todos os outros endpoints)
- Refresh token strategy adiciona código de manutenção de sessão
- Dependência da disponibilidade do Google para login (impacta apenas
  primeiro login e refresh; sessão ativa não é afetada)

**Neutras:**
- Necessário registrar OAuth client no Google Cloud Console
  (configuração one-time)
- Necessário documentar fluxo de autenticação para futuros
  contribuidores
