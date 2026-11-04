# Job Portal – Project Reference

The complete idea of the project. `CLAUDE.md` holds conventions and progress; this file holds the *what* and *why*.
Source: "Job Portal – Project Reference" PDF (2026-10-29), plus decisions made since.

## 1. Overview
Job portal for India. Solo build. Jobs belong to the **company**, not the recruiter.

| Layer | Choice |
|---|---|
| Frontend | Next.js (`/seeker`, `/employer`, `/admin`), role-based middleware |
| Backend | Spring Boot microservices |
| Database | PostgreSQL, one DB per service, Flyway migrations |
| Auth | Keycloak (JWT) |
| Events | RabbitMQ + outbox pattern |
| Cache / rate limit | Redis |
| Files | MinIO (S3-compatible) |

## 2. Roles
| Role | Can do |
|---|---|
| SEEKER | Profile, resume, search, apply, track status |
| COMPANY_OWNER | Register company, invite recruiters, buy plan, post jobs |
| RECRUITER | Post jobs, manage applicants for the company |
| ADMIN | Verify companies, approve payments, moderate, view stats |

## 3. Business rules
| Rule | Value |
|---|---|
| Free tier | 5 job posts per company, lifetime; 1 recruiter |
| Paid plans | Subscription; limit = active job slots + recruiter seats |
| Job expiry | 60 days, then auto-close; slot freed |
| Expiry reminder | A few days before; recruiter can renew |
| Payment | Fake, no real money: transaction ID + optional screenshot |
| Activation | Only after admin approves payment |
| Pending requests | Max 1 per company |
| Renewal | Same flow; new period starts when current ends |
| Lapsed plan | Live jobs run to expiry; no new posts; short grace period |
| Company verification | Admin must verify before first post |

Plan names, prices and limits: set by admin in the Plan module.

## 4. Services
Admin actions live in the service that owns the data.

| Service | Port | DB | v1 modules | Later |
|---|---|---|---|---|
| API Gateway | 8080 | – | Routing, JWT check, rate limit, CORS, request logs | – |
| Auth (Keycloak + user mgmt) | 8081 | keycloak_db | Signup (seeker/company), login, refresh, email verify, password reset, roles, recruiter invites, suspend/delete | Google sign-in, admin 2FA |
| Core | 8082 | core_db | Company + verification, team/seats, seeker profile, jobs, expiry scheduler, entitlement/quota, Postgres search, saved jobs, master data | Elasticsearch, job alerts, recommendations, profile visibility |
| Application | 8083 | application_db | Apply (profile + resume snapshot), duplicate check, pipeline stages, status history, withdraw, recruiter notes | Bulk actions, interview scheduling, screening questions |
| Subscription & Payment | 8084 | payment_db | Plans, payment requests, admin approval queue, subscriptions, expiry/renewal scheduler, billing history | Coupons, PDF receipts |
| File | 8085 | file_db + MinIO | Resume/logo/screenshot upload, type + size checks, signed URLs, access control | Virus scan |
| Notification | 8086 | notification_db | Event consumers, email templates, in-app notifications | User preferences, SMS/WhatsApp |
| Admin & Reporting | – | – | Dashboard stats, moderation, audit log | Scam/fake job reports |

**Cross-cutting:** shared library (DTOs, errors, events), outbox pattern, Flyway, Actuator health, structured logs, DPDP consent + account deletion. Later: Prometheus, Grafana, OpenTelemetry, data export, retention cleanup.

## 5. Payment approval flow
1. Free quota used → posting blocked.
2. Owner picks plan → submits fake payment: transaction ID (+ screenshot). No money moves.
3. Request = `PENDING_APPROVAL`.
4. Admin approves → subscription `ACTIVE`, limits applied, email sent.
5. Admin rejects (with reason) → owner can resubmit.

## 6. Statuses
| Entity | Values |
|---|---|
| Company | `PENDING` → `VERIFIED` / `REJECTED` |
| Job | `DRAFT` → `ACTIVE` → `CLOSED` / `EXPIRED`; `BLOCKED` by admin |
| Application | `APPLIED` → `SHORTLISTED` → `INTERVIEW` → `OFFERED` → `HIRED` / `REJECTED`; `WITHDRAWN` |
| Payment request | `PENDING_APPROVAL` → `APPROVED` / `REJECTED` |
| Subscription | `PENDING` → `ACTIVE` → `EXPIRED` / `CANCELLED` |

## 7. Infrastructure (local)
| Component | Port | Notes |
|---|---|---|
| Next.js | 3000 | |
| PostgreSQL | 5432 | One instance, separate DBs |
| RabbitMQ | 5672 / 15672 | AMQP / UI |
| Redis | 6379 | Rate limit, cache |
| MinIO | 9000 / 9001 | API / console |

## 8. Events
| Event | Publisher | Consumers |
|---|---|---|
| CompanyVerified | Core | Notification |
| RecruiterInvited | Auth / Core | Notification |
| JobExpiring | Core | Notification |
| ApplicationSubmitted | Application | Notification |
| ApplicationStatusChanged | Application | Notification |
| PaymentApproved / PaymentRejected | Payment | Notification |
| SubscriptionActivated / SubscriptionExpired | Payment | Core (entitlements), Notification |

## 9. Backend code layout (decided)
- Maven multi-module in `backend/`; parent pom imports Spring Boot + Spring Cloud BOMs.
- Modules: `shared-lib`, `gateway`, `core-service`, `application-service`, `payment-service`, `file-service`, `notification-service`.
- Package by feature, e.g. `com.jobportal.core.job`, `com.jobportal.payment.subscription`.
- Feature packages:
  - core: company, team, seeker, job, entitlement, masterdata, scheduler, config
  - application: apply, pipeline, notes, config
  - payment: plan, paymentrequest, subscription, scheduler, config
  - file: upload, access, config
  - notification: listener, email, inapp, config
- shared-lib: `dto` (ApiResponse, PageResponse), `exception` (ApiException, GlobalExceptionHandler, one JSON error format), `security` (Roles), `event` (event contracts).

## 10. Open questions (decide before building the feature)
- Where user management lives (Core vs a separate user service); who publishes RecruiterInvited.
- Admin & Reporting: own service or built into each service; where the audit log lives.
- Free tier counts lifetime posts, paid counts active slots: does renewing a job count as a new post? What happens when a paid plan lapses?
- Subscription `PENDING` = approved but waiting for the current period to end (renewal case).
- Downgrade/lapse with more recruiters or live jobs than the new limit allows.
- Exact numbers for "short grace period" and "a few days before" the expiry reminder.
- Can a rejected company resubmit for verification?
- Does a `BLOCKED` job free its slot? What happens to open applications when a job expires or is blocked? `REJECTED` should be reachable from any stage.
- Missing events: UserDeletionRequested (DPDP deletion across services), JobClosed/JobBlocked, CompanyRejected, RecruiterRemoved, PaymentSubmitted.
- Event payloads should carry the recipient's email; consumers must be idempotent; schedulers need a lock (e.g. ShedLock) if a service runs more than once.
