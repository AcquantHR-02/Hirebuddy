# auth-service – endpoints

Base URL: `http://localhost:8081`

> ⚠️ Endpoints 1–2 are TEMPORARY demo endpoints (learning only). Remove them from this file when `DemoController` is deleted.

---

### 1. Demo – OK

**What it does:** Says the service is alive. Proves a normal (good) reply works.
**Needs login:** no

```bash
curl -X GET "http://localhost:8081/auth/demo/ok"
```

**Expected reply:** `200 OK` (plain text, not JSON)
```text
auth-service is alive
```

**Try also (bad cases):**
- Change method to `POST` → expect `405 Method Not Allowed` (Spring's default JSON; header `Allow: GET`)
- Wrong case `/auth/demo/OK` → expect `404` (paths are case-sensitive; Spring's default JSON)

---

### 2. Demo – Fail

**What it does:** Always throws `ApiException`. Proves our `GlobalExceptionHandler` turns it into our `ApiError` JSON.
**Needs login:** no

```bash
curl -X GET "http://localhost:8081/auth/demo/fail"
```

**Expected reply:** `404 Not Found`
```json
{
  "timestamp": "2026-11-03T12:44:31.060020100Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Demo: this thing does not exist",
  "path": "/auth/demo/fail"
}
```
(`timestamp` changes every call; it always ends in `Z` = UTC.)

**Try also (bad cases):**
- Unknown path `/auth/demo/nothing` → expect `404`, but **Spring's default** JSON, not ours:
  `{"timestamp":"...","status":404,"error":"Not Found","path":"/auth/demo/nothing"}`
  (no `message`; `error` is `"Not Found"`, not `"NOT_FOUND"`). Our handler only catches `ApiException`; we fix this later.
