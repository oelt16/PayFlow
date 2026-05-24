# Tasks: Payment Creation UI (Phase 10)

> **Source**: Engram observation #7

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~380 lines |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |

## Phase 1: Foundation / Schema

- [ ] 1.1 Create `frontend/src/lib/payment-form-schema.ts` with Zod schema
- [ ] 1.2 Export `PaymentFormValues` type via `z.infer<typeof paymentFormSchema>`

## Phase 2: Core Implementation

- [ ] 2.1 Create `frontend/src/components/CreatePaymentForm.tsx` using react-hook-form + zodResolver
- [ ] 2.2 Create `frontend/src/pages/CreatePaymentPage.tsx` with useMutation
- [ ] 2.3 Modify `frontend/src/hooks/usePayments.ts` — add onMutate/onError/onSettled

## Phase 3: Integration / Wiring

- [ ] 3.1 Modify `frontend/src/App.tsx` — add route for `/payments/new`
- [ ] 3.2 Modify `frontend/src/components/layout/Sidebar.tsx` — add nav link
- [ ] 3.3 Update `frontend/README.md` — remove "not available from UI" note

## Phase 4: Testing (TDD)

- [ ] 4.1 Create `CreatePaymentForm.test.tsx` — RED: failing tests
- [ ] 4.2 GREEN: implement form to pass validation tests
- [ ] 4.3 Write test for submit success (mock resolve, assert navigate)
- [ ] 4.4 Write test for API error state (mock reject, assert toast)
