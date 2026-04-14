import { z } from 'zod'

export const cardResponseSchema = z.object({
  last4: z.string(),
  brand: z.string(),
  expMonth: z.number(),
  expYear: z.number(),
})

export const paymentResponseSchema = z.object({
  id: z.string(),
  amount: z.number(),
  currency: z.string(),
  status: z.string(),
  description: z.string().nullable().optional(),
  clientSecret: z.string().optional(),
  metadata: z.record(z.string(), z.string()).optional(),
  card: cardResponseSchema.nullable().optional(),
  createdAt: z.string(),
  expiresAt: z.string().nullable().optional(),
  capturedAt: z.string().nullable().optional(),
  cancelledAt: z.string().nullable().optional(),
  totalRefunded: z.number(),
  amountRefunded: z.number(),
})

export const paymentListResponseSchema = z.object({
  content: z.array(paymentResponseSchema),
  totalElements: z.number(),
  page: z.number(),
  size: z.number(),
})

export const refundResponseSchema = z.object({
  id: z.string(),
  paymentId: z.string(),
  amount: z.number(),
  currency: z.string(),
  reason: z.string().nullable().optional(),
  createdAt: z.string(),
})

export const refundListResponseSchema = z.object({
  data: z.array(refundResponseSchema),
  totalElements: z.number(),
})

export type CardResponse = z.infer<typeof cardResponseSchema>
export type PaymentResponse = z.infer<typeof paymentResponseSchema>
export type PaymentListResponse = z.infer<typeof paymentListResponseSchema>
export type RefundResponse = z.infer<typeof refundResponseSchema>
export type RefundListResponse = z.infer<typeof refundListResponseSchema>

export const PAYMENT_STATUSES = [
  'PENDING',
  'CAPTURED',
  'CANCELLED',
  'REFUNDED',
  'PARTIAL_REFUND',
  'EXPIRED',
] as const

export type PaymentStatus = (typeof PAYMENT_STATUSES)[number]
