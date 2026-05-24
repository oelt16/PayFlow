# Design: Payment Creation UI (Phase 10)

> **Source**: Engram observation #6

## Technical Approach

New `/payments/new` route with a form component using Zod + react-hook-form + zodResolver (matching existing RefundForm pattern). On success, redirect to the payment detail page. Implement optimistic update in useCreatePayment for immediate UI feedback.

## Architecture Decisions

### Decision: Optimistic Update Strategy
**Choice**: Add new payment to cache immediately via `onMutate`, rollback on error
**Rationale**: User sees new PENDING payment instantly while API responds

### Decision: Card Expiry Field Split
**Choice**: Separate month/year Select components (not single string)
**Rationale**: Prevents invalid dates, easier UX, matches shadcn/ui Select pattern

### Decision: Form Component Boundaries
**Choice**: Separate CreatePaymentPage (router + mutation logic) from CreatePaymentForm (UI + validation only)
**Rationale**: Cleaner separation, easier testing, matches existing RefundPage pattern

## Data Flow

```
User Input → CreatePaymentForm (Zod validation)
    │
    ▼
CreatePaymentPage (useMutation)
    ├─ onMutate: Add optimistic payment to cache
    ├─ onError: Rollback cache, toast error
    └─ onSuccess: Navigate to /payments/:id
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `frontend/src/pages/CreatePaymentPage.tsx` | Create | Page wrapper, mutation logic, redirect |
| `frontend/src/components/CreatePaymentForm.tsx` | Create | Form UI, Zod integration |
| `frontend/src/lib/payment-form-schema.ts` | Create | Zod schema with validations |
| `frontend/src/hooks/usePayments.ts` | Modify | Add optimistic update to useCreatePayment |
| `frontend/src/App.tsx` | Modify | Add route for `/payments/new` |
| `frontend/src/components/layout/Sidebar.tsx` | Modify | Add "New Payment" nav link |
| `frontend/src/components/CreatePaymentForm.test.tsx` | Create | TDD tests |
| `frontend/src/test/mock-payment-api.ts` | Create | Mock utilities for tests |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Form validation errors | Render form, trigger submit, assert errors |
| Unit | Submit success | Mock useMutation resolve, assert redirect |
| Unit | API error handling | Mock useMutation reject, assert toast |
