# Job Portal – Project Reference

Instructions and context for building this project. Read the whole file before starting any work.

## 1. Overview

Job portal for India, built by one developer. Spring Boot microservices, Next.js and MySQL 8.

| Layer | Choice |
| --- | --- |
| Frontend | Next.js (App Router): `/seeker`, `/employer`, `/admin`, plus role-based middleware |
| Backend | Spring Boot microservices (latest stable), Java 21, Maven multi-module |
| Database | MySQL 8, one database per service, Flyway migrations |
| Auth | In-house `auth-service`: Spring Security + JWT (no Keycloak) |
| Events | RabbitMQ + outbox pattern (added later) |
| Cache / rate limit | Redis (added later) |
| Files | MinIO, S3-compatible (added later) |

| Role | Can do |
| --- | --- |
| `SEEKER` | Profile, resume, search, apply, track status |
| `COMPANY_OWNER` | Register company, invite recruiters, buy plan, post jobs |
| `RECRUITER` | Post jobs, manage applicants for the company |
| `ADMIN` | Verify companies, approve payments, moderate, view stats |

A company can have several recruiters. Jobs belong to the company, not to an individual recruiter.

## 2. Business rules

| Rule | Value |
| --- | --- |
| Free tier | 5 job posts per company, lifetime; 1 recruiter |
| Paid plans | Subscription; limits are active job slots + recruiter seats |
| Job expiry | 60 days, then auto-close; the slot is freed |
| Expiry reminder | A few days before expiry; recruiter can renew |
| Payment | Fake, no real money: transaction ID + optional screenshot |
| Activation | Only after the admin approves the payment |
| Pending requests | At most 1 per company |
| Renewal | Same flow; the new period starts when the current one ends |
| Lapsed plan | Live jobs run to expiry; no new posts; short grace period |
| Company verification | Admin must verify the company before its first post |

Plan names, prices and limits are not fixed yet. The admin sets them in the Plan module.

## 3. Services and modules

| Service | v1 modules | Later |
| --- | --- | --- |
| API Gateway | Routing, JWT check (auth-service public key), rate limit, CORS, request logs | — |
| Auth service | Users, BCrypt passwords, signup (seeker / company), login, JWT access + refresh tokens, logout, login lockout, email verification, password reset, recruiter invites, suspend / delete | Google sign-in, admin 2FA |
| Core | Company + verification, team / seats, seeker profile, jobs, expiry scheduler, entitlement / quota, MySQL FULLTEXT search, saved jobs, master data | Elasticsearch, job alerts, recommendations, profile visibility |
| Application | Apply (profile + resume snapshot), duplicate check, pipeline stages, status history, withdraw, recruiter notes | Bulk actions, interview scheduling, screening questions |
| Subscription & Payment | Plans, payment requests, admin approval queue, subscriptions, expiry / renewal scheduler, billing history | Coupons, PDF receipts |
| File | Resume / logo / screenshot upload, type + size checks, signed URLs, access control | Virus scan |
| Notification | Event consumers, email templates, in-app notifications | User preferences, SMS / WhatsApp |
| Admin & Reporting | Dashboard stats, moderation, audit log | Scam / fake job reports |

Admin actions live in the service that owns the data. For example, company verification is in Core and payment approval is in Payment.

**Cross-cutting:** shared library (DTOs, errors, events), outbox pattern, Flyway, Actuator health, structured logs, DPDP consent and account deletion. Later: Prometheus, Grafana, OpenTelemetry, data export, retention cleanup.

### Module details

**API Gateway**
- Single entry point for the frontend; routes `/api/auth/**`, `/api/core/**`, `/api/applications/**`, `/api/payments/**`, `/api/files/**`, `/api/notifications/**` to their services.
- Validates the JWT with the auth-service public key and rejects invalid requests before they reach a service.
- Rate limits login, apply and search (Redis, added later). Handles CORS and request logging.

**Core: company**
- Company registration: name, logo, GSTIN, website, industry, size, location.
- Verification: `PENDING → VERIFIED / REJECTED` (with reason), reviewed by the admin. Only verified companies can post jobs.

**Core: team**
- Owner invites recruiters (invite token issued by auth-service), removes them, transfers ownership.
- Recruiter seat limit comes from the company's plan (free = 1).

**Core: seeker profile**
- Personal details, education, experience, skills, preferred locations, expected salary.
- Resume attached through the File service; can be replaced.

**Core: job**
- Create, edit, close, reopen. Fields: title, description, skills, experience range, salary range, location, work mode (remote / hybrid / onsite), job type, category.
- Every job has `company_id` and `created_by` (recruiter).
- Scheduler closes jobs after 60 days and sends reminders a few days before.

**Core: entitlement / quota**
- Local copy of each company's limits: free posts used, active job slots, recruiter seats, subscription end date.
- Updated when the Payment service activates or expires a subscription.
- Checked on every job post and every recruiter invite. Posting never calls the Payment service directly.

**Core: search, saved jobs and master data**
- Filter by keyword, location, skills, experience, salary and work mode using MySQL FULLTEXT search.
- Seekers can save jobs.
- Skills, cities, industries and job categories are managed by the admin.

**Application**
- Apply stores a snapshot of the seeker's profile and the resume version at that moment, so later edits don't change what the recruiter sees.
- One application per seeker per job.
- Recruiters move candidates through the stages; every change is recorded in a status history (who, what, when).
- Seekers can withdraw. Recruiter notes and ratings are visible only inside the company.

**Subscription & Payment**
- Plan: name, price, duration in days, active job slots, recruiter seats. Admin creates and edits plans.
- Payment request: plan, transaction ID, optional screenshot, status, rejection reason. Only one pending request per company.
- Subscription: plan, start and end dates, status. Renewal approved early starts when the current period ends.
- Built behind a `PaymentProvider` interface with one implementation, `ManualApprovalProvider`, so a real gateway can be added later without changing subscription logic.
- Billing history for companies and admin.

**File**
- Upload and download for resumes (PDF / DOCX), logos and payment screenshots (images), with type and size checks.
- Access control: a resume is downloadable only by its seeker, recruiters of companies they applied to, and admins. Downloads use short-lived signed URLs.

**Notification**
- Listens to the events in section 6 and sends emails from templates.
- In-app notifications with read / unread state.

**Admin & reporting**
- Dashboard: users, companies, active jobs, applications, approved payments.
- Moderation: block jobs, suspend users or companies.
- Audit log of every admin action (who, what, when).

**Privacy (India DPDP Act)**
- Consent checkbox at signup, account deletion, and limited retention of old applications.

## 4. Auth service design

**Tables**
- `users`: id, email (unique), password_hash, role, status (`UNVERIFIED` / `ACTIVE` / `SUSPENDED`), company_id (nullable), failed_login_count, locked_until, timestamps
- `refresh_tokens`: hashed token, user_id, expiry, revoked
- `verification_tokens`: hashed token, user_id, type (`EMAIL_VERIFY` / `PASSWORD_RESET`), expiry, used
- `recruiter_invites`: email, company_id, invited_by, hashed token, expiry, status

**Endpoints**
- `POST /auth/signup/seeker`, `POST /auth/signup/company`
- `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`
- `POST /auth/verify-email`, `POST /auth/forgot-password`, `POST /auth/reset-password`
- `POST /auth/invites` (owner), `POST /auth/invites/accept` (recruiter)

**Tokens**
- Access token: JWT signed with RS256, valid 15 minutes. Claims: user id, role, company id.
- Refresh token: valid 7 days, rotated on every refresh, stored hashed.
- Auth-service holds the private key. The gateway and all other services get only the public key.

**Security rules**
- BCrypt for passwords; never log or return them.
- Store refresh, verification and reset tokens hashed, with an expiry, and make each single-use.
- Lock the account temporarily after 5 failed logins.
- Forgot-password returns the same response whether or not the email exists.
- Until the notification service exists, log verification and reset links to the console.

## 5. Key flows and statuses

**Payment approval (fake payment, admin approval required)**

1. Free quota used → posting is blocked.
2. Owner picks a plan → submits a fake payment: transaction ID (+ screenshot). No money moves.
3. Request status = `PENDING_APPROVAL`.
4. Admin approves → subscription `ACTIVE`, limits applied, email sent.
5. Admin rejects (with reason) → owner can resubmit.

**Company onboarding**

1. Owner signs up → verifies email → registers the company (`PENDING`).
2. Admin reviews → `VERIFIED` (can post) or `REJECTED` with a reason (owner can fix and resubmit).

**Recruiter invite**

1. Owner invites by email (seat limit checked).
2. Recruiter opens the link, sets a password, and joins the company with role `RECRUITER`.

**Posting a job**

1. Recruiter submits a job.
2. Core checks: company `VERIFIED`, and either free posts used < 5 or active jobs < plan slots.
3. Pass → job `ACTIVE` with an expiry 60 days out. Fail → error telling the owner to choose a plan.

**Applying**

1. Seeker applies → snapshot of profile + resume saved → status `APPLIED`.
2. Recruiter moves the application through the stages; each change is recorded and the seeker is notified.

**Expiry**

- Jobs: reminder a few days before; auto-close at 60 days, freeing the slot. Applications stay visible.
- Subscriptions: reminder before the end date; after expiry and the grace period, no new posts. Live jobs run until their own expiry.

**Statuses**

| Entity | Values |
| --- | --- |
| User | `UNVERIFIED` → `ACTIVE`; `SUSPENDED` by admin |
| Company | `PENDING` → `VERIFIED` / `REJECTED` |
| Job | `DRAFT` → `ACTIVE` → `CLOSED` / `EXPIRED`; `BLOCKED` by admin |
| Application | `APPLIED` → `SHORTLISTED` → `INTERVIEW` → `OFFERED` → `HIRED` / `REJECTED`; `WITHDRAWN` |
| Payment request | `PENDING_APPROVAL` → `APPROVED` / `REJECTED` |
| Subscription | `PENDING` → `ACTIVE` → `EXPIRED` / `CANCELLED` |

## 6. Ports, databases and events

| Component | Local port | Database / notes |
| --- | --- | --- |
| Next.js | 3000 | — |
| API Gateway | 8080 | Only public backend entry point |
| Auth service | 8081 | `auth_db` |
| Core | 8082 | `core_db` |
| Application | 8083 | `application_db` |
| Subscription & Payment | 8084 | `payment_db` |
| File | 8085 | `file_db` + MinIO |
| Notification | 8086 | `notification_db` |
| MySQL 8 | 3306 | One local instance, separate databases |
| RabbitMQ | 5672 / 15672 | Added later |
| Redis | 6379 | Added later |
| MinIO | 9000 / 9001 | Added later |

| Event | Publisher | Consumers |
| --- | --- | --- |
| `CompanyVerified` | Core | Notification |
| `RecruiterInvited` | Auth | Notification |
| `JobExpiring` | Core | Notification |
| `ApplicationSubmitted` | Application | Notification |
| `ApplicationStatusChanged` | Application | Notification |
| `PaymentApproved` / `PaymentRejected` | Payment | Notification |
| `SubscriptionActivated` / `SubscriptionExpired` | Payment | Core (entitlements), Notification |

## 7. Backend folder structure

```
job-portal/
├── backend/
│   ├── pom.xml                      # parent pom: Boot + Cloud BOMs, module list
│   ├── shared-lib/                  # plain jar, no main class
│   │   └── com/jobportal/shared/  dto/, exception/, security/, event/
│   ├── gateway/                     # Spring Cloud Gateway (8080)
│   ├── auth-service/                # 8081
│   │   └── com/jobportal/auth/  user/, token/, verification/, invite/, config/
│   ├── core-service/                # 8082
│   │   └── com/jobportal/core/  company/, team/, seeker/, job/, entitlement/,
│   │                            masterdata/, scheduler/, config/
│   ├── application-service/         # 8083
│   │   └── com/jobportal/application/  apply/, pipeline/, notes/, config/
│   ├── payment-service/             # 8084
│   │   └── com/jobportal/payment/  plan/, paymentrequest/, subscription/,
│   │                               scheduler/, config/
│   ├── file-service/                # 8085
│   │   └── com/jobportal/file/  upload/, access/, config/
│   └── notification-service/        # 8086
│       └── com/jobportal/notification/  listener/, email/, inapp/, config/
├── frontend/                        # Next.js (later)
└── docs/
    └── Job_Portal_Project_Reference.md
```

Each service has `src/main/resources/application.yml` and `src/main/resources/db/migration/` (Flyway).

Each service is its own Spring Boot project with its own `main` class, database, port and jar. The parent pom only keeps versions aligned and lets everything build with one command.

### Frontend structure (Phase 6)

```
frontend/
├── app/
│   ├── (public)/       # home, jobs search + detail, companies/[id]
│   ├── (auth)/         # login, signup (seeker / company), verify-email, reset-password
│   ├── seeker/         # dashboard, profile, applications, saved-jobs
│   ├── employer/       # dashboard, jobs (list, new, [id]/edit, [id]/applicants),
│   │                   # team, company, billing (plans, checkout, history)
│   └── admin/          # dashboard, companies (verification), payments (approval),
│                       # plans, users, jobs (moderation), audit-log
├── components/         # ui/ (buttons, inputs, tables), shared/ (navbar, job card)
├── lib/                # api/ (one file per service), auth/ (token handling), utils/
├── types/              # TypeScript types matching backend DTOs
└── middleware.ts       # role-based route protection
```

## 8. Conventions (must follow)

- **Package by feature**, not by layer. Each feature folder holds its Controller, Service, Repository, entity and `dto/`.
- **One database per service.** A service never reads another service's database.
- **No cross-service imports.** Services talk only over HTTP or events. The only shared code is `shared-lib`.
- **`shared-lib` holds no business logic.** Only DTOs, error format, security helpers and event classes.
- **Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate`. Never change a migration that has already run; add a new one.
- **Credentials come from environment variables** (`DB_USER`, `DB_PASSWORD`, JWT key paths). Never hard-code them.
- **Consistent API errors** through `shared-lib`'s global exception handler.
- **Payments are fake.** No payment gateway. Every subscription needs admin approval.
- **No Docker, no deployment, no CI/CD** for now. Everything runs locally from the IDE.

## 9. Development phases

| Phase | Scope | Done when |
| --- | --- | --- |
| 0 | Backend skeleton | `mvn clean install` passes; every service returns UP at `/actuator/health` |
| 1 | Auth service + gateway JWT check | Users of all 4 roles can sign up, log in and refresh tokens; the gateway rejects requests without a valid token |
| 2 | Core: companies, verification, recruiters, jobs, free quota, job search | A verified company can post up to 5 jobs; seekers can search them |
| 3 | Seeker profile, resume upload, apply, recruiter pipeline | A seeker applies and a recruiter moves them through the stages |
| 4 | Plans, fake checkout, admin approval, subscriptions | An approved payment raises the company's limits |
| 5 | RabbitMQ events, notifications, schedulers, admin dashboard, audit log | Emails and in-app alerts fire; jobs expire after 60 days |
| 6 | Next.js frontend | All flows usable from the browser |

Work on one phase at a time. Do not start a phase until the previous one meets its "done when".

## 10. Phase 0 brief (done)

> Phases 0–2 are done. Current status and next steps: `backend/CLAUDE.md` → Progress / Next.

**Scope:** backend skeleton only. No business features, no frontend, no Docker, no deployment. MySQL 8 is installed locally (managed with MySQL Workbench). RabbitMQ, Redis and MinIO are not installed, so nothing may require them to start.

**Build:**
1. `backend/pom.xml`: Maven parent (packaging `pom`), groupId `com.jobportal`, Java 21, latest stable Spring Boot with the matching Spring Cloud release, both imported as BOMs in `dependencyManagement`.
2. `shared-lib`: `dto` (`ApiResponse`, `PageResponse`), `exception` (`ApiException`, `GlobalExceptionHandler` with a consistent JSON error format), `security` (`Roles` constants), `event` (empty).
3. `gateway` with routes to every service. No JWT check yet.
4. `auth-service`, `core-service`, `application-service`, `payment-service`, `file-service`, `notification-service`, each with: Spring Web, Data JPA, MySQL driver, Flyway, Validation, Actuator, Lombok, and a dependency on `shared-lib`. No security, RabbitMQ or MinIO dependencies yet.
5. Each service's `application.yml`: its port and database from section 6, credentials from env vars, `ddl-auto=validate`, Flyway enabled, and an empty `db/migration/V1__init.sql`.
6. Feature packages from section 7, each with a `package-info.java` so it exists.
7. `backend/README.md`: how to create the 6 databases and run each service.
8. `CLAUDE.md` at the repo root summarising section 8.

**How to work:**
- First show the plan and the full folder tree. Wait for approval before writing files.
- Run `mvn clean install` yourself and fix every error before reporting done.
- Don't add anything outside this scope. If something seems necessary, ask first.
