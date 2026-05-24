# Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

## How to Access and Verify the Docs

After running `docker compose -f infra/docker-compose.yml up --build` from the repo root, all
services start and expose their OpenAPI documentation.

Each service hosts **both** the Swagger UI and the raw OpenAPI spec at a **prefixed path**
(e.g. `/payment/swagger-ui.html`, `/payment/v3/api-docs`). This lets nginx pass through the full
path to the correct backend without path-stripping — no redirect issues.

---

## 1. Quick Access (via nginx on port 3000)

The frontend nginx proxies each service's Swagger UI and raw spec. Open any of these URLs in
your browser:

| Service | Swagger UI | Raw OpenAPI Spec |
|---------|------------|------------------|
| **Payments** | http://localhost:3000/payment/swagger-ui.html | http://localhost:3000/payment/v3/api-docs |
| **Merchants** | http://localhost:3000/merchant/swagger-ui.html | http://localhost:3000/merchant/v3/api-docs |
| **Webhooks** | http://localhost:3000/webhook/swagger-ui.html | http://localhost:3000/webhook/v3/api-docs |

nginx passes the full `/payment/*` / `/merchant/*` / `/webhook/*` prefix to the backend
unchanged — no prefix stripping, no redirect rewriting needed.

> **Important:** The Swagger UI HTML page lives at `/{service}/swagger-ui.html` (with `.html`).
> The path `/{service}/swagger-ui/` (without `.html`) also works — Spring will redirect you.

---

## 2. Direct Access (per service, bypass nginx)

Each service serves its own Swagger UI and spec **with the same prefixed path**:

| Service | Port | Swagger UI | OpenAPI Spec |
|---------|:----:|------------|--------------|
| payment-service | `8081` | http://localhost:8081/payment/swagger-ui.html | http://localhost:8081/payment/v3/api-docs |
| merchant-service | `8082` | http://localhost:8082/merchant/swagger-ui.html | http://localhost:8082/merchant/v3/api-docs |
| webhook-service | `8083` | http://localhost:8083/webhook/swagger-ui.html | http://localhost:8083/webhook/v3/api-docs |

> **Why the prefix?** Swagger UI's JavaScript uses an absolute path to fetch the OpenAPI spec
> (`url: "/{service}/v3/api-docs"`). By serving both the UI and the spec under the same prefix,
> the URL works the same whether you go through nginx or direct. No sub_filter or proxy_redirect
> magic needed.

---

## 3. CLI Health Checks (no browser needed)

Use `curl` to verify the raw OpenAPI specs without opening a browser:

### Linux / macOS / WSL / Git Bash

```bash
# Verify all 3 services serve valid OpenAPI specs (direct)
curl -s http://localhost:8081/payment/v3/api-docs | head -c 200
curl -s http://localhost:8082/merchant/v3/api-docs | head -c 200
curl -s http://localhost:8083/webhook/v3/api-docs | head -c 200

# Same via nginx proxy
curl -s http://localhost:3000/payment/v3/api-docs | head -c 200
curl -s http://localhost:3000/merchant/v3/api-docs | head -c 200
curl -s http://localhost:3000/webhook/v3/api-docs | head -c 200
```

### Windows PowerShell

```powershell
# Verify all 3 services (direct)
Invoke-RestMethod -Uri http://localhost:8081/payment/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi
Invoke-RestMethod -Uri http://localhost:8082/merchant/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi
Invoke-RestMethod -Uri http://localhost:8083/webhook/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi

# Same via nginx
Invoke-RestMethod -Uri http://localhost:3000/payment/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi
Invoke-RestMethod -Uri http://localhost:3000/merchant/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi
Invoke-RestMethod -Uri http://localhost:3000/webhook/v3/api-docs -Method Get |
    Select-Object -ExpandProperty openapi
```

Expected output for each: `3.1.0` — confirms the spec is OpenAPI 3.1.

### Full endpoint listing

```bash
# Count documented endpoints per service (Linux/WSL/Mac)
curl -s http://localhost:8081/payment/v3/api-docs | jq '.paths | keys | length'
curl -s http://localhost:8082/merchant/v3/api-docs | jq '.paths | keys | length'
curl -s http://localhost:8083/webhook/v3/api-docs | jq '.paths | keys | length'
# Expected: 7 (payment), 5 (merchant), 5 (webhook)
```

```powershell
# Count with PowerShell
$spec = Invoke-RestMethod -Uri http://localhost:8081/payment/v3/api-docs
$spec.paths.PSObject.Properties.Count
```

### Swagger UI accessibility

```bash
# Check Swagger UI loads (200 = OK)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/payment/swagger-ui.html
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/merchant/swagger-ui.html
curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/webhook/swagger-ui.html

# Same via nginx
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/payment/swagger-ui.html
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/merchant/swagger-ui.html
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/webhook/swagger-ui.html
```

---

## 4. What to Check in the Docs

### 4.1 Internal endpoints are hidden

Two internal endpoints must **not** appear in any public OpenAPI spec:

- `POST /internal/webhooks/dispatch` (webhook-service) — **must be absent**
- `POST /v1/internal/merchants/validate-key` (merchant-service) — **must be absent**

```bash
# Verify they're hidden (should return empty / no match)
curl -s http://localhost:8081/payment/v3/api-docs | grep -i "internal"   # nothing
curl -s http://localhost:8082/merchant/v3/api-docs | grep -i "internal"  # nothing
curl -s http://localhost:8083/webhook/v3/api-docs | grep -i "internal"   # nothing
```

### 4.2 Bearer auth scheme is declared

Open the Swagger UI for any service and click the **"Authorize"** button (top-right).
You should see a Bearer token input field with the description:

> *"API key issued by merchant-service. Format: Bearer sk_test\_..."*

### 4.3 Idempotency-Key header is documented

In the payment-service Swagger UI, expand:

- `POST /v1/payments` → should show `Idempotency-Key` as a **required** header
- `POST /v1/payments/{id}/refunds` → should show `Idempotency-Key` as an **optional** header

### 4.4 Error responses have a typed schema

In any endpoint's response section, look for `ApiErrorResponse` instead of a generic
`object`. It should show 4 typed string fields:

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Machine-readable error code (e.g. `payment_not_found`) |
| `message` | string | Human-readable error message |
| `requestId` | string | Trace ID for debugging |
| `param` | string (nullable) | Which input field caused the error (optional) |

### 4.5 Public endpoint `POST /v1/merchants` has no lock icon

In the merchant-service Swagger UI, the `POST /v1/merchants` endpoint should **not** show
a lock icon — it's a public registration endpoint that doesn't require authentication.
All other endpoints should show the lock icon.

---

## 5. Troubleshooting — Checking Logs

### Linux / macOS

```bash
# Check if a specific service started successfully
docker compose -f infra/docker-compose.yml logs payment-service --tail=50

# Filter for springdoc / swagger startup messages
docker compose -f infra/docker-compose.yml logs payment-service |
    grep -i "swagger\|openapi\|springdoc"

# Check if nginx loaded the config correctly
docker compose -f infra/docker-compose.yml logs frontend --tail=30

# All services at once (last 20 lines each)
docker compose -f infra/docker-compose.yml logs --tail=20
```

### Windows PowerShell

```powershell
# Check a specific service
docker compose -f infra/docker-compose.yml logs payment-service --tail=50

# Filter for springdoc startup
docker compose -f infra/docker-compose.yml logs payment-service |
    Select-String -Pattern "swagger|openapi|springdoc"

# Frontend / nginx logs
docker compose -f infra/docker-compose.yml logs frontend --tail=30

# All services
docker compose -f infra/docker-compose.yml logs --tail=20
```

### Common issues

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `404` on `/payment/swagger-ui.html` | nginx location `/payment/` missing or `proxy_pass` has trailing slash (strips prefix) | Verify nginx.conf uses `proxy_pass http://payment-service:8081;` (no trailing slash) |
| `502 Bad Gateway` via nginx (direct port works) | nginx can't reach the backend | Check service is healthy: `docker compose ps` |
| `401 Unauthorized` on `/payment/v3/api-docs` | Auth filter is blocking the path | Check `springdoc.paths-to-match` is `/v1/**`; path `/payment/v3/api-docs` doesn't match it |
| Raw spec is `{}` | Controller has no endpoints mapped | Verify `@RestController` + `@RequestMapping` are present |
| Schema shows `object` instead of typed fields | `ApiErrorResponse` not created | Check `ApiExceptionHandler.java` for the record definition |
| Swagger UI loads but shows "Unable to render" | JS can't fetch the spec URL | Check `application.yml` → `springdoc.swagger-ui.url` matches the access path |

---

## 6. Full Verification Checklist

Use this **after** `docker compose up --build` completes:

```bash
# 1. All 3 services serve OpenAPI 3.1
curl -s http://localhost:8081/payment/v3/api-docs | head -c 100  # should be JSON
curl -s http://localhost:8082/merchant/v3/api-docs | head -c 100
curl -s http://localhost:8083/webhook/v3/api-docs | head -c 100

# 2. Swagger UI loads (direct)
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8081/payment/swagger-ui.html  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8082/merchant/swagger-ui.html  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8083/webhook/swagger-ui.html  # 200

# 3. Swagger UI loads (via nginx)
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/payment/swagger-ui.html  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/merchant/swagger-ui.html  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/webhook/swagger-ui.html  # 200

# 4. nginx proxy works for spec
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/payment/v3/api-docs  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/merchant/v3/api-docs  # 200
curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:3000/webhook/v3/api-docs  # 200

# 5. Internal endpoints are hidden (empty result = good)
curl -s http://localhost:8082/merchant/v3/api-docs | grep "validate-key"  # nothing
curl -s http://localhost:8083/webhook/v3/api-docs | grep "internal"       # nothing

# 6. notification-service has NO OpenAPI (404 expected)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/v3/api-docs  # 404
```

---

## 7. What Notification-Service Looks Like

`notification-service` is a Kafka consumer with **no REST API**. It runs on port `8084` but
only exposes Actuator health/metrics endpoints. It does **not** have springdoc-openapi, so:

- `http://localhost:8084/payment/swagger-ui.html` → `404 Not Found`
- `http://localhost:8084/payment/v3/api-docs` → `404 Not Found`

This is **correct** — it has no controllers to document.

---

## 8. Architecture Recap

```
Browser ──→ nginx:3000 ──→ /payment/swagger-ui.html ──→ payment-service:8081/payment/swagger-ui.html
                          ├── /merchant/swagger-ui.html ──→ merchant-service:8082/merchant/swagger-ui.html
                          ├── /webhook/swagger-ui.html ──→ webhook-service:8083/webhook/swagger-ui.html
                          │
                          └── Also:
                              /payment/v3/api-docs → payment-service:8081/payment/v3/api-docs
                              /merchant/v3/api-docs → merchant-service:8082/merchant/v3/api-docs
                              /webhook/v3/api-docs → webhook-service:8083/webhook/v3/api-docs
```

### How the routing works

Each service hosts **both** the Swagger UI and the OpenAPI spec under the same path prefix:

| Service | Swagger UI path | Spec path |
|---------|----------------|-----------|
| payment-service | `/payment/swagger-ui.html` | `/payment/v3/api-docs` |
| merchant-service | `/merchant/swagger-ui.html` | `/merchant/v3/api-docs` |
| webhook-service | `/webhook/swagger-ui.html` | `/webhook/v3/api-docs` |

Nginx uses a **pass-through** proxy (`proxy_pass http://backend:PORT;` without trailing slash) so
the full `/payment/...` path reaches the backend unchanged. This means:

1. Springdoc's redirect (`/payment/swagger-ui.html` → `/payment/swagger-ui/index.html`) uses the
   same prefix — no path mismatch.
2. Swagger UI's JavaScript fetches the spec from `url: "/payment/v3/api-docs"`, which works
   whether accessed through nginx (`localhost:3000`) or directly (`localhost:8081`).
3. No `proxy_redirect` or `sub_filter` needed — the prefix is consistent everywhere.

### Old vs new URLs

| What | Before (broken) | After (fixed) |
|------|----------------|---------------|
| Direct Swagger UI | `:8081/swagger-ui.html` | `:8081/payment/swagger-ui.html` |
| Direct spec | `:8081/v3/api-docs` | `:8081/payment/v3/api-docs` |
| nginx Swagger UI | `:3000/payment/swagger-ui/` (broken) | `:3000/payment/swagger-ui.html` (works) |
| nginx spec | `:3000/payment/v3/api-docs` (worked) | `:3000/payment/v3/api-docs` (still works) |
