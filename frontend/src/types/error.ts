import { z } from 'zod'

export const apiErrorSchema = z.object({
  error: z.object({
    code: z.string(),
    message: z.string(),
    param: z.string().optional(),
    requestId: z.string(),
  }),
})

export type ApiErrorBody = z.infer<typeof apiErrorSchema>

export function parseApiError(data: unknown): ApiErrorBody | null {
  const parsed = apiErrorSchema.safeParse(data)
  return parsed.success ? parsed.data : null
}
