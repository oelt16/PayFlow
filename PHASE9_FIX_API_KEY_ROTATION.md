# Phase 9: API key rotation race and dashboard parsing

## What went wrong

### Transient 401 right after “Rotate API key”

When a merchant rotated the API key from **Settings**, nginx often showed:

- `POST /api/v1/merchants/me/api-keys` → **200**
- `GET /api/v1/merchants/me` → **401** (`invalid_api_key` / `Unknown API key`)
- another `GET /api/v1/merchants/me` → **200**

That looked like a broken rotation. The backend was behaving correctly: rotation replaces the stored hash immediately, so the **previous** Bearer token stops working as soon as the `POST` succeeds.

The bug was on the **frontend**. TanStack Query runs a mutation’s `onSuccess` **before** `mutateAsync()` resolves to the caller. Our `useRotateApiKey` hook called `invalidateQueries` for `merchant/me` inside `onSuccess`, which triggered a refetch while `useAuthStore` still held the **old** key. The axios interceptor in `frontend/src/services/api-client.ts` reads `useAuthStore.getState().apiKey` on every request, so that refetch still sent the old Bearer token → **401**. Only after the promise resolved did `SettingsPage` call `setApiKey(res.apiKey)`, so the following request succeeded.

The same ordering risk existed for **Register merchant**: `onSuccess` invalidated `/me` before the page saved the new `apiKey` from the registration response.

### “Could not load payments” with HTTP 200

Separately, list payment responses from payment-service include `"clientSecret": null`. The Zod schema in `frontend/src/types/payment.ts` used `z.string().optional()`, which allows a missing field or `undefined`, but **not** JSON `null`. Parsing failed after a successful **200**, so React Query surfaced an error and the UI showed the generic payments failure message.

---

## What we changed

### 1. Persist the new key before invalidating queries

In `frontend/src/hooks/useMerchant.ts`:

- **`useRotateApiKey`**: `onSuccess` now calls `useAuthStore.getState().setApiKey(data.apiKey)` and **then** `invalidateQueries` for `merchantKeys.me`.
- **`useRegisterMerchant`**: same pattern with `data.apiKey` from the registration response.

Any refetch triggered by invalidation therefore uses the new Bearer token.

### 2. Single place for saving the key after rotate/register

`frontend/src/pages/SettingsPage.tsx` no longer calls `setApiKey` after `mutateAsync` for those two flows; the hooks own updating the store so the order cannot drift.

### 3. Accept `clientSecret: null` in payment list payloads

`paymentResponseSchema` now uses `clientSecret: z.string().nullable().optional()` so list responses match what payment-service returns.

### 4. Regression coverage

- `frontend/src/hooks/useMerchant.test.tsx`: when `invalidateQueries` runs after rotation, the store already contains the new API key.
- `frontend/src/types/payment.test.ts`: list payload with `clientSecret: null` parses successfully.

---

## How to confirm

- Rotate the key from Settings: the first `GET /api/v1/merchants/me` after `POST …/api-keys` should be **200**, with no spurious `invalid_api_key` from the old token.
- Load Overview or Payments with real data: lists should render when the API returns **200** and `clientSecret` is `null` on items.

For more debugging notes (HAR, nginx, address-bar 401 without `Authorization`), see `DASHBOARD_PAYMENTS_ERRORS.md`.
