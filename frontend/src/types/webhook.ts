import { z } from 'zod'

export const webhookRegisteredResponseSchema = z.object({
  id: z.string(),
  url: z.string(),
  events: z.array(z.string()),
  secret: z.string(),
  createdAt: z.string(),
})

export const webhookSummaryResponseSchema = z.object({
  id: z.string(),
  url: z.string(),
  events: z.array(z.string()),
  active: z.boolean(),
  createdAt: z.string(),
})

export const webhookListResponseSchema = z.object({
  content: z.array(webhookSummaryResponseSchema),
})

export const deliveryResponseSchema = z.object({
  id: z.string(),
  eventType: z.string(),
  status: z.string(),
  attempts: z.number(),
  lastAttemptAt: z.string().nullable().optional(),
  nextRetryAt: z.string().nullable().optional(),
  lastError: z.string().nullable().optional(),
  createdAt: z.string(),
})

export const deliveryListResponseSchema = z.object({
  data: z.array(deliveryResponseSchema),
  totalElements: z.number(),
})

export type WebhookRegisteredResponse = z.infer<typeof webhookRegisteredResponseSchema>
export type WebhookSummaryResponse = z.infer<typeof webhookSummaryResponseSchema>
export type WebhookListResponse = z.infer<typeof webhookListResponseSchema>
export type DeliveryResponse = z.infer<typeof deliveryResponseSchema>
export type DeliveryListResponse = z.infer<typeof deliveryListResponseSchema>

/** Event types aligned with PayFlow Kafka spec (section 5.3). */
export const WEBHOOK_EVENT_TYPES = [
  'payment.created',
  'payment.captured',
  'payment.cancelled',
  'payment.refunded',
  'payment.expired',
  'merchant.created',
  'merchant.deactivated',
] as const
