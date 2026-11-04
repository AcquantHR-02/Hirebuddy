# Postman – API test notes

One file per service. Every time we build a new endpoint, we add it here.

| File | Service | Port |
|---|---|---|
| `auth-service.md` | Auth (signup, login…) | 8081 |

(More files come when we build more services.)

## How to use an endpoint from these files

Each endpoint has a **cURL** block. cURL is just a request written as one text command.

1. Copy the whole cURL block.
2. In Postman click **Import** (top-left).
3. Paste the text and press **Enter** / **Import**.
4. Postman fills the method, URL, headers and body for you. Click **Send**.
5. Compare the reply with **"Expected reply"** in the file.

## Template for a new endpoint (copy this)

````markdown
### <number>. <Short name>

**What it does:** <one simple line>
**Needs login:** yes / no

```bash
curl -X POST "http://localhost:8081/..." \
  -H "Content-Type: application/json" \
  -d '{ "key": "value" }'
```

**Expected reply:** `<status code>`
```json
{ }
```

**Try also (bad cases):**
- <what to change> → expect `<status code>`
````
