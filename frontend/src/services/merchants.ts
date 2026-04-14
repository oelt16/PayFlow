import {
  merchantResponseSchema,
  registerMerchantResponseSchema,
  rotateApiKeyResponseSchema,
} from '@/types/merchant'

import { apiClient } from './api-client'

export async function registerMerchant(body: { name: string; email: string }) {
  const { data } = await apiClient.post('/v1/merchants', body)
  return registerMerchantResponseSchema.parse(data)
}

export async function getMerchantMe() {
  const { data } = await apiClient.get('/v1/merchants/me')
  return merchantResponseSchema.parse(data)
}

export async function deactivateMerchant() {
  await apiClient.delete('/v1/merchants/me')
}

export async function rotateApiKey() {
  const { data } = await apiClient.post('/v1/merchants/me/api-keys')
  return rotateApiKeyResponseSchema.parse(data)
}
