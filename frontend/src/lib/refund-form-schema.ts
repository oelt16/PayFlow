import { z } from 'zod'

export const refundFormSchema = z.object({
  paymentId: z.string().min(1, 'Payment ID is required'),
  amount: z.coerce.number().int().min(1, 'Amount must be at least 1 (minor unit)'),
  currency: z
    .string()
    .length(3, 'Currency must be 3 letters (ISO 4217)')
    .transform((c) => c.toUpperCase()),
  reason: z.string().optional(),
})

export type RefundFormValues = z.infer<typeof refundFormSchema>
