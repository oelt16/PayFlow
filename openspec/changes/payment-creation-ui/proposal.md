# Proposal: Phase 10 — Payment Creation UI

> **Reconstructed from**: Engram exploration (#5) and design (#6) observations

## Intent

Close the most visible gap in the PayFlow frontend: payment creation was only available via API, not through the UI. Adding `/payments/new` route with a Zod-validated form completes the user-facing payment lifecycle.

## Scope

- New `/payments/new` route in the React frontend
- `CreatePaymentForm` component with amount, currency, card fields
- Zod validation matching backend schema
- React Query mutation with optimistic update
- TDD: form validation, submit success, API error states
- Update README to remove "payment creation not available from UI" note

## Approach

- Follow existing `RefundForm` pattern (Zod + react-hook-form + zodResolver + shadcn/ui)
- Separate `CreatePaymentPage` (mutation + navigation) from `CreatePaymentForm` (UI + validation)
- Optimistic update via `onMutate` — prepend new payment to list cache
- Route-level: `<Route path="payments/new" element={<CreatePaymentPage />} />`
