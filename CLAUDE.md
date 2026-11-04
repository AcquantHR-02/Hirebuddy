# HireBuddy Backend – Job Portal (India)

Backend only. Full idea, rules, flows: `Job_Portal_Project_Reference.md` (read first).

## Learning mode (most important)
The user is building this to LEARN. Restarted from zero on 2026-11-03 because the first build went too fast to follow.
- One tiny piece per turn (one file, or a few lines). Never several files at once.
- Before writing: explain what it is and why, in simple words.
- After writing: explain every line / annotation. Then ask a quick check question or let the user ask.
- Wait for the user's OK before the next piece. A general "go on" is approval for ONE piece only.
- Introduce a concept (Maven, Spring Boot, JPA, Flyway, JWT…) the first time it appears, briefly.
- No scope creep; ask first. No Docker/CI/deploy.
- Update Progress after every step. Keep this file short.
- Testing: every new endpoint → add it to `backend/postman/<service>.md` (template in `postman/README.md`: cURL to import, expected reply, bad cases). Automated tests are taught with the first endpoint (plan below).

### Explanation style (user liked this — keep it every session)
VERY simple words (user asked again in session 2): short sentences, no unexplained jargon, plain meaning of each line first. AND detailed. Each step's reply follows this shape:
1. Title "Part N: <what>" + one line on what changed and build result (BUILD SUCCESS ✅).
2. New ideas first, each with a simple real-life analogy (thali, emergency brake, parcel sticker…).
3. Code shown block by block, each line explained; small tables for fields/options.
4. Small ASCII diagrams for flows/structure (folder tree, request journey).
5. Run something to prove it works (mvn build, `dependency:tree`, tiny demo in scratchpad — never in project).
6. End: "✅ Quick check" (2–3 questions) + "Next: part N+1" preview + "Say OK when ready".

## Stack
Spring Boot 4.0.9, Spring Cloud 2025.1.4, Java 21, Maven multi-module, MySQL 8 (Workbench).
Auth: own `auth-service` (Spring Security + JWT RS256), no Keycloak.
Later (nothing may require them to start yet): RabbitMQ, Redis, MinIO.

## Conventions
- groupId `com.jobportal`; package by feature `com.jobportal.<service>.<feature>`.
- One DB per service; no cross-DB access or cross-service imports. Talk via HTTP/events.
- Flyway owns schema; `ddl-auto=validate`; never edit a run migration.
- Creds from env: `DB_USER`, `DB_PASSWORD`, `INTERNAL_API_KEY`, `ADMIN_EMAIL`/`ADMIN_PASSWORD`, JWT key paths.
- Versions only in parent `pom.xml` (BOMs). Plugins need explicit `${spring-boot.version}`.
- Own parent pom (not spring-boot-starter-parent) so every setting is visible.
- Inject `Clock`, never `Instant.now()`. Every service must define a `Clock` bean.
- IDs: UUID `CHAR(36)`; timestamps UTC `DATETIME(6)`.

## Services (planned: port · DB)
gateway 8080 · auth 8081 `auth_db` · core 8082 `core_db` · application 8083 `application_db` · payment 8084 `payment_db` · file 8085 `file_db` · notification 8086 `notification_db`

## Environment
- Maven: `C:\Users\Rakesh\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd`. Build all: `mvn install` in `backend/`.
- Java 21.0.9 on PATH (`jar` tool is not). Demos: `java -cp shared-lib\target\classes <scratchpad>\Demo.java`.
- MySQL 8: service `MySQL80`, localhost:3306, user `root`; password only in env `DB_PASSWORD` (never in files). Client: `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`.
- Env vars are set at Windows user level; a fresh shell may need them loaded.
- JWT RSA keys (from old build, still there): `C:\Users\Rakesh\.hirebuddy\keys\`.
- DB `audit_management` is NOT ours; never touch it. No HireBuddy DBs exist yet.

## Progress
**Session 1 (2026-11-03):** old code + 7 DBs deleted; big picture explained (services, Maven, multi-module). Then:

Parent `pom.xml` (done):
- 1.1 skeleton + coordinates (`hirebuddy-parent`, packaging `pom`). Taught: XML, groupId/artifactId/version, SNAPSHOT.
- 1.2 `<properties>`: `maven.compiler.release` 21, UTF-8, `spring-boot.version`, `spring-cloud.version`. Taught: `${...}`.
- 1.3 `<dependencyManagement>`: Spring Boot + Spring Cloud BOMs (`type pom`, `scope import`). Taught: BOM, `.m2`.
- 1.4 `<build><pluginManagement>`: `spring-boot-maven-plugin` + `repackage`; `maven.compiler.parameters=true`. Taught: library vs plugin, fat jar.
- 1.5 `<modules>` (only `shared-lib`). Taught: reactor, `target/`, lifecycle, install.

`shared-lib` (enough for now):
- 1.5 `pom.xml` with `<parent>` link.
- 1.6 `security/Role.java` enum (SEEKER, COMPANY_OWNER, RECRUITER, ADMIN). Taught: src layout, packages, enum.
- 1.7 `error/ApiError.java` record (timestamp, status, error, message, path). Taught: DTO, record, import, Instant/UTC.
- 1.8 `error/ApiException.java` (extends RuntimeException; `int status`, `String code`). Taught: throw/try/catch, extends/super, checked vs unchecked.
- 1.9 dep `spring-boot-starter-webmvc` `<optional>true</optional>`. Taught: starter, transitive deps, `dependency:tree`, optional.
- 1.10 `error/GlobalExceptionHandler.java` (`@RestControllerAdvice`, handles only `ApiException`, `Clock` via constructor). Taught: annotations, beans, DI, ResponseEntity, generics. Not run yet.

Build: `mvn install` BUILD SUCCESS, 4 classes.

**Session 2 (2026-11-03):** project analysed (open issues listed below).
- 2.1 `auth-service/pom.xml` + `<module>` in parent (deps webmvc + shared-lib `${project.version}`; boot plugin on). Taught: module pom, pluginManagement vs plugins, `${project.version}`. Build failed "Unable to find main class" (expected).
- 2.2a `auth/AuthServiceApplication.java`: only `@SpringBootApplication` + `main` (split slow on purpose; no @Import/Clock yet). BUILD SUCCESS, fat jar ~20 MB, ran it: Tomcat on default 8080, `/` → 404. Taught: main method, static, annotation, SpringApplication.run, component scan, auto-config, fat jar.
- 2.2b `@Bean Clock clock()` → `Clock.systemUTC()` in main class. BUILD SUCCESS. Taught: @Bean, Clock, systemUTC vs fixed (scratchpad demo).
- 2.2c `@Import(GlobalExceptionHandler.class)` on main class. BUILD SUCCESS; app starts (3 s), `/` still 404 (no controllers). Taught: @Import vs component scan, why Clock had to come first. Explained Whitelabel 404 page.
- 3 `src/main/resources/application.yml` (app name `auth-service`, port 8081) BUILD SUCCESS; runs on 8081, logs show `[auth-service]`. Taught: YAML, resources folder, properties. Lesson: a running jar locks `target/*.jar` → stop test runs (full java.exe path so PID is right).
- 4a `auth/demo/DemoController.java` (TEMPORARY, delete later): `GET /auth/demo/ok` → 200 text; `GET /auth/demo/fail` → ApiException → 404 ApiError JSON. Verified with curl. Taught: controller, @RestController, @RequestMapping, @GetMapping, request journey.
- 4b `postman/auth-service.md`: 2 demo endpoints + bad cases (POST → 405, wrong path → Spring default 404 JSON, not ours). Replies recorded from a real run. Taught: cURL, status codes, browser HTML vs Postman JSON.
- `postman/README.md` (import cURL into Postman + endpoint template).

Session 2 ended after 4b (user stopped for the day). Build: BUILD SUCCESS; auth-service runs on 8081.

**Session 3 (2026-11-04):** project re-analysed; clean build OK (shared-lib 5.7 KB jar, auth-service ~20 MB). Env vars + JWT keys present; MySQL80 running, still no HireBuddy DBs. Issues B, H, I added.
- 5 `backend/.gitignore` (target/, .idea/, *.iml, .vscode/, OS junk, *.log, .env, *.pem). Proved with scratchpad git demo. Taught: what git ignores and why. `git init` not done yet (ask user).

Testing plan (suggested, user to confirm): Layer 1 = Spring Boot tests in Java, run by `mvn test`. Layer 2 (later) = Postman collection + Newman (Node v24 installed; Newman not yet).

## Open issues (found in analysis; ask user before fixing)
- A. Reference doc §10 says "Phases 0–2 are done" → stale (from old build).
- B. `hirebuddy/docs/Job_Portal_Project_Reference.md` = older copy (117 lines vs 320): says PostgreSQL + Keycloak → wrong; `backend/` copy is the truth.
- H. Reference §10 Phase 0 brief (all 6 services at once, Lombok, `package-info.java`, root CLAUDE.md) conflicts with learning mode → treat as goal, not instructions. Phase 0 "done" also needs Actuator `/actuator/health` (not added yet).
- I. Empty `hirebuddy/frontend/` folder (fine; Phase 6).
- C. Reference doc names differ from code: `exception/` vs our `error/`, `Roles` constants vs `Role` enum, `ApiResponse`/`PageResponse`, Lombok.
- D. `.idea/compiler.xml` lists 6 deleted modules → IntelliJ "Reload All Maven Projects".
- E. Not a git repo yet (`.gitignore` done in step 5; `git init` pending).
- F. Two error shapes: unknown path / wrong method → Spring default JSON (no `message`), not our ApiError. Fix in handler later.
- G. `ApiException` takes any int status (e.g. 999 would crash); maybe use `HttpStatus` later.

## Next (start here)
auth-service, part by part (wait OK each):
4c-1. Add `spring-boot-starter-test` (scope `test`) to `auth-service/pom.xml`. Teach: test scope, JUnit, what the starter brings.
4c-2. First test class for the demo endpoints, run by `mvn test`. Teach: @SpringBootTest / MockMvc, assertions.
Then: handler cases (no-route 404, 405, validation errors, unknown → 500), fix issue F.
