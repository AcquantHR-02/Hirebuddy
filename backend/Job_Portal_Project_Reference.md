# Job Portal – Project Reference

Instructions and context for building this project. Read the whole file before starting any work.

## 1. Overview

Job portal for India, built by one developer. One Spring Boot application (modular monolith), Next.js and MySQL 8.

| Layer | Choice |
| --- | --- |
| Frontend | Next.js (App Router): `/seeker`, `/employer`, `/admin`, plus role-based middleware |
| Backend | ONE Spring Boot application (latest stable), Java 21, Maven. Modular monolith: one module (feature folder) per business area |
| Database | MySQL 8, ONE database `hirebuddy_db`, Flyway migrations |
| Auth | `auth` module inside the app: Spring Security + JWT (no Keycloak) |
| Events | Spring application events inside the app (no message broker) |
| Cache / rate limit | Added later (in-app first; Redis only if really needed) |
| Files | MinIO, S3-compatible (added later) |

**Why a monolith:** one app to run, debug and deploy. Each module keeps clean borders, so it could be split into services later if ever needed.

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

## 3. Modules

| Module | v1 features | Later |
| --- | --- | --- |
| auth | Users, BCrypt passwords, signup (seeker / company), login, JWT access + refresh tokens, logout, login lockout, email verification, password reset, recruiter invites, suspend / delete | Google sign-in, admin 2FA |
| company | Company + verification, team / seats | — |
| seeker | Seeker profile | Profile visibility |
| job | Jobs, expiry scheduler, entitlement / quota check, MySQL FULLTEXT search, saved jobs | Elasticsearch, job alerts, recommendations |
| masterdata | Skills, cities, industries, job categories | — |
| application | Apply (profile + resume snapshot), duplicate check, pipeline stages, status history, withdraw, recruiter notes | Bulk actions, interview scheduling, screening questions |
| payment | Plans, payment requests, admin approval queue, subscriptions, expiry / renewal scheduler, billing history | Coupons, PDF receipts |
| file | Resume / logo / screenshot upload, type + size checks, signed URLs, access control | Virus scan |
| notification | Event listeners, email templates, in-app notifications | User preferences, SMS / WhatsApp |
| admin | Dashboard stats, moderation, audit log | Scam / fake job reports |
| common | Error format, security setup, shared config (no business logic) | — |

Admin actions live in the module that owns the data. For example, company verification is in `company` and payment approval is in `payment`.

**Cross-cutting:** global error handler, Flyway, Actuator health, structured logs, CORS, DPDP consent and account deletion. Later: rate limiting, Prometheus, Grafana, OpenTelemetry, data export, retention cleanup.

### Module details

**Security (whole app)**
- One `SecurityConfig`: public endpoints (signup, login, refresh, logout) are open; everything else needs a valid JWT.
- The JWT is checked once, by Spring Security, inside the app. Role rules use `@PreAuthorize`.
- CORS for the Next.js frontend. Rate limits on login, apply and search (later).

**company**
- Company registration: name, logo, GSTIN, website, industry, size, location.
- Verification: `PENDING → VERIFIED / REJECTED` (with reason), reviewed by the admin. Only verified companies can post jobs.
- Team: owner invites recruiters (invite token issued by the `auth` module), removes them, transfers ownership. Recruiter seat limit comes from the company's plan (free = 1).

**seeker**
- Personal details, education, experience, skills, preferred locations, expected salary.
- Resume attached through the `file` module; can be replaced.

**job**
- Create, edit, close, reopen. Fields: title, description, skills, experience range, salary range, location, work mode (remote / hybrid / onsite), job type, category.
- Every job has `company_id` and `created_by` (recruiter).
- Scheduler closes jobs after 60 days and sends reminders a few days before.
- Entitlement / quota: before every job post and recruiter invite, ask the `payment` module's public service for the company's limits (free posts used, active job slots, recruiter seats, subscription end date).

**job: search and saved jobs**
- Filter by keyword, location, skills, experience, salary and work mode using MySQL FULLTEXT search.
- Seekers can save jobs.

**masterdata**
- Skills, cities, industries and job categories are managed by the admin.

**application**
- Apply stores a snapshot of the seeker's profile and the resume version at that moment, so later edits don't change what the recruiter sees.
- One application per seeker per job.
- Recruiters move candidates through the stages; every change is recorded in a status history (who, what, when).
- Seekers can withdraw. Recruiter notes and ratings are visible only inside the company.

**payment**
- Plan: name, price, duration in days, active job slots, recruiter seats. Admin creates and edits plans.
- Payment request: plan, transaction ID, optional screenshot, status, rejection reason. Only one pending request per company.
- Subscription: plan, start and end dates, status. Renewal approved early starts when the current period ends.
- Built behind a `PaymentProvider` interface with one implementation, `ManualApprovalProvider`, so a real gateway can be added later without changing subscription logic.
- Billing history for companies and admin.

**file**
- Upload and download for resumes (PDF / DOCX), logos and payment screenshots (images), with type and size checks.
- Access control: a resume is downloadable only by its seeker, recruiters of companies they applied to, and admins. Downloads use short-lived signed URLs.

**notification**
- Listens to the events in section 6 and sends emails from templates.
- In-app notifications with read / unread state.

**admin**
- Dashboard: users, companies, active jobs, applications, approved payments.
- Moderation: block jobs, suspend users or companies.
- Audit log of every admin action (who, what, when).

**Privacy (India DPDP Act)**
- Consent checkbox at signup, account deletion, and limited retention of old applications.

## 4. Auth module design

**Tables**
- `users`: id, email (unique), password_hash, role, status (`UNVERIFIED` / `ACTIVE` / `SUSPENDED`), company_id (nullable), failed_login_count, locked_until, timestamps
- `refresh_tokens`: hashed token, user_id, expiry, revoked
- `verification_tokens`: hashed token, user_id, type (`EMAIL_VERIFY` / `PASSWORD_RESET`), expiry, used
- `recruiter_invites`: email, company_id, invited_by, hashed token, expiry, status

**Endpoints** (all under `/api`)
- `POST /api/auth/signup/seeker`, `POST /api/auth/signup/company`
- `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`
- `POST /api/auth/verify-email`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`
- `POST /api/auth/invites` (owner), `POST /api/auth/invites/accept` (recruiter)
- `GET /api/auth/me`, `DELETE /api/auth/me` (delete own account, password asked again)
- `POST /api/auth/users/{id}/suspend`, `POST /api/auth/users/{id}/unsuspend` (admin)

**Tokens**
- Access token: JWT signed with RS256, valid 15 minutes. Claims: user id, role, company id.
- Refresh token: valid 7 days, rotated on every refresh, stored hashed.
- The app signs with the private key and checks with the public key (key files outside the project).

**Security rules**
- BCrypt for passwords; never log or return them.
- Store refresh, verification and reset tokens hashed, with an expiry, and make each single-use.
- Lock the account temporarily after 5 failed logins.
- Forgot-password returns the same response whether or not the email exists.
- Until the notification module exists, log verification and reset links to the console.

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
2. The `job` module checks: company `VERIFIED`, and either free posts used < 5 or active jobs < plan slots.
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

## 6. Ports, database and events

| Component | Local port | Notes |
| --- | --- | --- |
| Next.js | 3000 | — |
| HireBuddy backend (one app) | 8080 | All endpoints under `/api/...` |
| MySQL 8 | 3306 | One database `hirebuddy_db` |
| MinIO | 9000 / 9001 | Added later |

Events are Spring application events (`ApplicationEventPublisher` + `@TransactionalEventListener`, run after the transaction commits). No broker needed.

| Event | Published by | Listened by |
| --- | --- | --- |
| `CompanyVerified` | company | notification |
| `RecruiterInvited` | auth | notification |
| `JobExpiring` | job | notification |
| `ApplicationSubmitted` | application | notification |
| `ApplicationStatusChanged` | application | notification |
| `PaymentApproved` / `PaymentRejected` | payment | notification |
| `SubscriptionActivated` / `SubscriptionExpired` | payment | notification |

## 7. Backend folder structure

```
hirebuddy/
├── backend/
│   ├── pom.xml                         # one Maven project: Boot BOM, all dependencies
│   ├── .env / .env.example             # DB + JWT + admin settings (.env gitignored)
│   ├── postman/                        # one Postman collection
│   ├── README.md                       # setup + run (later)
│   └── src/
│       ├── main/
│       │   ├── java/com/jobportal/hirebuddy/
│       │   │   ├── HireBuddyApplication.java   # main class only
│       │   │   │
│       │   │   ├── common/                     # shared, NO business logic
│       │   │   │   ├── config/      AppConfig (Clock, PasswordEncoder), CorsConfig
│       │   │   │   ├── exception/   ApiException, GlobalExceptionHandler, ErrorCode
│       │   │   │   ├── dto/         ApiError, PageResponse
│       │   │   │   ├── security/    SecurityConfig, JwtService, Role, error handlers
│       │   │   │   └── util/        small helpers (only if needed)
│       │   │   │
│       │   │   ├── auth/                       # one business module
│       │   │   │   ├── controller/  SignupController, AuthController, InviteController, UserAdminController
│       │   │   │   ├── service/     SignupService, LoginService, TokenService, UserService, VerificationService,
│       │   │   │   │                AccountService, InviteService, AdminSeeder
│       │   │   │   ├── repository/  UserRepository, RefreshTokenRepository, VerificationTokenRepository,
│       │   │   │   │                RecruiterInviteRepository
│       │   │   │   ├── entity/      User, UserStatus, RefreshToken, VerificationToken, TokenType,
│       │   │   │   │                RecruiterInvite, InviteStatus
│       │   │   │   ├── dto/         *Request (Signup, Login, Refresh, VerifyEmail, ForgotPassword, ResetPassword,
│       │   │   │   │                DeleteAccount, Invite, AcceptInvite), UserResponse, TokenResponse, InviteResponse
│       │   │   │   ├── mapper/      entity → DTO (only if needed)
│       │   │   │   └── event/       events this module publishes (later)
│       │   │   │
│       │   │   ├── company/   seeker/   job/   masterdata/   application/
│       │   │   └── payment/   file/   notification/   admin/
│       │   │                  (each uses the same inner folders as auth/,
│       │   │                   plus scheduler/ where needed)
│       │   │
│       │   └── resources/
│       │       ├── application.yml            # common settings
│       │       ├── application-dev.yml        # local dev profile (when needed)
│       │       ├── db/migration/              # V1__..., V2__... (one Flyway folder)
│       │       └── templates/email/           # email templates (later)
│       └── test/java/com/jobportal/hirebuddy/ # same folders as main
└── frontend/                           # Next.js (later)
```

Create a folder only when its first file comes.

**Request journey through the layers:** `controller` (HTTP in/out, validation) → `service` (business rules, transactions) → `repository` (database) → `entity` (table row). DTOs carry data in and out; entities never leave the service layer.

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
├── lib/                # api/ (one file per module), auth/ (token handling), utils/
├── types/              # TypeScript types matching backend DTOs
└── middleware.ts       # role-based route protection
```

## 8. Conventions (must follow)

- **Module first, then layers.** Top level = business modules (`auth`, `job`…); inside each module = layer folders (`controller/`, `service/`, `repository/`, `entity/`, `dto/` (flat: `...Request` and `...Response` side by side), `mapper/`, `event/`). Only the folders a module needs; lowercase singular names.
- **Module borders.** A module owns its tables; only its own `repository/` reads or writes them. Other modules call its `service/`, never its `repository/` or `entity/`.
- **`common` holds no business logic.** Only error format, security setup and shared config.
- **Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate`. Never change a migration that has already run; add a new one.
- **Credentials come from `backend/.env`** (`DB_USER`, `DB_PASSWORD`, JWT key paths, admin email/password). Never hard-code them.
- **Consistent API errors** through the global exception handler in `common/exception`.
- **Payments are fake.** No payment gateway. Every subscription needs admin approval.
- **No Docker, no deployment, no CI/CD** for now. Everything runs locally from the IDE.

## 9. Development phases

| Phase | Scope | Done when |
| --- | --- | --- |
| 0 | Backend skeleton (one app) | `mvn clean install` passes; app starts and `/actuator/health` is UP |
| 1 | Auth module + security | Users of all 4 roles can sign up, log in and refresh tokens; protected endpoints reject requests without a valid token |
| 2 | company, job, masterdata: companies, verification, recruiters, jobs, free quota, job search | A verified company can post up to 5 jobs; seekers can search them |
| 3 | Seeker profile, resume upload, apply, recruiter pipeline | A seeker applies and a recruiter moves them through the stages |
| 4 | Plans, fake checkout, admin approval, subscriptions | An approved payment raises the company's limits |
| 5 | Events, notifications, schedulers, admin dashboard, audit log | Emails and in-app alerts fire; jobs expire after 60 days |
| 6 | Next.js frontend | All flows usable from the browser |

Work on one phase at a time. Do not start a phase until the previous one meets its "done when".

## 10. Current status

2026-11-13: architecture changed from microservices to a modular monolith. All old code and the old database were removed; the backend is rebuilt from Phase 0, step by step while learning.
2026-11-15: Phase 0 done (skeleton, DB, Flyway, error handling, Actuator). Phase 1 done (all auth features in §4; recruiter invite success path needs Phase 2 companies). Phase 2 `company` module done (register, edit/resubmit, admin verify/reject, team list/remove, transfer ownership; invites need a VERIFIED company). Next: `masterdata`, then `job`.
Current status and next steps: `backend/CLAUDE.md` → Progress / Next.
