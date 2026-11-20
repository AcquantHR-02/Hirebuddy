# Postman – Kaamsathi API

The collection is in Postman **v3 format**: a folder, one YAML file per request.

```
postman/collections/Kaamsathi API/
├── .resources/definition.yaml      collection name + variables
└── auth/                           one folder per backend module
    ├── .resources/definition.yaml  folder description + order
    └── *.request.yaml              one request each: method, url, body, tests, expected reply
```

## Variables (collection variables, no environment needed)
- `baseUrl` = `http://localhost:8080` (the only one you set; the rest fill themselves)
- `seekerEmail` ← "Signup seeker - success"
- `ownerEmail` ← "Signup company owner - success"
- `verifyToken` ← 6-digit email OTP from signup header `X-Dev-Verify-Token` (needs `DEV_EXPOSE_TOKENS=true`)
- `resetToken` ← "Forgot password - known email" header `X-Dev-Reset-Token` (same flag)
- `accessToken`, `refreshToken` ← "Login - success" / "Refresh - success"
- `oldRefreshToken` ← "Refresh - success" (proves the old token stops working)
- `companyOwnerEmail`, `ownerVerifyToken`, `ownerAccessToken` ← company/"Setup - …" requests
- `gstin`, `companyId` ← "Register company - success" (random valid GSTIN each run)
- `adminEmail`, `adminPassword` → **you set these** (same as `ADMIN_EMAIL`/`ADMIN_PASSWORD` in your `.env`).
  They are empty in the files on purpose (no secrets in git). CLI:
  `--env-var "adminEmail=..." --env-var "adminPassword=..."`; in the app: set the *current value* only.
- `adminAccessToken` ← company/"Setup - login admin"
- `recruiterEmail`, `inviteToken` ← company/"Invite recruiter - success" (header `X-Dev-Invite-Token`)
- `skillName`, `skillNameLower`, `skillId` ← masterdata/"Skills - add (201)" (unique name each run)
- `industryName`, `industryId`, `categoryName`, `categoryId` ← masterdata/"Industries - add" / "Categories - add"
- `cityName`, `cityId` ← masterdata/"Cities - add (201)"
- `recruiterId`, `recruiterAccessToken`, `recruiterRefreshToken` ← company/ team requests
- job/: `jobOwnerEmail`, `jobOwnerVerifyToken`, `jobOwnerAccessToken`, `jobCompanyId`, `jobCategoryId`,
  `jobCityId`, `jobSkillId`, `jobId` ← "Setup - …" + "Post job - success";
  `jobSeekerEmail`, `jobSeekerOtp`, `jobSeekerAccessToken`, `liveJobId` ← saved-jobs setup
- admin/: `newAdminEmail`, `newAdminSetupToken`, `newAdminAccessToken` ← "Add admin" flow;
  `targetEmail`, `targetOtp`, `targetId`, `mainAdminId`, `newAdminId` ← "Users" screen requests
- seeker/: `seekerCityId`, `seekerSkillId`, `seekerEmail2`, `seekerOtp2`, `seekerAccessToken2` ← "Setup - …";
  `photoUserId`, `photoFileId` ← "Photo - upload success"; `resumeFileId` ← "Resume - upload success"
- application/: `appCategoryId`, `appCityId`, `appSkillId`, `appOwnerEmail`, `appOwnerOtp`, `appOwnerToken`,
  `appCompanyId`, `appJobId`, `appClosedJobId`, `appSeekerEmail`, `appSeekerOtp`, `appSeekerToken` ← "Setup - …";
  `appApplicationId` ← "Apply - success (201)"; `appViewedAt` ← "Open application - first open (VIEWED)";
  `appFirstResponseAt` ← "Move - skip to INTERVIEW"; `appNoteId` ← "Note - add (201)"
- payment/: `planName`, `planNameLower`, `planId` ← "Plans - add (201)"; `paymentId` ← "Pay - success with screenshot (201)".
  Reuses `appOwnerToken` (owner of a VERIFIED company) and `appSeekerToken` from application/

## Upload test files
`postman/files/` holds tiny files for upload requests (`photo.png` = real 1×1 PNG, `not-an-image.png` = text, `resume.pdf` = tiny PDF).
Requests point to them as `postman/files/...`, so run the CLI from `backend/`.

## One-time setup
Login needs a verified email. So Postman can verify automatically, put this in your own
`backend/.env` (local PC only, never in production):

```
DEV_EXPOSE_TOKENS=true
```

Then signup (and resend-otp) returns the 6-digit OTP in the `X-Dev-Verify-Token` header and
"Verify email - success" uses it. Without it, the verify/login tests fail.

## Run from the terminal (easiest)
Start the app first (`KaamsathiApplication`, port 8080), then in `backend/`:

```
postman collection run "postman/collections/Kaamsathi API"
```

Other port: add `--env-var "baseUrl=http://localhost:18080"`.

## Open in the Postman app
Open the repo folder as a local / Git-connected workspace (Postman v11+); the app reads
`postman/collections/` directly. If your app version cannot open it, run it from the terminal.

## Check the file format
```
postman collection lint "postman/collections/Kaamsathi API"
```

## Adding a new endpoint
Add `<Name>.request.yaml` in the module folder (create the folder + its
`.resources/definition.yaml` for a new module). Use `order` 1000, 2000, … and quote
`'{{baseUrl}}'`. Run lint, then run the collection.

No passwords/secrets are stored here.
