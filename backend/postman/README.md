# Postman – API tests

All backend requests live in ONE Postman collection, saved here so it is in git.
No environment file: the server addresses are **collection variables**, so you only import one file.

```
postman/
└── collections/
    └── HireBuddy-API.postman_collection.json   ← all requests + variables + small tests
```

| Variable (collection) | Value / meaning |
|---|---|
| `authUrl` | `http://localhost:8081` (auth-service, direct) |
| `gatewayUrl` | `http://localhost:8080` (gateway; paths start with `/api/...`) |
| `seekerEmail` | set by "1. Signup seeker - success", reused by Signup 2 and Login |
| `accessToken`, `refreshToken`, `oldRefreshToken` | set by `Login & tokens` requests (run Signup 1 first) |

See / change them: click the collection **HireBuddy API** → **Variables** tab.

## First time: import

Postman → **Import** (top-left, or `Ctrl + O`) → choose the collection JSON. Done.
Top-right can stay on **No Environment**.

Re-importing: delete the old "HireBuddy API" collection first (or choose **Replace** if Postman asks), then import.

## Run

- One request: open it → **Send** → see **Test Results** tab (✅ / ❌).
- A whole folder: right-click folder (e.g. `Auth/Signup`) → **Run folder** → **Run**. Requests run top to bottom, so order matters (`1.`, `2.`, `3.`).
- The service must be running (e.g. auth-service from IntelliJ on 8081).
- Each request's **description** (right side) shows the expected reply.

## Adding a new endpoint

1. In Postman, add the request in the right folder (`Auth/...`). One success request + the important bad cases.
2. Use variables, never fixed addresses: `{{authUrl}}/auth/...`.
3. Description: what it does, needs login yes/no, expected status + example JSON.
4. Tests: keep them small, e.g.
   ```js
   pm.test('Status is 201', () => pm.response.to.have.status(201));
   ```
5. New server or value needed? Add it in the collection's **Variables** tab.
6. Collection `...` menu → **Export** → v2.1 → overwrite `collections/HireBuddy-API.postman_collection.json`.
7. Commit the JSON file.

## Never put secrets in this file

It goes to GitHub. No real passwords, admin password or tokens.
For those, fill only the variable's **Current value** in Postman (it is not exported); leave the shared value empty.
