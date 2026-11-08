# HireBuddy Backend – Job Portal (India)

Backend only. Full idea, rules, flows: `Job_Portal_Project_Reference.md` (read first).

## Learning mode (most important)
The user is building this to LEARN. Restarted from zero on 2026-11-03 because the first build went too fast to follow.
- One tiny piece per turn (one file, or a few lines). Never several files at once. EXCEPTION (user, 2026-11-07): when user says "pura karo ek hi baar me", build the whole feature in one go and explain VERY short (only how to use + key points).
- Before writing: explain what it is and why, in simple words.
- After writing: explain every line / annotation. Then ask a quick check question or let the user ask.
- Wait for the user's OK before the next piece. A general "go on" is approval for ONE piece only.
- Introduce a concept (Maven, Spring Boot, JPA, Flyway, JWT…) the first time it appears, briefly.
- No scope creep; ask first. No Docker/CI/deploy.
- BEFORE any change (user, 2026-11-08): show a short plan (files to add/change/delete + why) and "Effect on project/idea", then wait for OK. Never change the Reference idea/features as a side effect.
- Update Progress after every step. Keep this file short.
- Less testing, more building (user, 2026-11-04: heavy testing kills interest). One build check per piece; a short test only when an endpoint is done. Every new endpoint → add a request (success + key bad cases, small `pm.test`, expected reply in description) to `backend/postman/collections/HireBuddy-API.postman_collection.json` under its service folder; vars are COLLECTION variables (no environment file; user couldn't select env in Postman, chose this). No secrets in it. How-to: `postman/README.md`.
- Git: user commits + pushes THEMSELVES, when THEY want (2026-11-08: stop adding git commands/reminders to replies). Never run `git commit`/`git push`. Read-only git is fine.
- "Advanced" project: use modern/pro choices (Boot 4 way, not old tutorials) and say why.

### Explanation style (user liked this — keep it every session)
VERY simple words: short sentences, no unexplained jargon, plain meaning of each line first. AND detailed — but ONLY on must-know points + what interviews ask (user, 2026-11-07). Skip side trivia; explain key lines, not every import. Add a "🎤 Interview" box (likely questions + short answers). Build only what's needed, no extras. User often writes Hinglish; answering in simple Hinglish is welcome when they ask in Hindi. Say "random value (salt)" for salt; avoid food analogies for crypto. Each step's reply:
1. Title "Part N: <what>" + one line on what changed and build result (BUILD SUCCESS ✅).
2. New ideas first, each with a simple real-life analogy.
3. Code shown block by block, each line explained; small tables for fields/options.
4. Small ASCII diagrams for flows/structure (folder tree, request journey).
5. Quick proof it works (build / app start / tiny scratchpad demo — never in project).
6. End: "✅ Quick check" (2–3 questions) + "Next" preview + "Say OK". (No git commands.)

## Stack
Spring Boot 4.0.9, Spring Cloud 2025.1.4, Java 21, Maven multi-module, MySQL 8 (Workbench).
Auth: own `auth-service` (Spring Security + JWT RS256), no Keycloak.
Later (nothing may require them to start yet): RabbitMQ, Redis, MinIO.
Boot 4 notes: test starters per tech (`spring-boot-starter-webmvc-test`); Flyway needs `spring-boot-starter-flyway` (+ `flyway-mysql`); `@WebMvcTest` is `org.springframework.boot.webmvc.test.autoconfigure` and skips `@Configuration` classes (tests `@Import(AppConfig.class)`); JUnit 6.

## Conventions
- groupId `com.jobportal`; package by feature `com.jobportal.<service>.<feature>`.
- One DB per service; no cross-DB access or cross-service imports. Talk via HTTP/events.
- DB created by hand once (utf8mb4); tables only via Flyway; `ddl-auto=validate`; never edit a run migration (add V2…).
- Creds: `backend/.env` (gitignored; template `backend/.env.example` committed). Each service yml imports `optional:file:./.env[.properties]` + `../.env[.properties]`. OS env vars win if set. Don't set new Windows env vars. Names: `DB_USER`, `DB_PASSWORD`, later `INTERNAL_API_KEY`, `ADMIN_EMAIL`/`ADMIN_PASSWORD`, JWT key paths.
- Versions only in parent `pom.xml` (BOMs). Plugins need explicit `${spring-boot.version}`.
- Own parent pom (not spring-boot-starter-parent) so every setting is visible.
- Inject `Clock`, never `Instant.now()`. Every service defines a `Clock` bean (in `config/`).
- IDs: UUID `CHAR(36)` (`@JdbcTypeCode(SqlTypes.CHAR)`); timestamps UTC `DATETIME(6)` ↔ `Instant`; enums `@Enumerated(STRING)`.
- No Lombok; entities: protected no-arg ctor, public ctor, getters only (behaviour methods later). Request DTOs = records with validation.
- Depend on interfaces (`PasswordEncoder`, `Clock`), not implementations.

## Folder structure (user asked to keep it strict)
Follow Reference §7 feature folders. auth-service `com.jobportal.auth`:
- `AuthServiceApplication` (main only) · `config/AppConfig` (Clock, PasswordEncoder, `@Import(GlobalExceptionHandler)`)
- `user/` User, UserStatus, UserRepository, SignupService, SignupController, MeController, `dto/`
- `token/` login/refresh/logout + JWT making · `verification/` email verify + reset · `invite/` recruiter invites
- `config/SecurityConfig` (filter chain, JwtEncoder, JwtDecoder). Tests mirror main packages in `src/test/java`. SQL in `resources/db/migration/`.
gateway `com.jobportal.gateway`: `GatewayApplication` · `config/AppConfig` (Clock) · `config/SecurityConfig`. shared-lib: `error/`, `security/` (Role, SecurityErrorHandler).
Create a folder only when its first file comes. Write files UTF-8 without BOM (PowerShell 5 adds BOM → javac "illegal character \ufeff").

## Services (planned: port · DB)
gateway 8080 · auth 8081 `auth_db` · core 8082 `core_db` · application 8083 `application_db` · payment 8084 `payment_db` · file 8085 `file_db` · notification 8086 `notification_db`

## Environment
- Maven: `C:\Users\Rakesh\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd`. Build all: `mvn install` in `backend/`.
- Java 21.0.9 on PATH (`jar` tool is not). Demos in scratchpad; BCrypt demo classpath = spring-security-crypto 7.1.1 + commons-logging + spring-core 7.0.10 jars from `.m2`.
- Run checks on spare port: `java -jar auth-service\target\...jar --server.port=18081`, then kill that java (else jar stays locked → "Unable to rename … .jar.original").
- MySQL 8: service `MySQL80`, localhost:3306, user `root`; password in `backend/.env`. Client: `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`. DB `auth_db` exists (Flyway v2).
- Gateway test on spare ports: auth `--server.port=18081` + gateway `--server.port=18080` with FULL route override (`routes[0].id/uri/predicates[0]/filters[0]`; overriding only `uri` drops the list → "Route id is required").
- Old Windows user env vars REMOVED 2026-11-08 (DB_USER, DB_PASSWORD, INTERNAL_API_KEY, ADMIN_EMAIL, ADMIN_PASSWORD). Everything now lives only in `backend/.env` (INTERNAL_API_KEY copied there first).
- JWT RSA keys (from old build): `C:\Users\Rakesh\.hirebuddy\keys\` (private.pem, public.pem).
- DB `audit_management` is NOT ours; never touch it.
- Git: repo root `hirebuddy/` (root `.gitignore`), remote github.com/AcquantHR-02/Hirebuddy, branch `feat/auth`.
- IntelliJ may have an old auth-service running on 8081 → user restarts it there.

## Progress
**Session 1 (2026-11-03):** old code + DBs deleted. Parent `pom.xml` (coordinates, properties, Boot+Cloud BOMs, pluginManagement boot plugin, modules). `shared-lib`: `security/Role` enum, `error/ApiError` record, `error/ApiException`, `error/GlobalExceptionHandler` (@RestControllerAdvice, Clock injected); webmvc dep `optional`.

**Session 2 (2026-11-03):** auth-service: pom (webmvc + shared-lib), `AuthServiceApplication`, Clock bean, `@Import` handler, `application.yml` (name, port 8081), TEMP `demo/DemoController` (`/auth/demo/ok`, `/auth/demo/fail`), `postman/auth-service.md` + `postman/README.md`.

**Session 3 (2026-11-04):**
- 5 `.gitignore` (target/, .idea/, *.iml, .vscode/, OS junk, *.log, .env, *.pem). User set up git + GitHub themselves (moved it to repo root).
- 4c-1 test dep `spring-boot-starter-webmvc-test` (scope test). 4c-2 `DemoControllerTest` (@WebMvcTest + MockMvcTester + @Primary fixed Clock).
- 6a/6b (TDD, issue F part 1): handler `handleNoEndpoint` for `NoResourceFoundException` → 404 ApiError "No endpoint <METHOD> <path>". 3/3 tests green.
- 7 Created `auth_db` by hand (`mysql.exe`); deps `spring-boot-starter-data-jpa` + `mysql-connector-j` (runtime); yml datasource, `ddl-auto: validate`, `open-in-view: false`, Hibernate time_zone UTC. 7b creds moved to `backend/.env` + `.env.example`.
- 8 Flyway (`spring-boot-starter-flyway` + `flyway-mysql` 11.14.1); `db/migration/V1__create_users.sql` (users: id, email UNIQUE, password_hash, role, status + CHECKs, company_id NULL + index, failed_login_count, locked_until, created_at, updated_at). Ran → tables `users`, `flyway_schema_history`. Explained adding columns later (NULL/DEFAULT, add→backfill→enforce).
- 9 `user/UserStatus` enum + `user/User` @Entity (validate passes).
- 10 `user/UserRepository` (`existsByEmail`, `findByEmail` → Optional).
- 11a dep `spring-boot-starter-validation`; `user/dto/SeekerSignupRequest` record (email @NotBlank @Email ≤254; password 8–72 (BCrypt 72-byte limit); consent @NotNull @AssertTrue). Consent not stored yet.
- Folder structure fixed (DTO moved `signup/` → `user/dto/`).
- 11b dep `spring-security-crypto`; `config/AppConfig` (handler import, Clock, `BCryptPasswordEncoder(12)`); main class cleaned; test `@Import(AppConfig.class)`.
- Explained in depth: interface vs implementation (PasswordEncoder/BCrypt), encode vs matches, login verify flow, BCrypt hash structure (`$2a$` version, cost `12`, 22-char random value, 31-char result = 60), demo: "1100" vs hash of "secret123" → false.
Build: BUILD SUCCESS, 3/3 tests. Session ended here; user said "we will understand tomorrow".

**Session 4 (2026-11-05):**
- 11c `SignupService` written, undone at user's request, then REDONE with detailed explanation (Service layer, DI, @Transactional, normalize email, existsByEmail, BCrypt, Clock, saveAndFlush + race → 409). Also explained: signup ≠ login (one `/auth/login` for all; admin made by seeder from `.env`, no admin signup; role set by server, never by request), why `SeekerSignupRequest` not `SignupRequest` (one endpoint = one DTO). Build OK.
- 11d-1 `user/dto/SignupResponse` record (id, email, role, status; `static from(User)`); never return the entity. Shared by seeker + company signup (same reply shape). Build OK.
- 11d-2 `user/SignupController` (`@RestController @RequestMapping("/auth/signup")`, `POST /seeker`, `@ResponseStatus(CREATED)`, `@Valid @RequestBody`, returns `SignupResponse.from(user)`). `mvn install` OK, tests green.
- Postman switch (user chose REPLACE .md with JSON): P1 done `postman/environments/HireBuddy-Local.postman_environment.json` (var `authUrl`=http://localhost:8081; add vars only when first needed; no secrets in it). P2 done: `postman/collections/HireBuddy-API.postman_collection.json` (v2.1; Auth → `Demo (TEMPORARY)` ok/fail, `Signup` 1 success (pre-request sets collection var `seekerEmail`=seeker_<ms>@example.com) / 2 email taken 409 / 3 invalid 400; small `pm.test` checks; expected reply in each description). P3 done: `postman/README.md` rewritten (import, run, add endpoint, no secrets); `postman/auth-service.md` deleted (replaced by collection); Postman rule updated.
- 11d-3 shared-lib handler `handleValidation` (`MethodArgumentNotValidException` → 400 `VALIDATION_FAILED`, message = field errors sorted by field, joined "; "). Proven live on 18081. Postman request 3 now checks the code. Later idea: per-field `fieldErrors` list in ApiError for frontend forms.
- Postman simplified: environment file deleted; `authUrl` moved into collection variables (import 1 file, no env dropdown). README updated. (Audit-backend reference had no Postman files: its `postman/` is gitignored.)

**Session 5 (2026-11-07):** (user: explain only must-know + interview points; later "ek hi baar me" batches, very short explanations)
- JWT concepts re-taught from zero (why tokens, 3 parts, RS256, access/refresh + rotation, login flow). Decision A: UNVERIFIED may log in (issue M).
- User chose: build JWT OURSELVES (no JWT library; JDK `Signature` SHA256withRSA + `KeyFactory` + Base64URL, Jackson 3 `tools.jackson.databind.json.JsonMapper` for JSON). Compliance project (memory) used as reference; its 1-JWT + `token_version` idea rejected (gateway can't read auth_db).
- auth-service `token/`: `dto/LoginRequest`, `dto/TokenResponse`, `dto/RefreshRequest`, `JwtProperties` (`jwt.private-key-path`, `access-token-ttl: 15m`, `refresh-token-ttl: 7d`; `@EnableConfigurationProperties` in AppConfig), `PemKeyReader` (private key PKCS#8), `JwtService.createAccessToken` (claims sub/role/companyId?/iat/exp), `RefreshToken` entity + `RefreshTokenRepository`, `TokenService` (issue / refresh with rotation / logout), `LoginService.login` (dummy BCrypt, same 401 INVALID_CREDENTIALS, lock 5 fails → 15 min 429 ACCOUNT_LOCKED, `noRollbackFor = ApiException.class`, SUSPENDED 403), `TokenController` POST `/auth/login` `/auth/refresh` `/auth/logout` (204).
- `V2__create_refresh_tokens.sql` (token_hash CHAR(64) UNIQUE = SHA-256 hex, FK users ON DELETE CASCADE). auth_db at v2. User got `isLocked/recordFailedLogin/recordSuccessfulLogin`.
- `user/MeController` GET `/auth/me` (reads `X-User-Id` header, 401 if missing). `SignupResponse` renamed → `user/dto/UserResponse` (signup + me).
- shared-lib `security/JwtVerifier` (verify order: 3 parts → alg RS256 only → seal with public key → payload → exp; 401 INVALID_TOKEN / TOKEN_EXPIRED; `readPublicKey(pem)`), `security/AuthHeaders` (X-User-Id, X-User-Role, X-Company-Id). Verify moved here from JwtService; auth-service no longer needs the public key.
- NEW module `gateway` (port 8080): `spring-cloud-starter-gateway-server-webmvc` (5.0.4), `GatewayApplication`, `config/AppConfig` (Clock, JwtVerifier bean from `jwt.public-key-path`), `security/JwtAuthFilter` (OncePerRequestFilter: public = /api/auth/login|refresh|logout + /api/auth/signup/**; else Bearer JWT → verify → add X-User-* headers; errors as ApiError JSON), `security/AuthHeadersRequest` (wrapper: hides client X-User-*, adds trusted ones). yml route `auth-service` Path=/api/auth/** → http://localhost:8081, StripPrefix=1 (prefix `spring.cloud.gateway.server.webmvc.routes`).
- Live tests all OK (login/refresh/rotation/logout/lockout; via gateway: signup 201, me no token 401, me token 200, broken token 401, fake X-User-Id ignored). Postman folders `Auth/Login & tokens` (7) + `Gateway` (5), var `gatewayUrl`. User has NOT had the gateway explained yet (left mid-way, "baad me smjhnge").

**Session 6 (2026-11-08) – PROFESSIONAL REWRITE** (user: "only needed things, professional like real websites, simple explanations"; hand-made JWT was for learning only):
- Hand-made JWT removed: `PemKeyReader`, shared `JwtVerifier`/`AuthHeaders`, gateway `JwtAuthFilter`/`AuthHeadersRequest`, X-User-* headers. Demo code + `DemoControllerTest` deleted (were TEMPORARY).
- Now Spring Security everywhere (`spring-boot-starter-security-oauth2-resource-server`; brings BCrypt + Nimbus): auth-service `config/SecurityConfig` (stateless, CSRF off, public POST signup/login/refresh/logout + /error, rest authenticated; `NimbusJwtEncoder.withKeyPair`, `NimbusJwtDecoder.withPublicKey`, keys via `RsaKeyConverters`), `JwtProperties` (Resource privateKey/publicKey `file:${JWT_*}`, ttl 15m/7d), `JwtService` uses `JwtEncoder` (`JwtClaimsSet`, RS256 header), `MeController` uses `@AuthenticationPrincipal Jwt`.
- Gateway `config/SecurityConfig` same rules with `/api` prefix; decoder from `spring.security.oauth2.resourceserver.jwt.public-key-location`. Gateway forwards the Authorization header; services check the token AGAIN (defense in depth) → issue N fixed (direct 8081 call without token = 401).
- shared-lib `security/SecurityErrorHandler` (AuthenticationEntryPoint + AccessDeniedHandler → ApiError JSON: 401 UNAUTHENTICATED / INVALID_TOKEN, 403 FORBIDDEN).
- Kept as is (real business logic): signup, LoginService (lockout, dummy hash), TokenService (refresh rotation, SHA-256 in DB), refresh_tokens.
- `mvn clean install` OK (no tests now). Live tests all OK. Postman: Demo folder + fake-header test removed.

- Admin seeder: `user/AdminSeeder` (ApplicationRunner; creates ADMIN/ACTIVE from yml `admin.email`/`admin.password` ← `.env` ADMIN_EMAIL=4urakesh0@gmail.com / ADMIN_PASSWORD; never changes an existing admin; runs just after "Started"). Admin login → token role ADMIN ✅. 2026-11-08: auth_db DROPPED + recreated clean (user asked); Flyway rebuilt v1+v2; only row = admin 4urakesh0@gmail.com.

## Open issues (ask user before fixing)
- A. Reference doc §10 says "Phases 0–2 are done" → stale.
- B. `hirebuddy/docs/Job_Portal_Project_Reference.md` = old copy (PostgreSQL + Keycloak) → wrong; `backend/` copy is the truth.
- C. Reference names differ from code: `exception/` vs `error/`, `Roles` vs `Role` enum, `ApiResponse`/`PageResponse`, Lombok (we don't use it).
- D. `.idea/compiler.xml` lists deleted modules → IntelliJ "Reload All Maven Projects".
- F. Still Spring default JSON: wrong method (405), broken/missing JSON body (`HttpMessageNotReadableException`).
- J. No automated tests right now (demo tests deleted). Add a few when useful.
- L. Consent not stored → V3 migration `consent_at`.
- M. UNVERIFIED users can log in (decision A) → block once email verification is built.
- P. Gateway has no CORS / rate limit yet (needed when frontend starts; rate limit needs Redis).
- Q. TOKEN_EXPIRED vs INVALID_TOKEN no longer separate (one 401 INVALID_TOKEN): frontend refreshes on any 401, logs out if refresh fails.

## Next (start here)
1. Explain the rewrite SIMPLY (what Spring Security does for us now, request journey gateway → service, 401/403). Re-import Postman. In IntelliJ: reload Maven, run AuthServiceApplication + GatewayApplication.
2. Git for user: `git add .` → commit → push.
3. Order to finish auth (Phase 1 needs all 4 roles): company signup (NEXT; show plan first) → role-based auth (`JwtAuthenticationConverter`: claim `role` → `ROLE_X`, `@EnableMethodSecurity` + `@PreAuthorize`) → recruiter invite (COMPANY_OWNER only) → email verification → forgot/reset password → suspend/delete.
