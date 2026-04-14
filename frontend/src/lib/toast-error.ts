import { isAxiosError } from 'axios'
import { toast } from 'sonner'

import { parseApiError } from '@/types/error'

export function toastApiError(err: unknown, fallback = 'Something went wrong') {
  if (isAxiosError(err) && err.response?.data) {
    const parsed = parseApiError(err.response.data)
    if (parsed) {
      toast.error(parsed.error.message)
      return
    }
  }
  if (err instanceof Error && err.message) {
    toast.error(err.message)
    return
  }
  toast.error(fallback)
}
