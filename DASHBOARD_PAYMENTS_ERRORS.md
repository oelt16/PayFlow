# Dashboard “Could not load payments” and related errors

This note explains what went wrong in a real session (Bruno + Docker Compose + dashboard), using evidence from browser logs and a HAR export, and how to debug similar issues later.

## What you saw

- **Overview:** “Could not load payments. Check the API key and that payment-service is running on port 8081.”
- **Payments:** “Could not load payments.”
- **Bruno:** `POST /v1/payments` returned **201** with a full body including `clientSecret`.

Those messages are **generic**; they appear whenever the React Query request for payments **fails** for any reason, not only when the API key is wrong or port 8081 is down.

---

## Cause 1: API key mismatch on merchant profile (401)

From `pf_err.log`:

```text
GET http://localhost:3000/api/v1/merchants/me 401 (Unauthorized)
```

From `pf_err.har`, the response body was:

```json
{
  "error": {
    "code": "invalid_api_key",
    "message": "Unknown API key",
    "requestId": "req_f3ad6c2c9cc34e9a9264d2313bff0dbf"
  }
}
```

**Meaning:** The Bearer token stored in the dashboard (**Settings**) is not a key the **merchant-service** recognizes (typo, old key after rotation, key from another database, or a key that was never registered in this Compose stack).

**What to do:**

- Register or recover the correct key: `POST http://localhost:8082/v1/merchants` (no auth), or use **Settings → Register** in the UI.
- Paste the **exact** `apiKey` returned once (including the `sk_test_` prefix) into Settings and save.
- If you rotated the key, update Settings with the **new** key; the old one stops working.

**Why Bruno could still work:** Bruno may use a **different** Authorization header than the one saved in the dashboard’s `localStorage`. The dashboard always sends the stored key to `/api/v1/merchants/me` and `/api/v1/payments`.

### API key rotation and a one-shot 401 (real nginx log)

Yes: **rotating the key is directly tied to the 401 you saw**, but not because the new key is wrong. From `docker compose … logs` on the **frontend** (nginx access log), the same second shows:

```text
POST /api/v1/merchants/me/api-keys … 200 … "http://localhost:3000/settings"
GET  /api/v1/merchants/me          … 401 … "http://localhost:3000/settings"
GET  /api/v1/merchants/me          … 200 … "http://localhost:3000/settings"
```

**What happened:** `POST …/api-keys` succeeds and **invalidates the old key** immediately. The rotate mutation’s `onSuccess` in [`frontend/src/hooks/useMerchant.ts`](frontend/src/hooks/useMerchant.ts) calls `invalidateQueries` for `merchant/me` **before** [`SettingsPage`](frontend/src/pages/SettingsPage.tsx) runs `setApiKey(res.apiKey)` after `mutateAsync` resolves. The refetch triggered by invalidation still uses the **old** Bearer token from the store, so merchant-service correctly answers **401 Unknown API key**. A moment later the store has the **new** key and the next `GET /merchants/me` returns **200**.

So a **401 in DevTools right after “Rotate API key”** is expected with the current order of operations; it does **not** mean the rotated key failed to persist, as long as the following `GET` is 200 and the profile loads.

**Workaround:** After rotation, if the console still shows a red 401, confirm the **next** `merchants/me` is 200; or refresh the page once the profile section updated.

**Opening the API URL in a new tab:** Another 401 in the same log has **no** `Referer` (shown as `-` in nginx):

```text
GET /api/v1/merchants/me … 401 … "-"
```

If you paste `http://localhost:3000/api/v1/merchants/me` into the **address bar**, the browser performs a **navigation** with **no** `Authorization` header, so merchant-service always returns **401**, even when the dashboard store holds a valid key. That is unrelated to rotation; use DevTools **Network** on a normal page load, or `curl` with `Authorization: Bearer …`, to test the API.

---

## Cause 2: List/get payments JSON vs frontend schema (`clientSecret: null`)

From `pf_err.har`, `GET /api/v1/payments?page=0&size=20` returned **HTTP 200** with a body like:

```json
"clientSecret": null
```

on each payment in `content`.

In **payment-service**, list and get endpoints map payments with `PaymentApiMapper.toResponse(payment)` (no second argument), so `clientSecret` is **Java `null`**, which Jackson serializes as JSON **`null`**.

In the **frontend**, `paymentResponseSchema` in `frontend/src/types/payment.ts` declares:

```ts
clientSecret: z.string().optional()
```

In Zod, `.optional()` allows **`undefined`** (or a missing key), but **not** `null`. So when the API returns `"clientSecret": null`, **`paymentListResponseSchema.parse(...)` throws** after a successful HTTP response. TanStack Query treats that as a failed query → **`isError`** → the same “Could not load payments” copy, even though payment-service and nginx are fine.

**What to do (product / code fix, not a user misconfiguration):** Relax the schema (for example `z.string().nullable().optional()`) or omit `clientSecret` in list responses from the API. Until then, the dashboard can fail to render lists even with a valid key and a **200** response.

---

## Cause 3: Chart warning in the console (noise)

From `pf_err.log`:

```text
The width(-1) and height(-1) of chart should be greater than 0
```

That comes from **Recharts** when the chart container has no usable size (often layout or first paint). It is **separate** from the payments API. It does not explain the 401; it may appear alongside a failed overview load if the payments query failed before or after layout settled.

---

## How to debug this class of issue next time

### 1. Browser DevTools → Network

- Filter by **Fetch/XHR**.
- Click **`/api/v1/payments`** and **`/api/v1/merchants/me`**.
- Check **Status** (401 vs 200 vs 502).
- Open **Response** and read JSON: merchant-service errors include `error.code` (for example `invalid_api_key`).

If payments are **200** but the UI still shows an error, open the **Console** and look for **Zod** errors (for example “Expected string, received null” on `clientSecret`).

### 2. Compare the same call outside the browser

With the key you believe the dashboard uses:

```bash
curl -sS -o /dev/stderr -w "%{http_code}\n" \
  "http://localhost:3000/api/v1/merchants/me" \
  -H "Authorization: Bearer YOUR_KEY_HERE"
```

Then:

```bash
curl -sS "http://localhost:3000/api/v1/payments?page=0&size=5" \
  -H "Authorization: Bearer YOUR_KEY_HERE" | head -c 2000
```

If curl returns **200** with `clientSecret: null` in the JSON but the UI still errors, suspect **client-side parsing** (Zod), not nginx or payment-service.

### 3. Optional: HAR export

Chrome: Network panel → right‑click → **Save all as HAR with content**. Search the file for `merchants/me`, `payments`, `"status":`, and `"clientSecret"`.

### 4. Docker logs (server-side)

From the repo root:

```bash
docker compose -f infra/docker-compose.yml logs --tail=200 payment-service merchant-service frontend
```

The **frontend** container logs proxied API calls: status, path, response size, `Referer`, and user-agent. They do **not** include the `Authorization` header (so you cannot verify the key from logs alone).

Use these lines when you want to correlate **timestamps** with UI actions (for example rotate → 401 → 200). They are less useful for a **200 + Zod** parse failure (nginx still logs **200**).

---

## Short summary

| Observation | Likely meaning |
|-------------|----------------|
| `GET /api/v1/merchants/me` **401**, `invalid_api_key` | Wrong or stale key in **Settings**, **or** refetch right after rotation still using the **old** key, **or** you opened `/api/.../merchants/me` in the address bar (no `Authorization`). |
| Right after **Rotate API key**: `POST …/api-keys` **200**, then `GET …/me` **401**, then **200** | Known ordering bug: invalidation runs before the new key is saved in the client store (see Cause 1, rotation subsection). |
| `GET /api/v1/payments` **200** but UI still errors | Often **Zod** rejecting `clientSecret: null` in the list payload. |
| Message mentions “port 8081” | Misleading for Compose: the browser uses **`/api` on port 3000**; nginx proxies to the services. Use Network tab to see the real status and body. |

Bruno succeeding only proves the key and payment-service work **for the requests Bruno sends**; the dashboard is a separate client and must use a key merchant-service accepts **and** a response shape the frontend can parse.
