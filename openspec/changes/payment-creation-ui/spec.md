# Payment Creation UI — Specification

> **Reconstructed from**: Engram exploration (#5) and design (#6) observations

## Requirements

### R1: New Route
The system MUST provide a `/payments/new` route accessible from the sidebar and payments page.

### R2: CreatePaymentForm
A form component SHALL include:
- **Amount**: positive integer (cents), Zod validation `min(1)`
- **Currency**: dropdown with USD, EUR, GBP options
- **Card number**: exactly 16 digits, regex `^\d{16}$`
- **Expiry**: separate month (1-12) and year (2026+) Select components
- **CVC**: 3-4 digit string

### R3: Zod Validation Schema
A shared schema `payment-form-schema.ts` SHALL validate all form fields before submission.

### R4: Redirect on Success
On successful payment creation, the system SHALL redirect to `/payments/:id` detail page.

### R5: Optimistic Update
The `useCreatePayment` mutation SHALL optimistically add the new PENDING payment to the payments list cache, rollback on error, and show error toast on failure.

### R6: Tests
- Validation errors display correct messages
- Submit success navigates to `/payments/:id`
- API errors show toast notification
