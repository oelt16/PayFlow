import { z } from 'zod'

export const merchantResponseSchema = z.object({
  id: z.string(),
  name: z.string(),
  email: z.string(),
  active: z.boolean(),
  createdAt: z.string(),
})

export const registerMerchantResponseSchema = z.object({
  id: z.string(),
  name: z.string(),
  email: z.string(),
  apiKey: z.string(),
  createdAt: z.string(),
})

export const rotateApiKeyResponseSchema = z.object({
  apiKey: z.string(),
})

export type MerchantResponse = z.infer<typeof merchantResponseSchema>
export type RegisterMerchantResponse = z.infer<typeof registerMerchantResponseSchema>
export type RotateApiKeyResponse = z.infer<typeof rotateApiKeyResponseSchema>
