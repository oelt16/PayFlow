import { z } from 'zod'

const currencyEnum = z.enum(['USD', 'EUR', 'GBP'])

export const paymentFormSchema = z.object({
  amount: z.coerce
    .number()
    .int()
    .min(1, 'Amount must be at least 1 (minor unit)'),
  currency: currencyEnum,
  cardNumber: z
    .string()
    .regex(/^\d{16}$/, 'Card number must be exactly 16 digits'),
  expMonth: z.coerce.number().int().min(1).max(12, 'Expiry month must be 1-12'),
  expYear: z.coerce.number().int().min(2026, 'Expiry year must be 2026 or later'),
  cvc: z
    .string()
    .regex(/^\d{3,4}$/, 'CVC must be 3 or 4 digits'),
})

export type PaymentFormValues = z.infer<typeof paymentFormSchema>

export type CreatePaymentBody = {
  amount: number
  currency: string
  card: {
    number: string
    expMonth: number
    expYear: number
    cvc: string
  }
}

export function paymentFormValuesToCreatePaymentBody(
  values: PaymentFormValues,
): CreatePaymentBody {
  return {
    amount: values.amount,
    currency: values.currency,
    card: {
      number: values.cardNumber,
      expMonth: values.expMonth,
      expYear: values.expYear,
      cvc: values.cvc,
    },
  }
}