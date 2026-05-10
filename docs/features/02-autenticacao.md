# Feature 02: Autenticação

## Objetivo

Implementar o fluxo completo de autenticação Google OAuth + JWT próprio no
backend, proteger todos os endpoints com Spring Security e aplicar as regras
de autorização por papel (`VOLUNTARIO` / `COORDENADOR`). Esta feature substitui
o mecanismo provisório da Feature 01 (`app.auth.enabled` + `AuthGuardConfig`).

## Contexto

O ADR 0005 já define a estratégia de autenticação. Em síntese: o frontend usa
`@react-oauth/google` para obter um ID Token do Google e o envia uma única vez
ao backend; o backend valida a assinatura do Google, cria ou recupera o
`Voluntario`, e emite um **JWT próprio de curta duração** (access token) mais
um **token de renovação de longa duração** (refresh token). Sessões subsequentes
usam apenas os JWTs próprios — o Google não é consultado novamente até o logout.

Esta feature também resolve um débito de design deixado explícito na Feature 01:
a flag `app.auth.enabled=false` e a `AuthGuardConfig` de fail-fast existem
apenas para que a Feature 01 pudesse ser desenvolvida sem depender desta; ambas
são **removidas** quando esta feature entrar.

ADRs relevantes: `0005-google-oauth-com-jwt-proprio.md`.

## User Stories

- Como **voluntário**, quero entrar no app com minha conta Google sem criar
  senha, para que o acesso seja simples e seguro.
- Como **voluntário**, quero que minha sessão seja renovada automaticamente
  enquanto estou ativo, para não precisar logar a cada visita.
- Como **voluntário**, quero encerrar minha sessão explicitamente, para que
  outra pessoa não acesse minha conta no mesmo dispositivo.
- Como **coordenador**, quero que apenas coordenadores possam criar, editar
  e inativar áreas, para que voluntários não alterem dados inadvertidamente.
- Como **sistema**, quero rejeitar requisições sem token válido com `401`,
  e requisições de papel insuficiente com `403`, para garantir que a API
  nunca sirva dados sem autenticação adequada.

## Critérios de Aceitação

- [ ] **Login válido** — dado um ID Token legítimo do Google, quando
  `POST /api/auth/google`, então retorna `200` com `accessToken` (JWT),
  `expiresIn` (segundos), dados do voluntário, e define o cookie httpOnly
  de refresh token.
- [ ] **Criação automática** — dado um ID Token de um email nunca visto, o
  backend cria o `Voluntario` com role `VOLUNTARIO` e `dataCadastro = now()`.
- [ ] **Reauthenticação** — dado um ID Token de email já cadastrado, o backend
  atualiza `email` e `nome` se mudaram, e retorna o voluntário existente.
- [ ] **Renovação de access token** — dado um refresh token válido em cookie,
  quando `POST /api/auth/refresh`, então retorna novo `accessToken`.
- [ ] **Logout** — quando `POST /api/auth/logout`, então o cookie de refresh
  token é apagado e retorna `204 No Content`.
- [ ] **Rota protegida com token válido** — dado um access token JWT próprio
  válido no header `Authorization: Bearer ...`, quando qualquer endpoint
  protegido, então o backend processa normalmente.
- [ ] **Rota protegida sem token** — quando qualquer endpoint protegido sem
  header `Authorization`, então retorna `401 Unauthorized` com `problem+json`.
- [ ] **Rota protegida com token expirado** — quando access token expirado,
  então retorna `401 Unauthorized`.
- [ ] **Autorização de papel — coordenador** — dado token com role
  `COORDENADOR`, quando `POST /api/areas`, `PATCH /api/areas/{id}`,
  `DELETE /api/areas/{id}`, então a requisição é processada (`2xx`).
- [ ] **Autorização de papel — voluntário bloqueado** — dado token com role
  `VOLUNTARIO`, quando `POST /api/areas`, `PATCH` ou `DELETE`, então
  retorna `403 Forbidden`.
- [ ] **Voluntário acessa listagem** — dado token `VOLUNTARIO`, quando
  `GET /api/areas` ou `GET /api/areas/{id}`, então retorna `200`.
- [ ] **ID Token inválido** — dado ID Token com assinatura incorreta, expirado
  ou emitido para outro `client_id`, quando `POST /api/auth/google`, então
  retorna `401 Unauthorized`.
- [ ] **Refresh token inválido ou expirado** — quando `POST /api/auth/refresh`
  sem cookie ou com token inválido, então retorna `401`.

## Considerações Técnicas

### Entidade `Voluntario` e migration

**Migration `V3__create_voluntario.sql`:**

```sql
CREATE TABLE voluntario (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    google_id     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    nome          VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'VOLUNTARIO',
    data_cadastro TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_voluntario_google_id UNIQUE (google_id),
    CONSTRAINT uq_voluntario_email     UNIQUE (email)
);
```

**Entidade JPA** no pacote `com.marsemlixo.api.auth.domain`:

```java
@Entity
public class Voluntario {
    @Id @GeneratedValue UUID id;
    @Column(unique=true) String googleId;
    @Column(unique=true) String email;
    String nome;
    @Enumerated(EnumType.STRING) VoluntarioRole role;
    Instant dataCadastro;
}
```

`VoluntarioRole` é enum `{VOLUNTARIO, COORDENADOR}`.

**Invariantes implementadas no service:**
- `googleId` nunca é atualizado após criação
- `email` e `nome` são sincronizados a cada login bem-sucedido
- Promoção de role: fora de escopo desta feature — SQL direto no MVP

---

### Endpoints REST

| Método | Path                  | Auth required? | Descrição                                  |
|--------|-----------------------|----------------|--------------------------------------------|
| POST   | `/api/auth/google`    | Não            | Troca ID Token Google por tokens próprios  |
| POST   | `/api/auth/refresh`   | Não (cookie)   | Renova access token via refresh token      |
| POST   | `/api/auth/logout`    | Não (cookie)   | Invalida sessão e limpa cookie             |

**`POST /api/auth/google`**

Request body:
```json
{ "idToken": "<ID Token emitido pelo Google>" }
```

Response `200 OK`:
```json
{
  "accessToken": "<JWT>",
  "expiresIn": 900,
  "voluntario": {
    "id": "<uuid>",
    "nome": "João Silva",
    "email": "joao@gmail.com",
    "role": "VOLUNTARIO"
  }
}
```
+ `Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=2592000`

Erros: `401` (ID Token inválido), `502` (JWKS do Google indisponível).

**`POST /api/auth/refresh`**

Lê cookie `refresh_token`. Sem request body.

Response `200 OK`:
```json
{ "accessToken": "<novo JWT>", "expiresIn": 900 }
```
(+ `Set-Cookie` com novo refresh token se rotação ativa — ver Decisões Pendentes)

Erros: `401` (cookie ausente, token inválido ou expirado).

**`POST /api/auth/logout`**

Response `204 No Content`  
+ `Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0`

---

### Validação do Google ID Token

O Google publica suas chaves em `https://www.googleapis.com/oauth2/v3/certs`
(JWKS endpoint). Usar `nimbus-jose-jwt` com `RemoteJWKSet` (cache automático de
chaves com refresh quando necessário):

```java
// no AuthService
JWSVerifier verifier = new RSASSAVerifier(googleJwks.get(kid));
SignedJWT idToken = SignedJWT.parse(rawToken);
idToken.verify(verifier);
```

Validações obrigatórias além da assinatura:
- `iss` ∈ `{"accounts.google.com", "https://accounts.google.com"}`
- `aud` == `${APP_GOOGLE_CLIENT_ID}` (env var)
- `exp` no futuro
- `email_verified` == `true`

Claims extraídos: `sub` (→ `googleId`), `email`, `name`.

---

### JWT próprio — Access Token

- **Algoritmo:** HS256 (chave simétrica — adequado para monolito onde só o
  backend assina e verifica; RS256 seria overhead sem benefício real aqui)
- **TTL:** 15 minutos
- **Claims:**
  ```json
  { "sub": "<uuid do voluntario>", "email": "...", "role": "VOLUNTARIO", "iat": ..., "exp": ... }
  ```
- **Secret/chave:** via env var `APP_JWT_SECRET` (mínimo 32 bytes / 256 bits)
- **Fail-fast:** se `APP_JWT_SECRET` ausente ou curto demais, aplicação não sobe

---

### Refresh Token — Estratégia

**Stateful com rotação a cada uso.** Com TTL de 30 dias, stateless JWT seria um
risco relevante (token roubado = acesso por até 30 dias sem possibilidade de
revogação). A tabela é pequena e simples:

**Migration `V4__create_refresh_token.sql`:**

```sql
CREATE TABLE refresh_token (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    voluntario_id UUID        NOT NULL REFERENCES voluntario(id),
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    expira_em     TIMESTAMPTZ NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_token_voluntario ON refresh_token(voluntario_id);
```

**Rotação a cada uso:** cada `POST /api/auth/refresh` invalida o registro atual
(delete pelo hash) e insere um novo. Se o mesmo token for usado duas vezes
(sinal de roubo), a segunda chamada recebe `401`. Implementação: o service faz
`DELETE` + `INSERT` na mesma transação.

**Logout:** apaga o registro do banco pelo hash extraído do cookie + limpa o
cookie no cliente. Invalidação imediata — nenhuma janela de abuso residual.

---

### Spring Security — Configuração

Dependências a adicionar ao `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

(`nimbus-jose-jwt` vem transitivamente via `oauth2-resource-server`.)

**`SecurityFilterChain`** em `com.marsemlixo.api.config.SecurityConfig`:

```java
http
    .csrf(csrf -> csrf.disable())  // API stateless, JWT no header
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(POST, "/api/auth/google", "/api/auth/refresh", "/api/auth/logout").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
        .anyRequest().authenticated()
    )
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(appJwtDecoder())));
```

O `JwtDecoder` para HS256 não usa `issuer-uri` — bean customizado com
`NimbusJwtDecoder.withSecretKey(...)`.

**`JwtAuthenticationConverter`** extrai `role` do claim e cria um
`GrantedAuthority` (`ROLE_VOLUNTARIO` ou `ROLE_COORDENADOR`).

---

### Autorização por papel nos endpoints de Área

Ao entrar esta feature, os endpoints de Área ganham autorização explícita.
Opções:
- `@PreAuthorize("hasRole('COORDENADOR')")` nos métodos do controller
- Regras na `SecurityFilterChain` por padrão de URL e método

Escolhida anotação `@PreAuthorize` por ser mais legível e próxima do código:

| Endpoint                        | Acesso                       |
|---------------------------------|------------------------------|
| `POST /api/areas`               | `COORDENADOR`                |
| `GET /api/areas`                | `VOLUNTARIO`, `COORDENADOR`  |
| `GET /api/areas/{id}`           | `VOLUNTARIO`, `COORDENADOR`  |
| `PATCH /api/areas/{id}`         | `COORDENADOR`                |
| `DELETE /api/areas/{id}`        | `COORDENADOR`                |

Habilitar `@EnableMethodSecurity` na `SecurityConfig`.

---

### Remoção da infraestrutura provisória da Feature 01

Ao implementar esta feature:
- Apagar `AuthGuardConfig.java`
- Remover `app.auth.enabled` do `application.yml`
- Atualizar a spec `01-cadastro-de-area.md` (seção "Autorização") para refletir
  as regras reais
- O teste `MarSemLixoApplicationTests.contextLoads` deve continuar passando
  (Spring Security com todos os endpoints exigindo auth não quebra o boot —
  o contexto carrega normalmente, sem precisar de requests)

---

### Variáveis de ambiente

| Variável               | Obrigatória em prod | Descrição                                              |
|------------------------|---------------------|--------------------------------------------------------|
| `APP_GOOGLE_CLIENT_ID` | Sim                 | OAuth Client ID do Google Cloud Console                |
| `APP_GOOGLE_JWKS_URI`  | Não (default Google)| URI do JWKS para validar ID Token; sobrescrito em teste|
| `APP_JWT_SECRET`       | Sim                 | Chave simétrica HS256, ≥ 32 bytes                      |
| `APP_JWT_EXPIRY_S`     | Não (default 900)   | TTL do access token em segundos                        |
| `APP_REFRESH_EXPIRY_D` | Não (default 30)    | TTL do refresh token em dias                           |

---

### Testes

- **Unitários (`AuthServiceTest`):** mock de `RemoteJWKSet` / validador Google,
  mock de `VoluntarioRepository`. Cobrir: primeiro login (cria voluntário),
  reautenticação (atualiza campos), token inválido, `email_verified=false`.
- **Integração (`AuthControllerIT`):** Testcontainers + JWKS local de teste
  (par RSA gerado em `src/test/resources/test-jwks.json`; `RemoteJWKSet`
  apontado via property `APP_GOOGLE_JWKS_URI` sobrescrita no perfil de teste).
  Cobrir: fluxo completo de login → chamada protegida → refresh → logout.
  Verificar cookie setado/limpo. Verificar rotação: usar refresh token após
  `POST /refresh` → esperar `401`.
- **Testes de autorização:** requests a `POST /api/areas` com token VOLUNTARIO
  → `403`; com token COORDENADOR → `201`.
- **ArchUnit:** adicionar regra de que `SecurityConfig` não pode depender de
  pacotes de domínio exceto via interfaces de serviço.

---

### Estrutura no módulo `auth`

```
com.marsemlixo.api.auth
├── controller/
│   ├── AuthController.java
│   └── dto/
│       ├── GoogleLoginRequest.java
│       ├── TokenResponse.java
│       └── VoluntarioResponse.java
├── service/
│   ├── AuthService.java            (interface)
│   └── AuthServiceImpl.java
├── repository/
│   └── VoluntarioRepository.java
│   └── RefreshTokenRepository.java (se stateful)
└── domain/
    ├── Voluntario.java
    ├── VoluntarioRole.java
    └── RefreshToken.java           (se stateful)
```

`SecurityConfig` e `JwtConfig` ficam em `com.marsemlixo.api.config` (config
global da aplicação, fora do módulo de auth).

## Fora de Escopo

- **Gestão de papéis via UI** — promoção de VOLUNTARIO a COORDENADOR fica via
  SQL direto no banco no MVP.
- **Logout de todas as sessões** (invalidar todos os refresh tokens de um
  usuário) — útil para "mudei de dispositivo", mas adiciona complexidade de
  gestão de tokens.
- **Multi-device concorrente** — um voluntário com dois celulares abertos ao
  mesmo tempo; o comportamento com rotação de token pode surpreender se não
  houver família de tokens.
- **"Lembre de mim"** — TTL variável de refresh token por escolha do usuário.
- **Rate limiting** no endpoint de login — mitigação de abuso de tokens Google.
  Relevante em produção real; postergado para feature de observabilidade.
- **Frontend** — botão "Entrar com Google" e fluxo de renovação automática
  de token são features do frontend com spec dedicada.
- **Revogação de ID Token do Google** — Google suporta verificação de revogação
  via `https://oauth2.googleapis.com/tokeninfo`, mas o ID Token de curta duração
  já mitiga a janela de abuso; não implementado no MVP.

## Dependências

- **ADR 0005** — já aceito, define o fluxo completo.
- **Feature 01 (Cadastro de Área)** — implementada; `AreaController` precisará
  receber as anotações `@PreAuthorize` quando esta feature entrar.
- **Google Cloud Console** — OAuth 2.0 Client ID deve ser criado antes dos
  testes de integração com token real. Para testes automatizados, usar mock
  do JWKS.
- **Variáveis de ambiente** — `APP_GOOGLE_CLIENT_ID` e `APP_JWT_SECRET` devem
  estar configuradas no ambiente de CI para os testes de integração rodarem.

## Decisões Pendentes

Nenhuma pendência interna a esta feature — todas as cinco decisões abertas na
primeira versão da spec foram resolvidas e migradas para "Considerações
Técnicas":

| Decisão | Escolha |
|---------|---------|
| D1 — Algoritmo JWT próprio | **HS256** (simétrico, `APP_JWT_SECRET`) |
| D2 — Refresh token storage | **Stateful** (tabela `refresh_token`, hash do token) |
| D3 — Rotação de refresh token | **Com rotação** a cada `POST /api/auth/refresh` |
| D4 — Logout | **Apaga do banco** (invalidação imediata) + limpa cookie |
| D5 — Mock JWKS nos testes | **Par RSA local** em `src/test/resources/test-jwks.json` |
