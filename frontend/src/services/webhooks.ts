import {
  deliveryListResponseSchema,
  webhookListResponseSchema,
  webhookRegisteredResponseSchema,
} from '@/types/webhook'

import { apiClient } from './api-client'

export async function listWebhooks() {
  const { data } = await apiClient.get('/v1/webhooks')
  return webhookListResponseSchema.parse(data)
}

export async function registerWebhook(body: { url: string; events: string[] }) {
  const { data } = await apiClient.post('/v1/webhooks', body)
  return webhookRegisteredResponseSchema.parse(data)
}

export async function deactivateWebhook(id: string) {
  await apiClient.delete(`/v1/webhooks/${id}`)
}

export async function listDeliveries(webhookId: string) {
  const { data } = await apiClient.get(`/v1/webhooks/${webhookId}/deliveries`)
  return deliveryListResponseSchema.parse(data)
}
